package com.elearning.platform.repository;

import com.elearning.platform.entity.PerfilAprendizaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilAprendizajeRepository extends JpaRepository<PerfilAprendizaje, Long> {

    Optional<PerfilAprendizaje> findByEstudianteId(Long estudianteId);
}
