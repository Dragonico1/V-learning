package com.elearning.platform.services;

import com.elearning.platform.config.VlearningProperties;
import com.elearning.platform.dto.AuthDtos.*;
import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.EstadoUsuario;
import com.elearning.platform.enums.ResultadoAuditoria;
import com.elearning.platform.enums.RolUsuario;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.PerfilAccesibilidadRepository;
import com.elearning.platform.repository.PerfilAprendizajeRepository;
import com.elearning.platform.repository.RegistroAuditoriaRepository;
import com.elearning.platform.repository.UsuarioRepository;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.util.PoliticaPassword;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RF-002 Autenticar: correo + contraseña → OTP → JWT. Los fallos se auditan y cuentan en una
 * ventana; al superar el máximo se bloquea temporalmente y se avisa por correo.
 * noRollbackFor: los contadores de intentos deben guardarse aunque la operación responda con error.
 */
@Service
public class AuthService {

    private static final String ACCION_LOGIN_FALLIDO = "LOGIN_FALLIDO";
    private static final String ACCION_OTP_FALLIDO = "OTP_FALLIDO";
    private static final int MAX_OTP_FALLIDOS = 5;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final MailService correo;
    private final SesionService sesiones;
    private final AuditoriaService auditoria;
    private final RegistroAuditoriaRepository registros;
    private final NotificacionService notificaciones;
    private final PerfilAprendizajeRepository perfilesAprendizaje;
    private final PerfilAccesibilidadRepository perfilesAccesibilidad;
    private final VlearningProperties props;
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, OtpService otpService,
                       MailService correo, SesionService sesiones, AuditoriaService auditoria,
                       RegistroAuditoriaRepository registros, NotificacionService notificaciones,
                       PerfilAprendizajeRepository perfilesAprendizaje,
                       PerfilAccesibilidadRepository perfilesAccesibilidad, VlearningProperties props) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.correo = correo;
        this.sesiones = sesiones;
        this.auditoria = auditoria;
        this.registros = registros;
        this.notificaciones = notificaciones;
        this.perfilesAprendizaje = perfilesAprendizaje;
        this.perfilesAccesibilidad = perfilesAccesibilidad;
        this.props = props;
        // Para gastar el mismo tiempo cuando el correo no existe (evita enumerar cuentas).
        this.hashFalso = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public LoginRespuesta login(LoginRequest req, String ip) {
        String correoNormalizado = req.correo().trim().toLowerCase();
        Usuario u = usuarios.findByCorreoInstitucionalIgnoreCase(correoNormalizado).orElse(null);
        if (u == null) {
            passwordEncoder.matches(req.password(), hashFalso);
            auditoria.registrar(null, ACCION_LOGIN_FALLIDO, correoNormalizado, ResultadoAuditoria.DENEGADO, ip);
            throw credencialesInvalidas();
        }

        liberarBloqueoVencido(u);
        if (u.getEstado() == EstadoUsuario.BLOQUEADO || u.estaBloqueadoTemporalmente()) {
            auditoria.registrar(u.getId(), "LOGIN_BLOQUEADO", correoNormalizado, ResultadoAuditoria.DENEGADO, ip);
            throw cuentaBloqueada(u);
        }
        if (u.getEstado() == EstadoUsuario.INACTIVO) {
            passwordEncoder.matches(req.password(), hashFalso);
            auditoria.registrar(u.getId(), ACCION_LOGIN_FALLIDO, "cuenta inactiva", ResultadoAuditoria.DENEGADO, ip);
            throw credencialesInvalidas();
        }

        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            registrarFallo(u, ip);
            throw credencialesInvalidas();
        }

        String codigo = otpService.generar(u, ip);
        String cuerpo = "Tu código de verificación de V-Learning es " + codigo + ". Vence en "
                + props.otp().minutosValidez() + " minutos. Si no fuiste tú, ignora este mensaje.";
        boolean enviado = correo.enviar(u.getCorreoInstitucional(), "Tu código de verificación", cuerpo);
        boolean exponer = props.dev().exponerSecretos();
        if (!enviado && !exponer) {
            otpService.invalidar(u);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CORREO_NO_DISPONIBLE",
                    "No pudimos enviar el código a tu correo. Inténtalo más tarde o contacta al administrador.");
        }
        auditoria.registrar(u.getId(), "LOGIN_CREDENCIALES_OK", correoNormalizado, ResultadoAuditoria.PERMITIDO, ip);
        return new LoginRespuesta("OTP_REQUERIDO",
                "Enviamos un código de 6 números a tu correo. Vence en " + props.otp().minutosValidez() + " minutos.",
                exponer ? codigo : null);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public SesionRespuesta verificarOtp(OtpRequest req, String ip) {
        String correoNormalizado = req.correo().trim().toLowerCase();
        Usuario u = usuarios.findByCorreoInstitucionalIgnoreCase(correoNormalizado).orElse(null);
        if (u == null) {
            auditoria.registrar(null, ACCION_OTP_FALLIDO, correoNormalizado, ResultadoAuditoria.DENEGADO, ip);
            throw otpInvalido();
        }
        liberarBloqueoVencido(u);
        if (u.getEstado() == EstadoUsuario.BLOQUEADO || u.estaBloqueadoTemporalmente()) throw cuentaBloqueada(u);
        if (u.getEstado() == EstadoUsuario.INACTIVO) throw otpInvalido();

        LocalDateTime inicioCodigo = otpService.inicioCodigoVigente(u).orElse(LocalDateTime.now().minusMinutes(1));
        if (!otpService.verificar(u, req.codigo())) {
            auditoria.registrar(u.getId(), ACCION_OTP_FALLIDO, correoNormalizado, ResultadoAuditoria.DENEGADO, ip);
            long fallos = registros.countByUsuarioIdAndAccionAndResultadoAndFechaAfter(
                    u.getId(), ACCION_OTP_FALLIDO, ResultadoAuditoria.DENEGADO, inicioCodigo.minusSeconds(1));
            if (fallos >= MAX_OTP_FALLIDOS) {
                otpService.invalidar(u); // obliga a iniciar sesión de nuevo y pedir otro código
            }
            throw otpInvalido();
        }

        u.reiniciarIntentosFallidos();
        u.setUltimoAcceso(LocalDateTime.now());
        usuarios.save(u);
        SesionService.TokenEmitido emitido = sesiones.iniciar(u, ip);
        auditoria.registrar(u.getId(), "LOGIN_OK", correoNormalizado, ResultadoAuditoria.PERMITIDO, ip);
        return new SesionRespuesta(emitido.token(), "Bearer", emitido.expiraPorInactividad(), perfilActual(u));
    }

    @Transactional(readOnly = true)
    public PerfilActual perfilActual(UsuarioPrincipal principal) {
        Usuario u = usuarios.findById(principal.id())
                .orElseThrow(() -> ApiException.noAutenticado("NO_AUTENTICADO", "Tu sesión no es válida."));
        return perfilActual(u);
    }

    /** Cambio de la contraseña temporal (primer acceso). */
    @Transactional(noRollbackFor = ApiException.class)
    public PerfilActual primerAcceso(UsuarioPrincipal principal, PrimerAccesoRequest req, String ip) {
        Usuario u = usuarios.findById(principal.id())
                .orElseThrow(() -> ApiException.noAutenticado("NO_AUTENTICADO", "Tu sesión no es válida."));
        if (!passwordEncoder.matches(req.passwordActual(), u.getPasswordHash())) {
            auditoria.registrar(u.getId(), "PRIMER_ACCESO_FALLIDO", "contraseña actual incorrecta",
                    ResultadoAuditoria.DENEGADO, ip);
            throw ApiException.solicitudInvalida("PASSWORD_ACTUAL_INCORRECTA", "La contraseña actual no es correcta.");
        }
        PoliticaPassword.validar(req.nuevaPassword(), u.getCorreoInstitucional());
        if (passwordEncoder.matches(req.nuevaPassword(), u.getPasswordHash())) {
            throw ApiException.solicitudInvalida("PASSWORD_REPETIDA", "La nueva contraseña debe ser distinta de la temporal.");
        }
        u.setPasswordHash(passwordEncoder.encode(req.nuevaPassword()));
        if (u.getEstado() == EstadoUsuario.PENDIENTE_PRIMER_ACCESO) u.setEstado(EstadoUsuario.ACTIVO);
        usuarios.save(u);
        auditoria.registrar(u.getId(), "PRIMER_ACCESO_COMPLETADO", "usuarios/" + u.getId(),
                ResultadoAuditoria.PERMITIDO, ip);
        return perfilActual(u);
    }

    @Transactional
    public void cerrarSesion(UsuarioPrincipal principal, String ip) {
        sesiones.cerrar(principal.sesionId());
        auditoria.registrar(principal.id(), "LOGOUT", "sesiones/" + principal.sesionId(),
                ResultadoAuditoria.PERMITIDO, ip);
    }

    // ------------------------------------------------------------------

    private PerfilActual perfilActual(Usuario u) {
        Boolean vark = null;
        Boolean accesibilidad = null;
        if (u.getRol() == RolUsuario.ESTUDIANTE) {
            vark = perfilesAprendizaje.findByEstudianteId(u.getId())
                    .map(p -> p.getEstiloPredominante() != null).orElse(false);
            accesibilidad = perfilesAccesibilidad.findByEstudianteId(u.getId()).isPresent();
        }
        return new PerfilActual(u.getId(), u.getNombre(), u.getCorreoInstitucional(), u.getRol(),
                u.getEstado().name(), u.getEstado() == EstadoUsuario.PENDIENTE_PRIMER_ACCESO, vark, accesibilidad);
    }

    private void registrarFallo(Usuario u, String ip) {
        u.incrementarIntentosFallidos();
        usuarios.save(u);
        auditoria.registrar(u.getId(), ACCION_LOGIN_FALLIDO, u.getCorreoInstitucional(), ResultadoAuditoria.DENEGADO, ip);

        var bloqueo = props.bloqueo();
        long fallos = registros.countByUsuarioIdAndAccionAndResultadoAndFechaAfter(u.getId(), ACCION_LOGIN_FALLIDO,
                ResultadoAuditoria.DENEGADO, LocalDateTime.now().minusMinutes(bloqueo.ventanaMinutos()));
        if (fallos >= bloqueo.intentosMaximos()) {
            u.bloquear(LocalDateTime.now().plusMinutes(bloqueo.bloqueoMinutos()));
            usuarios.save(u);
            auditoria.registrar(u.getId(), "BLOQUEO_TEMPORAL", "demasiados intentos fallidos",
                    ResultadoAuditoria.DENEGADO, ip);
            notificaciones.notificarPorCorreo(u, "Tu cuenta fue bloqueada temporalmente",
                    "Detectamos " + bloqueo.intentosMaximos() + " intentos fallidos de inicio de sesión. "
                            + "Tu cuenta estará bloqueada " + bloqueo.bloqueoMinutos() + " minutos. "
                            + "Si no fuiste tú, cambia tu contraseña cuando puedas entrar.");
            throw cuentaBloqueada(u);
        }
    }

    private void liberarBloqueoVencido(Usuario u) {
        if (u.getBloqueadoHasta() != null && !u.estaBloqueadoTemporalmente()) {
            u.desbloquear();
            usuarios.save(u);
        }
    }

    private static ApiException credencialesInvalidas() {
        return ApiException.noAutenticado("CREDENCIALES_INVALIDAS", "El correo o la contraseña no son correctos.");
    }

    private static ApiException otpInvalido() {
        return ApiException.noAutenticado("OTP_INVALIDO",
                "El código no es correcto o ya venció. Inicia sesión de nuevo para recibir otro.");
    }

    private static ApiException cuentaBloqueada(Usuario u) {
        if (u.getBloqueadoHasta() != null) {
            long minutos = Math.max(1, Duration.between(LocalDateTime.now(), u.getBloqueadoHasta()).toMinutes() + 1);
            return new ApiException(HttpStatus.LOCKED, "CUENTA_BLOQUEADA",
                    "Tu cuenta está bloqueada temporalmente. Inténtalo de nuevo en unos " + minutos + " minutos.");
        }
        return new ApiException(HttpStatus.LOCKED, "CUENTA_BLOQUEADA",
                "Tu cuenta está bloqueada. Contacta al administrador.");
    }
}
