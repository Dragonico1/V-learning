package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "evaluaciones")
@Getter
@Setter
@NoArgsConstructor
public class Evaluacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_id")
    private Modulo modulo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoEvaluacion tipo;

    @Column(name = "puntaje_maximo", nullable = false, precision = 8, scale = 2)
    private BigDecimal puntajeMaximo;

    @Column(name = "tiempo_limite")
    private Integer tiempoLimite;

    @Column(name = "alternativa_accesible", nullable = false)
    private boolean alternativaAccesible = true;
}
