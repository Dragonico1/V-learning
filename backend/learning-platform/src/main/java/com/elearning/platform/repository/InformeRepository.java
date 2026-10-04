package com.elearning.platform.repository;

import com.elearning.platform.entity.Informe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InformeRepository extends JpaRepository<Informe, Long> {

    List<Informe> findByUsuarioIdOrderByFechaGeneracionDesc(Long usuarioId, Pageable pageable);
}
