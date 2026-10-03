package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "posiciones_ranking")
@Getter
@Setter
@NoArgsConstructor
@IdClass(PosicionRankingId.class)
public class PosicionRanking {
    @Id
    @Column(name = "ranking_id")
    private Long rankingId;

    @Id
    @Column(name = "estudiante_id")
    private Long estudianteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", insertable = false, updatable = false)
    private Estudiante estudiante;

    @Column(nullable = false)
    private Integer posicion;

    @Column(nullable = false)
    private Integer puntos = 0;
}
