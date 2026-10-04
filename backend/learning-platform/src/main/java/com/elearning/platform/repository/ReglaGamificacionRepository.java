package com.elearning.platform.repository;

import com.elearning.platform.entity.ReglaGamificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReglaGamificacionRepository extends JpaRepository<ReglaGamificacion, Long> {

    Optional<ReglaGamificacion> findFirstByEventoOrderByIdAsc(String evento);

    List<ReglaGamificacion> findAllByOrderByEventoAscIdAsc();
}
