package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "consultas_tutor")
@Getter
@Setter
@NoArgsConstructor
public class ConsultaTutor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id")
    private TutorIA tutor;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "contenido_id")
    private Contenido contenido;

    @Column(nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String pregunta;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void antesDeGuardar() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}
