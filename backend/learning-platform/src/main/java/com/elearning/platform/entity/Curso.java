package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "cursos")
@Getter
@Setter
@NoArgsConstructor
public class Curso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id")
    private Instructor instructor;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoCurso estado = EstadoCurso.BORRADOR;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    // Lo actualiza el trigger trg_cursos_fecha_actualizacion_update
    @Column(name = "fecha_actualizacion", nullable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void antesDeGuardar() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaCreacion == null) fechaCreacion = ahora;
        if (fechaActualizacion == null) fechaActualizacion = ahora;
    }

    public void publicar() { this.estado = EstadoCurso.PUBLICADO; }

    public void archivar() { this.estado = EstadoCurso.ARCHIVADO; }
}
