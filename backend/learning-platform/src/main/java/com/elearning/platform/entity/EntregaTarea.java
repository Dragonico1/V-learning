package com.elearning.platform.entity;

import com.elearning.platform.enums.EstadoEntrega;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entrega de un estudiante para una tarea (una por estudiante; puede reenviarse hasta ser calificada). */
@Entity
@Table(name = "entregas_tarea")
@Getter
@Setter
@NoArgsConstructor
public class EntregaTarea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tarea_id")
    private Tarea tarea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String texto;

    @Column(length = 500)
    private String enlace;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDateTime fechaEntrega = LocalDateTime.now();

    @Column(precision = 8, scale = 2)
    private BigDecimal puntaje;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String retroalimentacion;

    @Column(name = "fecha_calificacion")
    private LocalDateTime fechaCalificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoEntrega estado = EstadoEntrega.ENTREGADA;
}
