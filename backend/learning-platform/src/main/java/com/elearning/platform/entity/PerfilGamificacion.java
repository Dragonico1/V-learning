package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "perfiles_gamificacion")
@Getter
@Setter
@NoArgsConstructor
public class PerfilGamificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @Column(nullable = false)
    private Integer puntos = 0;

    @Column(nullable = false)
    private Integer nivel = 1;

    @Column(name = "visible_ranking", nullable = false)
    private boolean visibleRanking = true;

    public void agregarPuntos(int p) {
        if (p > 0) this.puntos += p;
    }
}
