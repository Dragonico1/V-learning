package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "informes")
@Getter
@Setter
@NoArgsConstructor
public class Informe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FormatoInforme formato;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String filtros;

    @PrePersist
    void antesDeGuardar() {
        if (fechaGeneracion == null) fechaGeneracion = LocalDateTime.now();
    }
}
