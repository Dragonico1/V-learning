package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "respuestas")
@Getter
@Setter
@NoArgsConstructor
public class Respuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intento_id")
    private IntentoEvaluacion intento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pregunta_id")
    private Pregunta pregunta;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String valor;

    @Column(name = "es_correcta")
    private Boolean esCorrecta;

    @Column(name = "puntaje_obtenido", precision = 8, scale = 2)
    private BigDecimal puntajeObtenido;
}
