package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "resultados_vark")
@Getter
@Setter
@NoArgsConstructor
public class ResultadoVark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_aprendizaje_id")
    private PerfilAprendizaje perfilAprendizaje;

    @Column(nullable = false)
    private Integer visual = 0;

    @Column(nullable = false)
    private Integer auditivo = 0;

    @Column(name = "lectura_escritura", nullable = false)
    private Integer lecturaEscritura = 0;

    @Column(nullable = false)
    private Integer kinestesico = 0;

    @Column(name = "fecha_realizacion", nullable = false)
    private LocalDateTime fechaRealizacion;

    @PrePersist
    void antesDeGuardar() {
        if (fechaRealizacion == null) fechaRealizacion = LocalDateTime.now();
    }

    /** Estilo con mayor puntaje; en empate gana el orden V, A, R, K. */
    public EstiloVark calcularPredominante() {
        EstiloVark mejor = EstiloVark.VISUAL;
        int max = visual;
        if (auditivo > max) { mejor = EstiloVark.AUDITIVO; max = auditivo; }
        if (lecturaEscritura > max) { mejor = EstiloVark.LECTURA_ESCRITURA; max = lecturaEscritura; }
        if (kinestesico > max) { mejor = EstiloVark.KINESTESICO; }
        return mejor;
    }
}
