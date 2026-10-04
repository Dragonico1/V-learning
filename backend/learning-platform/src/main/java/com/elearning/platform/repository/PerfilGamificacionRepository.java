package com.elearning.platform.repository;

import com.elearning.platform.entity.PerfilGamificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilGamificacionRepository extends JpaRepository<PerfilGamificacion, Long> {

    Optional<PerfilGamificacion> findByEstudianteId(Long estudianteId);
}
