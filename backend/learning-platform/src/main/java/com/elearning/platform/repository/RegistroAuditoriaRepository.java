package com.elearning.platform.repository;

import com.elearning.platform.entity.RegistroAuditoria;
import com.elearning.platform.enums.ResultadoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface RegistroAuditoriaRepository
        extends JpaRepository<RegistroAuditoria, Long>, JpaSpecificationExecutor<RegistroAuditoria> {

    long countByUsuarioIdAndResultadoAndFechaAfter(Long usuarioId, ResultadoAuditoria resultado, LocalDateTime desde);

    long countByUsuarioIdAndAccionStartingWithAndFechaAfter(Long usuarioId, String prefijoAccion, LocalDateTime desde);

    long countByUsuarioIdAndAccionAndResultadoAndFechaAfter(Long usuarioId, String accion,
                                                            ResultadoAuditoria resultado, LocalDateTime desde);
}
