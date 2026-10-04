package com.elearning.platform.repository;

import com.elearning.platform.entity.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {

    List<Pregunta> findByEvaluacionIdOrderByOrden(Long evaluacionId);

    boolean existsByEvaluacionIdAndOrden(Long evaluacionId, Integer orden);

    @Query("select coalesce(max(p.orden), 0) from Pregunta p where p.evaluacion.id = :evaluacionId")
    int maxOrden(@Param("evaluacionId") Long evaluacionId);
}
