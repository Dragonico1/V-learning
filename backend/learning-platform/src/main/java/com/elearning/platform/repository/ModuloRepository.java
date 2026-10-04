package com.elearning.platform.repository;

import com.elearning.platform.entity.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    List<Modulo> findByCursoIdOrderByOrden(Long cursoId);

    @Query("select coalesce(max(m.orden), 0) from Modulo m where m.curso.id = :cursoId")
    int maxOrden(@Param("cursoId") Long cursoId);

    boolean existsByCursoIdAndOrden(Long cursoId, Integer orden);
}
