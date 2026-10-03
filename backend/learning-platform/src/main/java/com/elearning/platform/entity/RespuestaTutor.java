package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "respuestas_tutor")
@Getter
@Setter
@NoArgsConstructor
public class RespuestaTutor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consulta_id")
    private ConsultaTutor consulta;

    @Column(nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoRespuestaTutor tipo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private boolean util = false;

    @PrePersist
    void antesDeGuardar() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}
