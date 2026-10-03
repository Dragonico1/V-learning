package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "estudiantes")
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Estudiante extends Usuario {
    @Column(name = "codigo_estudiante", nullable = false, length = 50, unique = true)
    private String codigoEstudiante;

    @Column(name = "programa_academico", nullable = false, length = 150)
    private String programaAcademico;

    {
        setRol(RolUsuario.ESTUDIANTE);
    }

    public Estudiante(String nombre, String correo, String passwordHash, String codigo, String programa) {
        setNombre(nombre);
        setCorreoInstitucional(correo);
        setPasswordHash(passwordHash);
        setRol(RolUsuario.ESTUDIANTE);
        this.codigoEstudiante = codigo;
        this.programaAcademico = programa;
    }
}
