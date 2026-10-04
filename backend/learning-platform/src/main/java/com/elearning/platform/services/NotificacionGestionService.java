package com.elearning.platform.services;

import com.elearning.platform.dto.NotificacionDtos.*;
import com.elearning.platform.entity.Notificacion;
import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.CanalNotificacion;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.NotificacionRepository;
import com.elearning.platform.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** RF-011: bandeja de notificaciones, marcar leída, posponer y preferencias. */
@Service
@RequiredArgsConstructor
public class NotificacionGestionService {

    private final NotificacionRepository notificaciones;
    private final UsuarioRepository usuarios;

    @Transactional(readOnly = true)
    public ListaNotificaciones listar(Long usuarioId, int limite) {
        LocalDateTime ahora = LocalDateTime.now();
        int n = Math.min(Math.max(limite, 1), 100);
        List<NotificacionDto> lista = notificaciones.visibles(usuarioId, ahora, PageRequest.of(0, n)).stream()
                .map(NotificacionGestionService::aDto).toList();
        return new ListaNotificaciones(notificaciones.contarNoLeidas(usuarioId, ahora), lista);
    }

    @Transactional
    public NotificacionDto marcarLeida(Long usuarioId, Long id) {
        Notificacion n = propia(usuarioId, id);
        if (!n.isLeida()) {
            n.marcarComoLeida();
            notificaciones.save(n);
        }
        return aDto(n);
    }

    @Transactional
    public NotificacionDto posponer(Long usuarioId, Long id, String opcion) {
        Notificacion n = propia(usuarioId, id);
        String o = opcion == null ? "" : opcion.toUpperCase();
        LocalDateTime hasta;
        if (o.equals("1_DIA")) {
            hasta = LocalDateTime.now().plusDays(1);
        } else if (o.equals("3_DIAS")) {
            hasta = LocalDateTime.now().plusDays(3);
        } else if (o.equals("1_SEMANA")) {
            hasta = LocalDateTime.now().plusWeeks(1);
        } else {
            throw ApiException.solicitudInvalida("OPCION_INVALIDA", "Elige una opción válida: 1 día, 3 días o 1 semana.");
        }
        n.setPospuestaHasta(hasta);
        n.setLeida(false);
        n.setFechaLectura(null);
        notificaciones.save(n);
        return aDto(n);
    }

    @Transactional(readOnly = true)
    public PreferenciasDto preferencias(Long usuarioId) {
        Usuario u = usuarios.findById(usuarioId).orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado."));
        return new PreferenciasDto(u.isNotificacionesHabilitadas(), u.getCanalPreferido());
    }

    @Transactional
    public PreferenciasDto guardarPreferencias(Long usuarioId, PreferenciasRequest req) {
        Usuario u = usuarios.findById(usuarioId).orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado."));
        u.setNotificacionesHabilitadas(req.habilitadas());
        if (req.canal() != null) u.setCanalPreferido(req.canal());
        usuarios.save(u);
        return new PreferenciasDto(u.isNotificacionesHabilitadas(), u.getCanalPreferido());
    }

    private Notificacion propia(Long usuarioId, Long id) {
        Notificacion n = notificaciones.findById(id).orElseThrow(() -> ApiException.noEncontrado("No encontramos esa notificación."));
        if (!n.getUsuario().getId().equals(usuarioId)) throw ApiException.noEncontrado("No encontramos esa notificación.");
        return n;
    }

    private static NotificacionDto aDto(Notificacion n) {
        return new NotificacionDto(n.getId(), n.getTitulo(), n.getMensaje(), n.getFechaCreacion(), n.getFechaLectura(),
                n.getCanal(), n.isLeida());
    }
}
