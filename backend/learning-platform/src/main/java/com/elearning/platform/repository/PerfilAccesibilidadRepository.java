package com.elearning.platform.repository;

import com.elearning.platform.entity.PerfilAccesibilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilAccesibilidadRepository extends JpaRepository<PerfilAccesibilidad, Long> {

    Optional<PerfilAccesibilidad> findByEstudianteId(Long estudianteId);
}
