package com.elearning.platform.repository;

import com.elearning.platform.entity.RespuestaTutor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RespuestaTutorRepository extends JpaRepository<RespuestaTutor, Long> {

    @Query("select r from RespuestaTutor r join fetch r.consulta c left join fetch c.contenido "
            + "where c.estudiante.id = :estudianteId order by c.fecha desc, r.id desc")
    List<RespuestaTutor> historial(@Param("estudianteId") Long estudianteId, Pageable pageable);

    @Query("select r from RespuestaTutor r join fetch r.consulta c left join fetch c.contenido where r.id = :id")
    java.util.Optional<RespuestaTutor> conConsulta(@Param("id") Long id);
}
