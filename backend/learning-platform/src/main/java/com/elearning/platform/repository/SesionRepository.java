package com.elearning.platform.repository;

import com.elearning.platform.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

    @Modifying
    @Query("update Sesion s set s.activa = false where s.usuarioId = :usuarioId and s.activa = true")
    int expirarTodasDelUsuario(@Param("usuarioId") Long usuarioId);
}
