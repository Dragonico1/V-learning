package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "reglas_gamificacion")
@Getter
@Setter
@NoArgsConstructor
public class ReglaGamificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String descripcion;

    @Column(nullable = false, length = 100)
    private String evento;

    @Column(nullable = false)
    private Integer puntos;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "administrador_id")
    private Administrador administrador;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    // Lo actualiza el trigger trg_reglas_gamificacion_fecha_actualizacion_update
    @Column(name = "fecha_actualizacion", nullable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void antesDeGuardar() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaCreacion == null) fechaCreacion = ahora;
        if (fechaActualizacion == null) fechaActualizacion = ahora;
    }
}
