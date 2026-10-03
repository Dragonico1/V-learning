package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "modulos")
@Getter
@Setter
@NoArgsConstructor
public class Modulo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String descripcion;

    @Column(nullable = false)
    private Integer orden;
}
