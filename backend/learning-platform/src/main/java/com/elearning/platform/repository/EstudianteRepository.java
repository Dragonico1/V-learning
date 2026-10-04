package com.elearning.platform.repository;

import com.elearning.platform.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
}
