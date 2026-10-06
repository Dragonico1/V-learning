package com.elearning.platform.repository;

import com.elearning.platform.entity.EntregaTarea;
import com.elearning.platform.enums.EstadoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EntregaTareaRepository extends JpaRepository<EntregaTarea, Long> {

    Optional<EntregaTarea> findByTareaIdAndEstudianteId(Long tareaId, Long estudianteId);

    List<EntregaTarea> findByTareaIdInAndEstudianteId(Collection<Long> tareaIds, Long estudianteId);

    List<EntregaTarea> findByTareaIdOrderByFechaEntregaAsc(Long tareaId);

    long countByTareaId(Long tareaId);

    long countByTareaIdAndEstado(Long tareaId, EstadoEntrega estado);
}
