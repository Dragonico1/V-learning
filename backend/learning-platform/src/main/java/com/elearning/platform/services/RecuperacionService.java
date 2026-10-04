package com.elearning.platform.services;

import com.elearning.platform.config.VlearningProperties;
import com.elearning.platform.dto.AuthDtos.*;
import com.elearning.platform.entity.TokenRecuperacionPassword;
import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.EstadoUsuario;
import com.elearning.platform.enums.ResultadoAuditoria;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.TokenRecuperacionRepository;
import com.elearning.platform.repository.UsuarioRepository;
import com.elearning.platform.util.Hashes;
import com.elearning.platform.util.PoliticaPassword;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/** RF-003 Recuperar contraseña: enlace de un solo uso con vigencia limitada. */
@Service
@RequiredArgsConstructor
public class RecuperacionService {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final String MENSAJE_GENERICO =
            "Si el correo está registrado, te enviamos un enlace para crear una nueva contraseña.";

    private final UsuarioRepository usuarios;
    private final TokenRecuperacionRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final MailService correo;
    private final SesionService sesiones;
    private final AuditoriaService auditoria;
    private final VlearningProperties props;

    /** La respuesta es siempre la misma exista o no la cuenta (no revela qué correos están registrados). */
    @Transactional
    public RecuperarRespuesta solicitar(RecuperarRequest req, String ip) {
        String correoNormalizado = req.correo().trim().toLowerCase();
        Optional<Usuario> encontrado = usuarios.findByCorreoInstitucionalIgnoreCase(correoNormalizado)
                .filter(u -> u.getEstado() != EstadoUsuario.INACTIVO);
        if (encontrado.isEmpty()) {
            auditoria.registrar(null, "RECUPERACION_SOLICITADA", correoNormalizado, ResultadoAuditoria.DENEGADO, ip);
            return new RecuperarRespuesta(MENSAJE_GENERICO, null);
        }
        Usuario u = encontrado.get();
        tokens.invalidarPendientes(u.getId());

        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        TokenRecuperacionPassword t = new TokenRecuperacionPassword();
        t.setUsuarioId(u.getId());
        t.setTokenHash(Hashes.sha256Hex(token));
        t.setFechaExpiracion(LocalDateTime.now().plusMinutes(props.recuperacion().minutosValidez()));
        t.setIpOrigen(ip);
        tokens.save(t);

        String enlace = props.recuperacion().urlBase() + "?token=" + token;
        correo.enviar(u.getCorreoInstitucional(), "Recupera tu contraseña de V-Learning",
                "Usa este enlace para crear una nueva contraseña. Es de un solo uso y vence en "
                        + props.recuperacion().minutosValidez() + " minutos:\n\n" + enlace
                        + "\n\nSi no lo pediste tú, ignora este mensaje.");
        auditoria.registrar(u.getId(), "RECUPERACION_SOLICITADA", correoNormalizado, ResultadoAuditoria.PERMITIDO, ip);
        return new RecuperarRespuesta(MENSAJE_GENERICO, props.dev().exponerSecretos() ? enlace : null);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public MensajeRespuesta restablecer(RestablecerRequest req, String ip) {
        TokenRecuperacionPassword t = tokens.findByTokenHash(Hashes.sha256Hex(req.token().trim())).orElse(null);
        if (t == null || !t.vigente()) {
            auditoria.registrar(t == null ? null : t.getUsuarioId(), "RESTABLECER_ENLACE_INVALIDO",
                    "enlace vencido o usado", ResultadoAuditoria.DENEGADO, ip);
            throw ApiException.solicitudInvalida("ENLACE_INVALIDO",
                    "El enlace venció o ya se usó. Solicita uno nuevo desde «¿Olvidaste tu contraseña?».");
        }
        Usuario u = usuarios.findById(t.getUsuarioId())
                .orElseThrow(() -> ApiException.solicitudInvalida("ENLACE_INVALIDO", "El enlace no es válido."));
        // Se valida antes de consumir el enlace: si la contraseña es débil, la persona puede reintentar.
        PoliticaPassword.validar(req.nuevaPassword(), u.getCorreoInstitucional());

        u.setPasswordHash(passwordEncoder.encode(req.nuevaPassword()));
        if (u.getEstado() == EstadoUsuario.PENDIENTE_PRIMER_ACCESO) u.setEstado(EstadoUsuario.ACTIVO);
        u.reiniciarIntentosFallidos();
        usuarios.save(u);
        t.setUsado(true);
        sesiones.cerrarTodas(u.getId());
        auditoria.registrar(u.getId(), "PASSWORD_RESTABLECIDA", "usuarios/" + u.getId(), ResultadoAuditoria.PERMITIDO, ip);
        return new MensajeRespuesta("Tu contraseña se actualizó. Ya puedes iniciar sesión.");
    }
}
