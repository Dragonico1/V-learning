package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "insignias_obtenidas")
@Getter
@Setter
@NoArgsConstructor
@IdClass(InsigniaObtenidaId.class)
public class InsigniaObtenida {
    @Id
    @Column(name = "perfil_gamificacion_id")
    private Long perfilGamificacionId;

    @Id
    @Column(name = "insignia_id")
    private Long insigniaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insignia_id", insertable = false, updatable = false)
    private Insignia insignia;

    @Column(name = "fecha_obtencion", nullable = false)
    private LocalDateTime fechaObtencion;

    @PrePersist
    void antesDeGuardar() {
        if (fechaObtencion == null) fechaObtencion = LocalDateTime.now();
    }
}
