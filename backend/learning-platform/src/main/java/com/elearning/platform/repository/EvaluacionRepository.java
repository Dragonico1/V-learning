package com.elearning.platform.repository;

import com.elearning.platform.entity.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    List<Evaluacion> findByModuloIdOrderByIdAsc(Long moduloId);

    List<Evaluacion> findByModuloIdInOrderByIdAsc(Collection<Long> moduloIds);
}
