package com.elearning.platform.repository;

import com.elearning.platform.entity.Insignia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsigniaRepository extends JpaRepository<Insignia, Long> {

    List<Insignia> findAllByOrderByPuntosRequeridosAscIdAsc();

    boolean existsByNombreIgnoreCase(String nombre);
}
