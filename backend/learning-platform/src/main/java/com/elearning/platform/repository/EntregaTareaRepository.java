package com.elearning.platform.repository;

import com.elearning.platform.entity.EntregaTarea;
import com.elearning.platform.enums.EstadoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EntregaTareaRepository extends JpaRepository<EntregaTarea, Long> {

    Optional<EntregaTarea> findByTareaIdAndEstudianteId(Long tareaId, Long estudianteId);

    List<EntregaTarea> findByTareaIdInAndEstudianteId(Collection<Long> tareaIds, Long estudianteId);

    List<EntregaTarea> findByTareaIdOrderByFechaEntregaAsc(Long tareaId);

    @Query("select e from EntregaTarea e join fetch e.tarea t join fetch t.modulo m join fetch m.curso "
            + "where e.estudiante.id = :estudianteId order by e.id asc")
    List<EntregaTarea> delEstudiante(@Param("estudianteId") Long estudianteId);

    long countByTareaId(Long tareaId);

    long countByTareaIdAndEstado(Long tareaId, EstadoEntrega estado);
}
