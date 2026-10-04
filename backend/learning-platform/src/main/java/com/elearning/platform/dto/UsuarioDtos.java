package com.elearning.platform.dto;

import com.elearning.platform.enums.EstadoUsuario;
import com.elearning.platform.enums.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** DTOs de gestión de usuarios (RF-001). */
public final class UsuarioDtos {

    private UsuarioDtos() {}

    public record UsuarioCrearRequest(
            @NotBlank(message = "Escribe el nombre.") @Size(max = 150, message = "Máximo 150 caracteres.") String nombre,
            @NotBlank(message = "Escribe el correo institucional.") @Email(message = "El correo no tiene un formato válido.")
            @Size(max = 180, message = "Máximo 180 caracteres.") String correo,
            @NotNull(message = "Elige el rol.") RolUsuario rol,
            @Size(max = 150, message = "Máximo 150 caracteres.") String programaAcademico,
            @Size(max = 150, message = "Máximo 150 caracteres.") String especialidad,
            @Size(max = 50, message = "Máximo 50 caracteres.") String codigo) {}

    public record EstadoRequest(@NotNull(message = "Elige el estado.") EstadoUsuario estado) {}

    public record UsuarioRespuesta(
            Long id,
            String nombre,
            String correo,
            RolUsuario rol,
            EstadoUsuario estado,
            String codigo,
            String programaAcademico,
            String especialidad,
            LocalDateTime fechaCreacion,
            LocalDateTime ultimoAcceso) {}

    /**
     * Resultado de crear o reenviar credenciales. contrasenaTemporal solo se incluye si el
     * correo no pudo enviarse (entrega manual) o en modo desarrollo; nunca se guarda en claro.
     */
    public record CredencialesRespuesta(
            UsuarioRespuesta usuario,
            boolean enviadoPorCorreo,
            boolean entregaManual,
            String contrasenaTemporal,
            String mensaje) {}

    public record Pagina<T>(List<T> contenido, long total, int pagina, int tamano) {}
}
