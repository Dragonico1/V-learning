package com.elearning.platform.repository;

import com.elearning.platform.entity.Mensaje;
import com.elearning.platform.enums.EstadoMensaje;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    /** Mensajes reportados pendientes de revisión en los cursos de un instructor. */
    @Query("select m from Mensaje m join fetch m.usuario join fetch m.curso c where c.instructor.id = :instructorId "
            + "and m.estado = :estado order by m.fechaReporte asc, m.id asc")
    List<Mensaje> reportadosDelInstructor(@Param("instructorId") Long instructorId, @Param("estado") EstadoMensaje estado);

    @Query("select m from Mensaje m join fetch m.usuario where m.curso.id = :cursoId and m.estado in :estados "
            + "order by m.fechaEnvio desc, m.id desc")
    List<Mensaje> recientes(@Param("cursoId") Long cursoId, @Param("estados") Collection<EstadoMensaje> estados,
                            Pageable pageable);
}
