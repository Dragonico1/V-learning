package com.elearning.platform.services;

import com.elearning.platform.events.ContenidoCompletadoEvent;
import com.elearning.platform.events.CursoFinalizadoEvent;
import com.elearning.platform.events.EvaluacionFinalizadaEvent;
import com.elearning.platform.events.VarkCompletadoEvent;
import com.elearning.platform.util.EventosGamificacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

/**
 * Otorga puntos después de confirmar la acción de estudio. Un fallo aquí nunca deshace el avance
 * del estudiante: se registra en el log.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GamificacionListener {

    private final GamificacionService gamificacion;
    private final RankingService ranking;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCompletarContenido(ContenidoCompletadoEvent e) {
        try {
            gamificacion.otorgar(e.estudianteId(), gamificacion.puntosDe(EventosGamificacion.CONTENIDO_COMPLETADO),
                    "Completaste un contenido.");
            ranking.actualizar(e.cursoId());
        } catch (RuntimeException ex) {
            log.error("No se pudieron otorgar puntos por contenido completado", ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alFinalizarEvaluacion(EvaluacionFinalizadaEvent e) {
        try {
            BigDecimal mejora = e.porcentaje().subtract(e.mejorPrevio()).max(BigDecimal.ZERO);
            int puntos = GamificacionService.proporcional(
                    gamificacion.puntosDe(EventosGamificacion.EVALUACION_COMPLETADA), mejora);
            gamificacion.otorgar(e.estudianteId(), puntos, "Mejoraste tu resultado en una evaluación.");
            ranking.actualizar(e.cursoId());
        } catch (RuntimeException ex) {
            log.error("No se pudieron otorgar puntos por evaluación", ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCompletarVark(VarkCompletadoEvent e) {
        try {
            gamificacion.otorgar(e.estudianteId(), gamificacion.puntosDe(EventosGamificacion.TEST_VARK),
                    "Completaste el test de estilo de aprendizaje.");
        } catch (RuntimeException ex) {
            log.error("No se pudieron otorgar puntos por el test VARK", ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alFinalizarCurso(CursoFinalizadoEvent e) {
        try {
            gamificacion.otorgar(e.estudianteId(), gamificacion.puntosDe(EventosGamificacion.CURSO_FINALIZADO),
                    "Terminaste un curso.");
        } catch (RuntimeException ex) {
            log.error("No se pudieron otorgar puntos por curso finalizado", ex);
        }
    }
}
