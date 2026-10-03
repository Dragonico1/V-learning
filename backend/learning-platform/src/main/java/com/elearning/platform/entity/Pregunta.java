package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "preguntas")
@Getter
@Setter
@NoArgsConstructor
public class Pregunta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_id")
    private Evaluacion evaluacion;

    @Column(nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String enunciado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoPregunta tipo;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal puntaje = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer orden;
}
