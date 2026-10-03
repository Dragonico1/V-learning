package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "opciones_respuesta")
@Getter
@Setter
@NoArgsConstructor
public class OpcionRespuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pregunta_id")
    private Pregunta pregunta;

    @Column(nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String texto;

    @Column(nullable = false)
    private boolean correcta = false;
}
