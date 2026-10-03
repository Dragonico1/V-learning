package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "tokens_recuperacion_password")
@Getter
@Setter
@NoArgsConstructor
public class TokenRecuperacionPassword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, length = 255, unique = true)
    private String tokenHash;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private boolean usado = false;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @PrePersist
    void antesDeGuardar() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }

    public boolean vigente() {
        return !usado && fechaExpiracion.isAfter(LocalDateTime.now());
    }
}
