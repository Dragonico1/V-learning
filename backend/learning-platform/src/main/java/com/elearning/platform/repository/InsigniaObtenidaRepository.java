package com.elearning.platform.repository;

import com.elearning.platform.entity.InsigniaObtenida;
import com.elearning.platform.entity.InsigniaObtenidaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsigniaObtenidaRepository extends JpaRepository<InsigniaObtenida, InsigniaObtenidaId> {

    List<InsigniaObtenida> findByPerfilGamificacionId(Long perfilGamificacionId);
}
