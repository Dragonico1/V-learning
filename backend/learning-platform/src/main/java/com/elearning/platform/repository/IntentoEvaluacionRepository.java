package com.elearning.platform.repository;

import com.elearning.platform.entity.IntentoEvaluacion;
import com.elearning.platform.enums.EstadoIntento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IntentoEvaluacionRepository extends JpaRepository<IntentoEvaluacion, Long> {

    Optional<IntentoEvaluacion> findFirstByEvaluacionIdAndEstudianteIdAndEstadoOrderByFechaInicioDesc(
            Long evaluacionId, Long estudianteId, EstadoIntento estado);

    List<IntentoEvaluacion> findByEvaluacionIdAndEstudianteIdOrderByFechaInicioDesc(Long evaluacionId, Long estudianteId);

    long countByEvaluacionId(Long evaluacionId);

    /** Mejor puntaje de intentos finalizados anteriores (excluye el indicado). null si no hay. */
    @Query("select max(i.puntaje) from IntentoEvaluacion i where i.evaluacion.id = :evaluacionId "
            + "and i.estudiante.id = :estudianteId and i.id <> :excluirId "
            + "and i.estado = com.elearning.platform.enums.EstadoIntento.FINALIZADO")
    java.math.BigDecimal mejorPuntajePrevio(@Param("evaluacionId") Long evaluacionId,
                                            @Param("estudianteId") Long estudianteId,
                                            @Param("excluirId") Long excluirId);

    @Query("select i from IntentoEvaluacion i join fetch i.evaluacion e join fetch e.modulo m join fetch m.curso c "
            + "where i.estudiante.id = :estudianteId and i.estado = com.elearning.platform.enums.EstadoIntento.FINALIZADO")
    List<IntentoEvaluacion> finalizadosDelEstudiante(@Param("estudianteId") Long estudianteId);

    /** Para el ranking: estudiante, evaluación, mejor puntaje y puntaje máximo, por curso. */
    @Query("select i.estudiante.id, i.evaluacion.id, max(i.puntaje), e.puntajeMaximo from IntentoEvaluacion i "
            + "join i.evaluacion e where e.modulo.curso.id = :cursoId "
            + "and i.estado = com.elearning.platform.enums.EstadoIntento.FINALIZADO "
            + "group by i.estudiante.id, i.evaluacion.id, e.puntajeMaximo")
    List<Object[]> mejoresPorCurso(@Param("cursoId") Long cursoId);
}
