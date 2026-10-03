package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "progresos")
@Getter
@Setter
@NoArgsConstructor
public class Progreso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contenido_id")
    private Contenido contenido;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje = BigDecimal.ZERO;

    @Column(name = "tiempo_consumido", nullable = false)
    private Long tiempoConsumido = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoProgreso estado = EstadoProgreso.NO_INICIADO;

    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;

    public void registrarConsumo(long segundos) {
        if (segundos > 0) this.tiempoConsumido += segundos;
        this.ultimaActividad = LocalDateTime.now();
        if (estado == EstadoProgreso.NO_INICIADO) estado = EstadoProgreso.EN_PROGRESO;
    }

    public void marcarCompletado() {
        this.porcentaje = new BigDecimal("100.00");
        this.estado = EstadoProgreso.COMPLETADO;
        this.ultimaActividad = LocalDateTime.now();
    }
}
