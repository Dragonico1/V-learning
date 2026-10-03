package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "configuraciones_accesibilidad")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionAccesibilidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_accesibilidad_id")
    private PerfilAccesibilidad perfilAccesibilidad;

    @Column(name = "alto_contraste", nullable = false)
    private boolean altoContraste = false;

    @Column(name = "tamano_fuente", nullable = false)
    private Integer tamanoFuente = 16;

    @Column(length = 100)
    private String tipografia;

    @Column(name = "espaciado_linea", precision = 5, scale = 2)
    private BigDecimal espaciadoLinea;

    @Column(name = "navegacion_teclado", nullable = false)
    private boolean navegacionTeclado = false;

    @Column(name = "lector_pantalla", nullable = false)
    private boolean lectorPantalla = false;

    @Column(nullable = false)
    private boolean subtitulos = false;

    @Column(nullable = false)
    private boolean transcripcion = false;

    @Column(name = "texto_a_voz", nullable = false)
    private boolean textoAVoz = false;

    @Column(name = "tiempo_adicional", nullable = false)
    private Integer tiempoAdicional = 0;

    @Column(name = "fecha_configuracion", nullable = false)
    private LocalDateTime fechaConfiguracion;

    @PrePersist
    void antesDeGuardar() {
        if (fechaConfiguracion == null) fechaConfiguracion = LocalDateTime.now();
    }
}
