package com.elearning.platform.services;

import com.elearning.platform.dto.ContenidoDtos.ProgresoRequest;
import com.elearning.platform.dto.ContenidoDtos.ProgresoRespuesta;
import com.elearning.platform.entity.Contenido;
import com.elearning.platform.entity.Inscripcion;
import com.elearning.platform.entity.Progreso;
import com.elearning.platform.enums.EstadoInscripcion;
import com.elearning.platform.enums.EstadoProgreso;
import com.elearning.platform.events.ContenidoCompletadoEvent;
import com.elearning.platform.events.CursoFinalizadoEvent;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Registro de avance y tiempo (RF-005). Regla: un módulo está completo cuando se completó al menos
 * un formato publicado; el % del curso = módulos completos ÷ módulos con contenido publicado.
 */
@Service
@RequiredArgsConstructor
public class ProgresoService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ContenidoRepository contenidos;
    private final InscripcionRepository inscripciones;
    private final ProgresoRepository progresos;
    private final EstudianteRepository estudiantes;
    private final ApplicationEventPublisher eventos;

    @Transactional
    public ProgresoRespuesta registrar(Long estudianteId, Long contenidoId, ProgresoRequest req) {
        Contenido c = contenidos.findById(contenidoId).filter(Contenido::isPublicado)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos ese contenido."));
        Long cursoId = c.getModulo().getCurso().getId();
        Inscripcion insc = inscripciones.findByEstudianteIdAndCursoId(estudianteId, cursoId)
                .filter(i -> i.getEstado() != EstadoInscripcion.CANCELADA)
                .orElseThrow(() -> ApiException.prohibido("NO_INSCRITO", "Inscríbete en el curso para registrar tu avance."));

        Progreso p = progresos.findByEstudianteIdAndContenidoId(estudianteId, contenidoId).orElseGet(() -> {
            Progreso nuevo = new Progreso();
            nuevo.setEstudiante(estudiantes.getReferenceById(estudianteId));
            nuevo.setContenido(c);
            return nuevo;
        });
        boolean yaCompleto = p.getEstado() == EstadoProgreso.COMPLETADO;

        p.registrarConsumo(req.segundos() == null ? 0 : req.segundos());
        if (req.porcentaje() != null && req.porcentaje().compareTo(p.getPorcentaje()) > 0) {
            p.setPorcentaje(req.porcentaje()); // el avance nunca retrocede
        }
        boolean completar = Boolean.TRUE.equals(req.completado()) || p.getPorcentaje().compareTo(CIEN) >= 0;
        if (completar) p.marcarCompletado();
        progresos.save(p);

        BigDecimal pct = recalcular(insc, cursoId, estudianteId);
        boolean finalizado = insc.getEstado() == EstadoInscripcion.FINALIZADA;
        boolean moduloCompleto = progresos.contarCompletadosEnModulo(estudianteId, c.getModulo().getId()) > 0;

        if (completar && !yaCompleto) {
            eventos.publishEvent(new ContenidoCompletadoEvent(estudianteId, contenidoId, cursoId));
        }
        return new ProgresoRespuesta(p.getEstado(), p.getPorcentaje(), p.getTiempoConsumido(), pct, moduloCompleto, finalizado);
    }

    /** Recalcula el % del curso y su estado; publica el evento al finalizar. */
    private BigDecimal recalcular(Inscripcion insc, Long cursoId, Long estudianteId) {
        long conContenido = contenidos.contarModulosConContenidoPublicado(cursoId);
        long completos = progresos.contarModulosCompletados(estudianteId, cursoId);
        BigDecimal pct = conContenido == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(completos * 100L).divide(BigDecimal.valueOf(conContenido), 2, RoundingMode.HALF_UP);
        if (pct.compareTo(CIEN) > 0) pct = CIEN;
        insc.setPorcentajeCompletado(pct);
        boolean estabaFinalizada = insc.getEstado() == EstadoInscripcion.FINALIZADA;
        if (pct.compareTo(CIEN) >= 0 && insc.getEstado() == EstadoInscripcion.ACTIVA) {
            insc.setEstado(EstadoInscripcion.FINALIZADA);
        } else if (pct.compareTo(CIEN) < 0 && estabaFinalizada) {
            insc.setEstado(EstadoInscripcion.ACTIVA); // se publicó contenido nuevo
        }
        inscripciones.save(insc);
        if (!estabaFinalizada && insc.getEstado() == EstadoInscripcion.FINALIZADA) {
            eventos.publishEvent(new CursoFinalizadoEvent(estudianteId, cursoId));
        }
        return pct;
    }
}
