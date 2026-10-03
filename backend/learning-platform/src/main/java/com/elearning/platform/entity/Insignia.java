package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "insignias")
@Getter
@Setter
@NoArgsConstructor
public class Insignia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150, unique = true)
    private String nombre;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String descripcion;

    @Column(name = "puntos_requeridos", nullable = false)
    private Integer puntosRequeridos;

    public boolean cumpleRequisito(int puntos) { return puntos >= puntosRequeridos; }
}
