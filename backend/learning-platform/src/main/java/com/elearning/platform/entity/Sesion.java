package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "sesiones")
@Getter
@Setter
@NoArgsConstructor
public class Sesion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** SHA-256 (hex) del JWT emitido. El token en claro nunca se guarda. */
    @Column(name = "token_jwt", nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String tokenHash;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @PrePersist
    void antesDeGuardar() {
        if (fechaInicio == null) fechaInicio = LocalDateTime.now();
    }

    public boolean esValida() {
        return activa && fechaExpiracion.isAfter(LocalDateTime.now());
    }

    public void expirar() { this.activa = false; }
}
