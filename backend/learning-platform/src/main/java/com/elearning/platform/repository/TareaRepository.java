package com.elearning.platform.repository;

import com.elearning.platform.entity.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Long> {

    List<Tarea> findByModuloIdOrderByIdAsc(Long moduloId);
}
