package com.elearning.platform.repository;

import com.elearning.platform.entity.TokenRecuperacionPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacionPassword, Long> {

    Optional<TokenRecuperacionPassword> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update TokenRecuperacionPassword t set t.usado = true where t.usuarioId = :usuarioId and t.usado = false")
    int invalidarPendientes(@Param("usuarioId") Long usuarioId);
}
