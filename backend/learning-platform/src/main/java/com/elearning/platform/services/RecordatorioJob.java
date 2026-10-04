package com.elearning.platform.services;

import com.elearning.platform.config.VlearningProperties;
import com.elearning.platform.entity.Inscripcion;
import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.EstadoInscripcion;
import com.elearning.platform.enums.EstadoUsuario;
import com.elearning.platform.enums.RolUsuario;
import com.elearning.platform.repository.InscripcionRepository;
import com.elearning.platform.repository.NotificacionRepository;
import com.elearning.platform.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RF-011: recordatorios diarios de actividades pendientes e inactividad. Respeta las preferencias de cada
 * estudiante y no repite el mismo aviso el mismo día. No hay fechas límite en el modelo de datos, por eso el
 * recordatorio se basa en cursos activos sin completar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordatorioJob {

    static final String TITULO_PENDIENTES = "Tienes actividades pendientes";
    static final String TITULO_INACTIVIDAD = "Te esperamos para seguir aprendiendo";

    private final UsuarioRepository usuarios;
    private final InscripcionRepository inscripciones;
    private final NotificacionRepository notificaciones;
    private final NotificacionService notificador;
    private final VlearningProperties props;

    @Scheduled(cron = "${vlearning.notificaciones.cron}")
    public void ejecutar() {
        int enviados = 0;
        for (Usuario u : usuarios.findByRolAndEstado(RolUsuario.ESTUDIANTE, EstadoUsuario.ACTIVO)) {
            if (!u.isNotificacionesHabilitadas()) continue;
            try {
                enviados += recordar(u);
            } catch (RuntimeException e) {
                log.warn("No se pudo generar el recordatorio para el usuario {}: {}", u.getId(), e.getMessage());
            }
        }
        log.info("Recordatorios diarios generados: {}", enviados);
    }

    int recordar(Usuario u) {
        int n = 0;
        List<Inscripcion> pendientes = inscripciones.delEstudiante(u.getId()).stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.ACTIVA
                        && i.getPorcentajeCompletado().compareTo(new BigDecimal("100")) < 0)
                .toList();
        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        if (!pendientes.isEmpty() && notificaciones.contarPorTituloDesde(u.getId(), TITULO_PENDIENTES, hoy) == 0) {
            String cursos = String.join(", ", pendientes.stream().limit(3).map(i -> "«" + i.getCurso().getTitulo() + "»").toList());
            notificador.notificar(u, TITULO_PENDIENTES,
                    "Tienes " + pendientes.size() + " curso(s) por completar: " + cursos + ".", false);
            n++;
        }
        LocalDateTime limite = LocalDateTime.now().minusDays(props.notificaciones().diasInactividad());
        boolean inactivo = u.getUltimoAcceso() != null && u.getUltimoAcceso().isBefore(limite);
        if (inactivo && !pendientes.isEmpty()
                && notificaciones.contarPorTituloDesde(u.getId(), TITULO_INACTIVIDAD, hoy) == 0) {
            notificador.notificar(u, TITULO_INACTIVIDAD,
                    "Hace más de " + props.notificaciones().diasInactividad() + " días que no entras. Retoma donde lo dejaste.", false);
            n++;
        }
        return n;
    }
}
