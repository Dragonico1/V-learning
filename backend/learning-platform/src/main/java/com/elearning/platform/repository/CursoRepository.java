package com.elearning.platform.repository;

import com.elearning.platform.entity.Curso;
import com.elearning.platform.enums.EstadoCurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    @Query("select c from Curso c join fetch c.instructor where c.estado = :estado order by c.titulo")
    List<Curso> publicados(@Param("estado") EstadoCurso estado);

    @Query("select c from Curso c join fetch c.instructor where c.instructor.id = :instructorId order by c.fechaCreacion desc")
    List<Curso> delInstructor(@Param("instructorId") Long instructorId);
}
