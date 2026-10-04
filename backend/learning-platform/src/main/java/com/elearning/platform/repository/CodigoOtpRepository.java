package com.elearning.platform.repository;

import com.elearning.platform.entity.CodigoOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CodigoOtpRepository extends JpaRepository<CodigoOtp, Long> {

    Optional<CodigoOtp> findFirstByUsuarioIdAndUsadoFalseOrderByFechaCreacionDesc(Long usuarioId);

    @Modifying
    @Query("update CodigoOtp c set c.usado = true where c.usuarioId = :usuarioId and c.usado = false")
    int invalidarPendientes(@Param("usuarioId") Long usuarioId);
}
