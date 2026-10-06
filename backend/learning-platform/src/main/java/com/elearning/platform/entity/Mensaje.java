package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "mensajes")
@Getter
@Setter
@NoArgsConstructor
public class Mensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Column(nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String contenido;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoMensaje estado = EstadoMensaje.ENVIADO;

    @Column(name = "fecha_reporte")
    private LocalDateTime fechaReporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderador_id")
    private Usuario moderador;

    @Column(name = "fecha_moderacion")
    private LocalDateTime fechaModeracion;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ResolucionMensaje resolucion;

    @PrePersist
    void antesDeGuardar() {
        if (fechaEnvio == null) fechaEnvio = LocalDateTime.now();
    }
}
