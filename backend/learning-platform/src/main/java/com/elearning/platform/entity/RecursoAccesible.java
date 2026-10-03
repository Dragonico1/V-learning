package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "recursos_accesibles")
@Getter
@Setter
@NoArgsConstructor
public class RecursoAccesible {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contenido_id")
    private Contenido contenido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoRecursoAccesible tipo;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String descripcion;

    @Column(nullable = false)
    private boolean disponible = true;
}
