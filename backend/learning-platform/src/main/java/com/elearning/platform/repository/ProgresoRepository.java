package com.elearning.platform.repository;

import com.elearning.platform.entity.Progreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProgresoRepository extends JpaRepository<Progreso, Long> {

    Optional<Progreso> findByEstudianteIdAndContenidoId(Long estudianteId, Long contenidoId);

    List<Progreso> findByEstudianteIdAndContenidoIdIn(Long estudianteId, Collection<Long> contenidoIds);

    List<Progreso> findByEstudianteId(Long estudianteId);

    /** Módulos del curso con al menos un contenido publicado completado por el estudiante. */
    @Query("select count(distinct c.modulo.id) from Progreso p join p.contenido c "
            + "where p.estudiante.id = :estudianteId and c.modulo.curso.id = :cursoId and c.publicado = true "
            + "and p.estado = com.elearning.platform.enums.EstadoProgreso.COMPLETADO")
    long contarModulosCompletados(@Param("estudianteId") Long estudianteId, @Param("cursoId") Long cursoId);

    @Query("select count(p) from Progreso p where p.estudiante.id = :estudianteId and p.contenido.modulo.id = :moduloId "
            + "and p.contenido.publicado = true and p.estado = com.elearning.platform.enums.EstadoProgreso.COMPLETADO")
    long contarCompletadosEnModulo(@Param("estudianteId") Long estudianteId, @Param("moduloId") Long moduloId);
}
