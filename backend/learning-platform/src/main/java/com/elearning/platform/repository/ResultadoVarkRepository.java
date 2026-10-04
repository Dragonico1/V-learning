package com.elearning.platform.repository;

import com.elearning.platform.entity.ResultadoVark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResultadoVarkRepository extends JpaRepository<ResultadoVark, Long> {

    Optional<ResultadoVark> findFirstByPerfilAprendizajeIdOrderByFechaRealizacionDescIdDesc(Long perfilAprendizajeId);
}
