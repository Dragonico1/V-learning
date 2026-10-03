package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "respuestas_vark_parciales")
@Getter
@Setter
@NoArgsConstructor
@IdClass(RespuestaVarkParcialId.class)
public class RespuestaVarkParcial {
    @Id
    @Column(name = "perfil_aprendizaje_id")
    private Long perfilAprendizajeId;

    @Id
    @Column(name = "numero_pregunta")
    private Integer numeroPregunta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstiloVark opcion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void antesDeGuardar() {
        fechaActualizacion = LocalDateTime.now();
    }
}
