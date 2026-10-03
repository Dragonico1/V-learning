package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "perfiles_aprendizaje")
@Getter
@Setter
@NoArgsConstructor
public class PerfilAprendizaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @Enumerated(EnumType.STRING)
    @Column(name = "estilo_predominante", length = 50)
    private EstiloVark estiloPredominante;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_secundario", length = 50)
    private EstiloVark metodoSecundario;

    @Column(name = "fecha_evaluacion")
    private LocalDateTime fechaEvaluacion;

    @Column(name = "version_test", length = 50)
    private String versionTest;

    public void actualizarEstilo(EstiloVark estilo) {
        this.estiloPredominante = estilo;
        this.fechaEvaluacion = LocalDateTime.now();
        if (estilo != null && estilo == metodoSecundario) this.metodoSecundario = null;
    }
}
