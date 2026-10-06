package com.elearning.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Quién reportó un mensaje de la comunidad (un reporte por persona y mensaje). */
@Entity
@Table(name = "reportes_mensaje")
@Getter
@Setter
@NoArgsConstructor
public class ReporteMensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mensaje_id")
    private Mensaje mensaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reportante_id")
    private Usuario reportante;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void antesDeGuardar() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}
