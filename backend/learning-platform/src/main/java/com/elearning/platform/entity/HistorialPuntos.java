package com.elearning.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Un otorgamiento de puntos a un estudiante; permite graficar la actividad por día. */
@Entity
@Table(name = "historial_puntos")
@Getter
@Setter
@NoArgsConstructor
public class HistorialPuntos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(nullable = false)
    private Integer puntos;

    @Column(length = 300)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public HistorialPuntos(Long estudianteId, int puntos, String motivo) {
        this.estudianteId = estudianteId;
        this.puntos = puntos;
        this.motivo = motivo == null ? null : motivo.substring(0, Math.min(300, motivo.length()));
    }

    @PrePersist
    void antesDeGuardar() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}
