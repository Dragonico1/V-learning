package com.elearning.platform.services;

import com.elearning.platform.config.VlearningProperties;
import com.elearning.platform.dto.UsuarioDtos.*;
import com.elearning.platform.entity.*;
import com.elearning.platform.enums.EstadoUsuario;
import com.elearning.platform.enums.ResultadoAuditoria;
import com.elearning.platform.enums.RolUsuario;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.UsuarioRepository;
import com.elearning.platform.util.PoliticaPassword;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * RF-001 Registrar cuenta (Administrador): valida dominio y correo único, genera contraseña
 * temporal con hash, envía credenciales (3 intentos), avisa al administrador si debe entregarlas
 * a mano y audita.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final int INTENTOS_ENVIO = 3;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final MailService correo;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;
    private final SesionService sesiones;
    private final VlearningProperties props;

    @Transactional(noRollbackFor = ApiException.class)
    public CredencialesRespuesta crear(UsuarioCrearRequest req, Long adminId, String ip) {
        String email = req.correo().trim().toLowerCase(Locale.ROOT);
        String dominio = "@" + props.dominioInstitucional().toLowerCase(Locale.ROOT);
        if (!email.endsWith(dominio)) {
            throw ApiException.solicitudInvalida("DOMINIO_NO_PERMITIDO",
                    "El correo debe terminar en " + dominio + ".");
        }
        if (usuarios.existsByCorreoInstitucionalIgnoreCase(email)) {
            auditoria.registrar(adminId, "USUARIO_DUPLICADO", email, ResultadoAuditoria.DENEGADO, ip);
            throw ApiException.conflicto("CORREO_DUPLICADO", "Ya existe una cuenta con ese correo. No se creó nada.");
        }

        String temporal = PoliticaPassword.generarTemporal();
        String hash = passwordEncoder.encode(temporal);
        String nombre = req.nombre().trim();
        Usuario nuevo;
        if (req.rol() == RolUsuario.ESTUDIANTE) {
            if (req.programaAcademico() == null || req.programaAcademico().isBlank()) {
                throw ApiException.solicitudInvalida("PROGRAMA_REQUERIDO",
                        "Indica el programa académico del estudiante.");
            }
            nuevo = new Estudiante(nombre, email, hash, codigoOGenerar(req.codigo(), "EST"),
                    req.programaAcademico().trim());
        } else if (req.rol() == RolUsuario.INSTRUCTOR) {
            nuevo = new Instructor(nombre, email, hash, codigoOGenerar(req.codigo(), "INS"),
                    req.especialidad() == null ? null : req.especialidad().trim());
        } else {
            nuevo = new Administrador(nombre, email, hash);
        }
        nuevo.setEstado(EstadoUsuario.PENDIENTE_PRIMER_ACCESO);
        nuevo = usuarios.save(nuevo);
        auditoria.registrar(adminId, "USUARIO_CREADO", "usuarios/" + nuevo.getId() + " (" + nuevo.getRol() + ")",
                ResultadoAuditoria.PERMITIDO, ip);
        return entregar(nuevo, temporal, adminId, "Se creó la cuenta.");
    }

    @Transactional(readOnly = true)
    public Pagina<UsuarioRespuesta> listar(RolUsuario rol, EstadoUsuario estado, String texto, int pagina, int tamano) {
        Specification<Usuario> spec = Specification.where(null);
        if (rol != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("rol"), rol));
        if (estado != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("estado"), estado));
        if (texto != null && !texto.isBlank()) {
            String like = "%" + texto.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((r, q, cb) -> cb.or(
                    cb.like(cb.lower(r.get("nombre")), like),
                    cb.like(cb.lower(r.get("correoInstitucional")), like)));
        }
        int p = Math.max(pagina, 0);
        int t = Math.min(Math.max(tamano, 1), 100);
        Page<Usuario> resultado = usuarios.findAll(spec, PageRequest.of(p, t, Sort.by("nombre").ascending()));
        return new Pagina<>(resultado.getContent().stream().map(UsuarioService::aRespuesta).toList(),
                resultado.getTotalElements(), p, t);
    }

    @Transactional
    public UsuarioRespuesta cambiarEstado(Long id, EstadoUsuario nuevoEstado, Long adminId, String ip) {
        if (id.equals(adminId)) {
            throw ApiException.solicitudInvalida("AUTOCAMBIO_NO_PERMITIDO", "No puedes cambiar el estado de tu propia cuenta.");
        }
        if (nuevoEstado == EstadoUsuario.PENDIENTE_PRIMER_ACCESO) {
            throw ApiException.solicitudInvalida("ESTADO_NO_PERMITIDO",
                    "Para volver a primer acceso usa «Reenviar credenciales».");
        }
        Usuario u = usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("No encontramos ese usuario."));
        if (nuevoEstado == EstadoUsuario.ACTIVO) {
            if (u.getEstado() == EstadoUsuario.PENDIENTE_PRIMER_ACCESO) {
                u.desbloquear(); // sigue pendiente de cambiar su contraseña temporal
            } else {
                u.setEstado(EstadoUsuario.ACTIVO);
                u.desbloquear();
            }
        } else {
            u.setEstado(nuevoEstado);
            u.setBloqueadoHasta(null); // bloqueo/inactivación manual: no vence solo
            sesiones.cerrarTodas(u.getId());
        }
        usuarios.save(u);
        auditoria.registrar(adminId, "USUARIO_ESTADO_" + nuevoEstado, "usuarios/" + id, ResultadoAuditoria.PERMITIDO, ip);
        return aRespuesta(u);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public CredencialesRespuesta reenviarCredenciales(Long id, Long adminId, String ip) {
        Usuario u = usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("No encontramos ese usuario."));
        if (u.getEstado() == EstadoUsuario.INACTIVO) {
            throw ApiException.conflicto("USUARIO_INACTIVO", "La cuenta está inactiva. Actívala antes de reenviar credenciales.");
        }
        String temporal = PoliticaPassword.generarTemporal();
        u.setPasswordHash(passwordEncoder.encode(temporal));
        u.setEstado(EstadoUsuario.PENDIENTE_PRIMER_ACCESO);
        u.setBloqueadoHasta(null);
        u.reiniciarIntentosFallidos();
        usuarios.save(u);
        sesiones.cerrarTodas(u.getId());
        auditoria.registrar(adminId, "CREDENCIALES_REENVIADAS", "usuarios/" + id, ResultadoAuditoria.PERMITIDO, ip);
        return entregar(u, temporal, adminId, "Se generó una contraseña temporal nueva.");
    }

    // ------------------------------------------------------------------

    /** Envía por correo hasta 3 veces; si falla, avisa al administrador para entrega manual. */
    private CredencialesRespuesta entregar(Usuario u, String temporal, Long adminId, String prefijo) {
        String cuerpo = "Hola " + u.getNombre() + ",\n\nTu cuenta de V-Learning está lista.\n"
                + "Usuario: " + u.getCorreoInstitucional() + "\nContraseña temporal: " + temporal
                + "\n\nAl entrar por primera vez te pediremos cambiarla.";
        boolean enviado = false;
        for (int i = 0; i < INTENTOS_ENVIO && !enviado; i++) {
            enviado = correo.enviar(u.getCorreoInstitucional(), "Tus credenciales de V-Learning", cuerpo);
        }
        boolean exponer = props.dev().exponerSecretos();
        boolean manual = !enviado;
        if (manual) {
            notificaciones.notificar(adminId, "Entrega manual de credenciales",
                    "No pudimos enviar las credenciales de " + u.getCorreoInstitucional()
                            + ". Entrégalas de forma segura (en persona o por un canal cifrado) o usa «Reenviar credenciales».",
                    true);
        }
        String mensaje = prefijo + (enviado ? " Enviamos las credenciales al correo."
                : " No se pudo enviar el correo: entrega la contraseña temporal de forma segura.");
        return new CredencialesRespuesta(aRespuesta(u), enviado, manual,
                (manual || exponer) ? temporal : null, mensaje);
    }

    private String codigoOGenerar(String proporcionado, String prefijo) {
        if (proporcionado != null && !proporcionado.isBlank()) return proporcionado.trim();
        return prefijo + "-" + LocalDateTime.now().getYear() + "-" + String.format("%06d", ALEATORIO.nextInt(1_000_000));
    }

    public static UsuarioRespuesta aRespuesta(Usuario u) {
        String codigo = null, programa = null, especialidad = null;
        if (u instanceof Estudiante e) {
            codigo = e.getCodigoEstudiante();
            programa = e.getProgramaAcademico();
        } else if (u instanceof Instructor i) {
            codigo = i.getCodigoInstructor();
            especialidad = i.getEspecialidad();
        }
        return new UsuarioRespuesta(u.getId(), u.getNombre(), u.getCorreoInstitucional(), u.getRol(),
                u.getEstado(), codigo, programa, especialidad, u.getFechaCreacion(), u.getUltimoAcceso());
    }
}
