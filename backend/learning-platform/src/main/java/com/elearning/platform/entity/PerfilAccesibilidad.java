package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "perfiles_accesibilidad")
@Getter
@Setter
@NoArgsConstructor
public class PerfilAccesibilidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_accesibilidad_categoria",
            joinColumns = @JoinColumn(name = "perfil_accesibilidad_id"))
    @Column(name = "categoria", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Set<CategoriaAccesibilidad> categorias = new HashSet<>();

    // Lo actualiza el trigger trg_perfiles_accesibilidad_fecha_actualizacion_update
    @Column(name = "fecha_actualizacion", nullable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void antesDeGuardar() {
        if (fechaActualizacion == null) fechaActualizacion = LocalDateTime.now();
    }

    public boolean tiene(CategoriaAccesibilidad c) { return categorias.contains(c); }
}
