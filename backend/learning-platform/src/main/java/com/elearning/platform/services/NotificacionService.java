package com.elearning.platform.services;

import com.elearning.platform.entity.Notificacion;
import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.CanalNotificacion;
import com.elearning.platform.enums.EstadoUsuario;
import com.elearning.platform.enums.RolUsuario;
import com.elearning.platform.repository.NotificacionRepository;
import com.elearning.platform.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Crea notificaciones (RF-011). Respeta el canal preferido y, si el correo falla,
 * reintenta por el canal alterno (la plataforma). Si la persona deshabilitó las
 * notificaciones no se envía nada, salvo que la notificación sea obligatoria
 * (alertas de seguridad).
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificaciones;
    private final UsuarioRepository usuarios;
    private final MailService correo;

    @Transactional
    public Optional<Notificacion> notificar(Usuario usuario, String titulo, String mensaje, boolean obligatoria) {
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
        return usuarios.findById(usuarioId).flatMap(u -> notificar(u, titulo, mensaje, obligatoria));
    }

    /** Alerta a todos los administradores activos (siempre obligatoria). */
    @Transactional
    public void notificarAdministradores(String titulo, String mensaje) {
        for (Usuario admin : usuarios.findByRolAndEstado(RolUsuario.ADMINISTRADOR, EstadoUsuario.ACTIVO)) {
            notificar(admin, titulo, mensaje, true);
        }
    }
}
