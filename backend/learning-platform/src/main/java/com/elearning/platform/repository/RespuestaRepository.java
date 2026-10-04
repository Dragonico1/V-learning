package com.elearning.platform.repository;

import com.elearning.platform.entity.Respuesta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RespuestaRepository extends JpaRepository<Respuesta, Long> {

    List<Respuesta> findByIntentoId(Long intentoId);

    Optional<Respuesta> findByIntentoIdAndPreguntaId(Long intentoId, Long preguntaId);
}
