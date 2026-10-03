package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "contenidos")
@Getter
@Setter
@NoArgsConstructor
public class Contenido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_id")
    private Modulo modulo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String descripcion;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Column(nullable = false)
    private boolean publicado = false;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FormatoContenido formato;

    @Column(name = "url_recurso", length = 500)
    private String urlRecurso;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String cuerpo;

    public void publicar() {
        this.publicado = true;
        this.fechaPublicacion = LocalDateTime.now();
    }

    public void despublicar() {
        this.publicado = false;
    }
}
