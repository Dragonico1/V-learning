package com.elearning.platform.repository;

import com.elearning.platform.entity.Contenido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ContenidoRepository extends JpaRepository<Contenido, Long> {

    List<Contenido> findByModuloIdInOrderByIdAsc(Collection<Long> moduloIds);

    List<Contenido> findByModuloIdInAndPublicadoTrueOrderByIdAsc(Collection<Long> moduloIds);

    @Query("select count(c) from Contenido c where c.modulo.curso.id = :cursoId and c.publicado = true")
    long contarPublicadosDelCurso(@Param("cursoId") Long cursoId);

    @Query("select count(distinct c.modulo.id) from Contenido c where c.modulo.curso.id = :cursoId and c.publicado = true")
    long contarModulosConContenidoPublicado(@Param("cursoId") Long cursoId);
}
