package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "instructores")
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Instructor extends Usuario {
    @Column(name = "codigo_instructor", nullable = false, length = 50, unique = true)
    private String codigoInstructor;

    @Column(length = 150)
    private String especialidad;

    {
        setRol(RolUsuario.INSTRUCTOR);
    }

    public Instructor(String nombre, String correo, String passwordHash, String codigo, String especialidad) {
        setNombre(nombre);
        setCorreoInstitucional(correo);
        setPasswordHash(passwordHash);
        this.codigoInstructor = codigo;
        this.especialidad = especialidad;
    }
}
