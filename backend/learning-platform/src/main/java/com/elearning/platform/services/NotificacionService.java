package com.elearning.platform.services;

import com.elearning.platform.entity.Inscripcion;
import com.elearning.platform.entity.Notificacion;
import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.CanalNotificacion;
import com.elearning.platform.enums.EstadoInscripcion;
import com.elearning.platform.enums.EstadoUsuario;
import com.elearning.platform.enums.RolUsuario;
import com.elearning.platform.repository.InscripcionRepository;
import com.elearning.platform.repository.NotificacionRepository;
import com.elearning.platform.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Crea notificaciones (RF-011). Respeta el canal preferido y, si el correo falla,
 * reintenta por el canal alterno (la plataforma). Si la persona deshabilitó las
 * notificaciones no se envía nada, salvo que la notificación sea obligatoria
 * (alertas de seguridad).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificaciones;
    private final UsuarioRepository usuarios;
    private final MailService correo;
    private final InscripcionRepository inscripciones;

    @Transactional
    public Optional<Notificacion> notificar(Usuario usuario, String titulo, String mensaje, boolean obligatoria) {
        return notificar(usuario, titulo, mensaje, obligatoria, null);
    }

    /** {@code enlace}: ruta de la aplicación a la que lleva el botón «Ir» de la notificación (o null). */
    @Transactional
    public Optional<Notificacion> notificar(Usuario usuario, String titulo, String mensaje, boolean obligatoria, String enlace) {
        if (!usuario.isNotificacionesHabilitadas() && !obligatoria) {
            return Optional.empty();
        }
        CanalNotificacion canal = usuario.getCanalPreferido();
        if (canal == CanalNotificacion.CORREO) {
            boolean enviado = correo.enviar(usuario.getCorreoInstitucional(), titulo, mensaje);
            if (!enviado) {
                canal = CanalNotificacion.PLATAFORMA; // canal alterno
            }
        }
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setCanal(canal);
        n.setEnlace(enlace);
        return Optional.of(notificaciones.save(n));
    }

    /** Intenta el correo primero (p. ej. aviso de bloqueo); si falla, queda en la plataforma. */
    @Transactional
    public Optional<Notificacion> notificarPorCorreo(Usuario usuario, String titulo, String mensaje) {
        CanalNotificacion canal = correo.enviar(usuario.getCorreoInstitucional(), titulo, mensaje)
                ? CanalNotificacion.CORREO : CanalNotificacion.PLATAFORMA;
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setCanal(canal);
        return Optional.of(notificaciones.save(n));
    }

    @Transactional
    public Optional<Notificacion> notificar(Long usuarioId, String titulo, String mensaje, boolean obligatoria) {
        return notificar(usuarioId, titulo, mensaje, obligatoria, null);
    }

    @Transactional
    public Optional<Notificacion> notificar(Long usuarioId, String titulo, String mensaje, boolean obligatoria, String enlace) {
        return usuarios.findById(usuarioId).flatMap(u -> notificar(u, titulo, mensaje, obligatoria, enlace));
    }

    /**
     * Avisos operativos al equipo (instructor): no repite el mismo título el mismo día.
     * Son obligatorios porque no son preferencias de estudio sino gestión de contenidos.
     */
    @Transactional
    public void notificarUnaVezAlDia(Long usuarioId, String titulo, String mensaje) {
        notificarUnaVezAlDia(usuarioId, titulo, mensaje, null);
    }

    @Transactional
    public void notificarUnaVezAlDia(Long usuarioId, String titulo, String mensaje, String enlace) {
        if (notificaciones.contarPorTituloDesde(usuarioId, titulo, LocalDate.now().atStartOfDay()) > 0) return;
        notificar(usuarioId, titulo, mensaje, true, enlace);
    }

    /** Alerta a todos los administradores activos (siempre obligatoria). */
    @Transactional
    public void notificarAdministradores(String titulo, String mensaje) {
        notificarAdministradores(titulo, mensaje, null);
    }

    @Transactional
    public void notificarAdministradores(String titulo, String mensaje, String enlace) {
        for (Usuario admin : usuarios.findByRolAndEstado(RolUsuario.ADMINISTRADOR, EstadoUsuario.ACTIVO)) {
            notificar(admin, titulo, mensaje, true, enlace);
        }
    }

    /**
     * Avisa a los estudiantes con inscripción activa en el curso (contenido nuevo, tarea nueva, etc.).
     * Un fallo al avisar nunca debe impedir la acción del instructor, por eso se registra y se sigue.
     * Respeta la preferencia de cada estudiante (notificaciones deshabilitadas no reciben nada).
     */
    @Transactional
    public int notificarInscritos(Long cursoId, String titulo, String mensaje) {
        return notificarInscritos(cursoId, titulo, mensaje, null);
    }

    @Transactional
    public int notificarInscritos(Long cursoId, String titulo, String mensaje, String enlace) {
        int enviados = 0;
        for (Inscripcion i : inscripciones.findByCursoIdAndEstadoIn(cursoId, List.of(EstadoInscripcion.ACTIVA))) {
            try {
                if (notificar(i.getEstudiante(), recortar(titulo, 190), mensaje, false, enlace).isPresent()) enviados++;
            } catch (RuntimeException e) {
                log.warn("No se pudo notificar al estudiante de la inscripción {}: {}", i.getId(), e.getMessage());
            }
        }
        return enviados;
    }

    public static String recortar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}
