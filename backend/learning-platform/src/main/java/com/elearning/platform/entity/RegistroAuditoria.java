package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "registros_auditoria")
@Getter
@Setter
@NoArgsConstructor
public class RegistroAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 150)
    private String accion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ResultadoAuditoria resultado;

    @Column(length = 200)
    private String recurso;

    @PrePersist
    void antesDeGuardar() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}
