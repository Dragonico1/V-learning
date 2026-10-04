package com.elearning.platform.repository;

import com.elearning.platform.entity.OpcionRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OpcionRespuestaRepository extends JpaRepository<OpcionRespuesta, Long> {

    List<OpcionRespuesta> findByPreguntaIdInOrderByIdAsc(Collection<Long> preguntaIds);

    List<OpcionRespuesta> findByPreguntaIdOrderByIdAsc(Long preguntaId);
}
