package com.elearning.platform.repository;

import com.elearning.platform.entity.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Long> {

    List<Tarea> findByModuloIdOrderByIdAsc(Long moduloId);

    @Query("select t from Tarea t join fetch t.modulo m join fetch m.curso where m.id in :moduloIds order by t.id asc")
    List<Tarea> deLosModulos(@Param("moduloIds") Collection<Long> moduloIds);
}
