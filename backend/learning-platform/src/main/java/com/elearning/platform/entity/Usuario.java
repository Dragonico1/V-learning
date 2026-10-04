package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "correo_institucional", nullable = false, length = 180, unique = true)
    private String correoInstitucional;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RolUsuario rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoUsuario estado = EstadoUsuario.PENDIENTE_PRIMER_ACCESO;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "notificaciones_habilitadas", nullable = false)
    private boolean notificacionesHabilitadas = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_preferido", nullable = false, length = 50)
    private CanalNotificacion canalPreferido = CanalNotificacion.PLATAFORMA;

    @PrePersist
    void antesDeGuardar() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }

    /**
     * Bloqueo temporal. Si la cuenta está en primer acceso conserva ese estado
     * (para no saltarse el cambio de contraseña) y el bloqueo lo da bloqueadoHasta.
     */
    public void bloquear(LocalDateTime hasta) {
        if (this.estado == EstadoUsuario.ACTIVO) {
            this.estado = EstadoUsuario.BLOQUEADO;
        }
        this.bloqueadoHasta = hasta;
    }

    public void desbloquear() {
        if (this.estado == EstadoUsuario.BLOQUEADO) {
            this.estado = EstadoUsuario.ACTIVO;
        }
        this.bloqueadoHasta = null;
        this.intentosFallidos = 0;
    }

    public void incrementarIntentosFallidos() {
        this.intentosFallidos = (intentosFallidos == null ? 0 : intentosFallidos) + 1;
    }

    public void reiniciarIntentosFallidos() {
        this.intentosFallidos = 0;
    }

    public boolean estaBloqueadoTemporalmente() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now());
    }
}
