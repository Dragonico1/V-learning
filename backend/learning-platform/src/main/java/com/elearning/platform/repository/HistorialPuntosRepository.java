package com.elearning.platform.repository;

import com.elearning.platform.entity.HistorialPuntos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface HistorialPuntosRepository extends JpaRepository<HistorialPuntos, Long> {

    /** Otorgamientos del estudiante desde una fecha (inclusive). */
    @Query("select h from HistorialPuntos h where h.estudianteId = :estudianteId and h.fecha >= :desde")
    List<HistorialPuntos> desde(@Param("estudianteId") Long estudianteId, @Param("desde") LocalDateTime desde);
}
