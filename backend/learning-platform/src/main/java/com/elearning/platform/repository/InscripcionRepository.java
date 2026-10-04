package com.elearning.platform.repository;

import com.elearning.platform.entity.Inscripcion;
import com.elearning.platform.enums.EstadoInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    Optional<Inscripcion> findByEstudianteIdAndCursoId(Long estudianteId, Long cursoId);

    @Query("select i from Inscripcion i join fetch i.curso c join fetch c.instructor "
            + "where i.estudiante.id = :estudianteId order by i.fechaInscripcion desc")
    List<Inscripcion> delEstudiante(@Param("estudianteId") Long estudianteId);

    List<Inscripcion> findByCursoIdAndEstadoIn(Long cursoId, java.util.Collection<EstadoInscripcion> estados);

    /** Para informes: todas las inscripciones con estudiante, curso e instructor cargados. */
    @Query("select i from Inscripcion i join fetch i.estudiante e join fetch i.curso c join fetch c.instructor "
            + "order by c.titulo, e.nombre")
    List<Inscripcion> conDetalle();

    @Query("select avg(i.porcentajeCompletado) from Inscripcion i where i.estado <> com.elearning.platform.enums.EstadoInscripcion.CANCELADA")
    java.math.BigDecimal promedioAvance();

    long countByEstado(EstadoInscripcion estado);
}
