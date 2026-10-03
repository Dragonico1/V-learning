package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "inscripciones")
@Getter
@Setter
@NoArgsConstructor
public class Inscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDateTime fechaInscripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoInscripcion estado = EstadoInscripcion.ACTIVA;

    @Column(name = "porcentaje_completado", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeCompletado = BigDecimal.ZERO;

    @PrePersist
    void antesDeGuardar() {
        if (fechaInscripcion == null) fechaInscripcion = LocalDateTime.now();
    }

    public void activar() { this.estado = EstadoInscripcion.ACTIVA; }

    public void cancelar() { this.estado = EstadoInscripcion.CANCELADA; }
}
