package com.elearning.platform.repository;

import com.elearning.platform.entity.ConfiguracionAccesibilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionAccesibilidadRepository extends JpaRepository<ConfiguracionAccesibilidad, Long> {

    Optional<ConfiguracionAccesibilidad> findFirstByPerfilAccesibilidadIdOrderByFechaConfiguracionDescIdDesc(Long perfilId);
}
