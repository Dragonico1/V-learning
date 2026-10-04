package com.elearning.platform.repository;

import com.elearning.platform.entity.RecursoAccesible;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RecursoAccesibleRepository extends JpaRepository<RecursoAccesible, Long> {

    List<RecursoAccesible> findByContenidoIdOrderByIdAsc(Long contenidoId);

    List<RecursoAccesible> findByContenidoIdIn(Collection<Long> contenidoIds);
}
