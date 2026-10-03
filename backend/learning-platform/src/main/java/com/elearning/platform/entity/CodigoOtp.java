package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "codigos_otp")
@Getter
@Setter
@NoArgsConstructor
public class CodigoOtp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "codigo_hash", nullable = false, length = 255)
    private String codigoHash;

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
