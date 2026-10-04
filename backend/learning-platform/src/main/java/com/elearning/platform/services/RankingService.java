package com.elearning.platform.services;

import com.elearning.platform.dto.GamificacionDtos.PosicionDto;
import com.elearning.platform.dto.GamificacionDtos.RankingRespuesta;
import com.elearning.platform.entity.*;
import com.elearning.platform.enums.EstadoInscripcion;
import com.elearning.platform.enums.EstadoCurso;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.*;
import com.elearning.platform.util.EventosGamificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Clasificación por curso (RF-009). Los puntos del curso se recalculan desde la actividad real
 * (contenidos completados + mejor nota de cada evaluación) con las reglas vigentes. Quien activó
 * el modo privado no figura en la tabla.
 */
@Service
@RequiredArgsConstructor
public class RankingService {

    public static final String PERIODO = "ACUMULADO";
    private static final Duration VIGENCIA = Duration.ofMinutes(10);

    private final RankingRepository rankings;
    private final PosicionRankingRepository posiciones;
    private final CursoRepository cursos;
    private final InscripcionRepository inscripciones;
    private final ProgresoRepository progresos;
    private final IntentoEvaluacionRepository intentos;
    private final PerfilGamificacionRepository perfiles;
    private final GamificacionService gamificacion;

    @Transactional
    public RankingRespuesta obtener(Long cursoId, Long estudianteId) {
        Curso curso = cursos.findById(cursoId).filter(c -> c.getEstado() == EstadoCurso.PUBLICADO)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos ese curso."));
        inscripciones.findByEstudianteIdAndCursoId(estudianteId, cursoId)
                .filter(i -> i.getEstado() != EstadoInscripcion.CANCELADA)
                .orElseThrow(() -> ApiException.prohibido("NO_INSCRITO", "Solo ven la clasificación quienes están inscritos."));
        Ranking r = rankings.findByCursoIdAndPeriodo(cursoId, PERIODO).orElse(null);
        if (r == null || r.getFechaActualizacion().isBefore(LocalDateTime.now().minus(VIGENCIA))) {
            r = actualizar(curso.getId());
        }
        boolean privado = !gamificacion.perfilDe(estudianteId).isVisibleRanking();
        List<PosicionDto> lista = new ArrayList<>();
        Integer mia = null;
        for (PosicionRanking p : posiciones.delRanking(r.getId())) {
            boolean yo = p.getEstudianteId().equals(estudianteId);
            if (yo) mia = p.getPosicion();
            lista.add(new PosicionDto(p.getPosicion(), p.getEstudiante().getNombre(), p.getPuntos(), yo));
        }
        return new RankingRespuesta(cursoId, PERIODO, r.getFechaActualizacion(), lista, mia, privado);
    }

    /** Recalcula y reemplaza las posiciones del curso. */
    @Transactional
    public Ranking actualizar(Long cursoId) {
        Curso curso = cursos.getReferenceById(cursoId);
        Ranking r = rankings.findByCursoIdAndPeriodo(cursoId, PERIODO).orElseGet(() -> {
            Ranking nuevo = new Ranking();
            nuevo.setCurso(curso);
            nuevo.setPeriodo(PERIODO);
            return nuevo;
        });
        r = rankings.save(r);

        int puntosContenido = gamificacion.puntosDe(EventosGamificacion.CONTENIDO_COMPLETADO);
        int puntosEvaluacion = gamificacion.puntosDe(EventosGamificacion.EVALUACION_COMPLETADA);

        Map<Long, Integer> puntos = new HashMap<>();
        for (Inscripcion i : inscripciones.findByCursoIdAndEstadoIn(cursoId,
                List.of(EstadoInscripcion.ACTIVA, EstadoInscripcion.FINALIZADA))) {
            puntos.put(i.getEstudiante().getId(), 0);
        }
        for (Object[] fila : progresos.completadosPorEstudiante(cursoId)) {
            puntos.computeIfPresent((Long) fila[0], (k, v) -> v + ((Long) fila[1]).intValue() * puntosContenido);
        }
        for (Object[] fila : intentos.mejoresPorCurso(cursoId)) {
            BigDecimal mejor = (BigDecimal) fila[2];
            BigDecimal maximo = (BigDecimal) fila[3];
            if (mejor == null || maximo == null || maximo.signum() == 0) continue;
            BigDecimal pct = mejor.multiply(new BigDecimal("100")).divide(maximo, 2, java.math.RoundingMode.HALF_UP);
            int pts = GamificacionService.proporcional(puntosEvaluacion, pct);
            puntos.computeIfPresent((Long) fila[0], (k, v) -> v + pts);
        }

        // Fuera de la tabla pública quienes activaron el modo privado.
        List<Map.Entry<Long, Integer>> visibles = new ArrayList<>();
        Map<Long, String> nombres = new HashMap<>();
        for (Map.Entry<Long, Integer> e : puntos.entrySet()) {
            boolean visible = perfiles.findByEstudianteId(e.getKey()).map(PerfilGamificacion::isVisibleRanking).orElse(true);
            if (visible) {
                visibles.add(e);
                nombres.put(e.getKey(), perfiles.findByEstudianteId(e.getKey()).map(p -> p.getEstudiante().getNombre()).orElse(""));
            }
        }
        visibles.sort((a, b) -> {
            int c = Integer.compare(b.getValue(), a.getValue());
            return c != 0 ? c : Long.compare(a.getKey(), b.getKey());
        });

        posiciones.borrarDelRanking(r.getId());
        int pos = 1;
        for (Map.Entry<Long, Integer> e : visibles) {
            PosicionRanking p = new PosicionRanking();
            p.setRankingId(r.getId());
            p.setEstudianteId(e.getKey());
            p.setPosicion(pos++);
            p.setPuntos(e.getValue());
            posiciones.save(p);
        }
        r.setFechaActualizacion(LocalDateTime.now());
        return rankings.save(r);
    }
}
