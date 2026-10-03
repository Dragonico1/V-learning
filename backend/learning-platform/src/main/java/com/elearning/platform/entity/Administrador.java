package com.elearning.platform.entity;

import com.elearning.platform.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "administradores")
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Administrador extends Usuario {
    {
        setRol(RolUsuario.ADMINISTRADOR);
    }

    public Administrador(String nombre, String correo, String passwordHash) {
        setNombre(nombre);
        setCorreoInstitucional(correo);
        setPasswordHash(passwordHash);
    }
}
