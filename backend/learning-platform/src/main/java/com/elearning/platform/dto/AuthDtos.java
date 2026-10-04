package com.elearning.platform.dto;

import com.elearning.platform.enums.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** DTOs de autenticación (RF-002, RF-003). */
public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "Escribe tu correo institucional.") @Email(message = "El correo no tiene un formato válido.") String correo,
            @NotBlank(message = "Escribe tu contraseña.") String password) {}

    public record OtpRequest(
            @NotBlank(message = "Escribe tu correo institucional.") @Email(message = "El correo no tiene un formato válido.") String correo,
            @NotBlank(message = "Escribe el código.") @Pattern(regexp = "\\d{6}", message = "El código tiene 6 números.") String codigo) {}

    /** codigoOtp solo se devuelve con vlearning.dev.exponer-secretos=true. */
    public record LoginRespuesta(String estado, String mensaje, String codigoOtp) {}

    public record RecuperarRequest(
            @NotBlank(message = "Escribe tu correo institucional.") @Email(message = "El correo no tiene un formato válido.") String correo) {}

    /** enlaceRecuperacion solo se devuelve con vlearning.dev.exponer-secretos=true. */
    public record RecuperarRespuesta(String mensaje, String enlaceRecuperacion) {}

    public record RestablecerRequest(
            @NotBlank(message = "Falta el enlace de recuperación.") String token,
            @NotBlank(message = "Escribe la nueva contraseña.") @Size(max = 72, message = "Máximo 72 caracteres.") String nuevaPassword) {}

    public record PrimerAccesoRequest(
            @NotBlank(message = "Escribe tu contraseña temporal.") String passwordActual,
            @NotBlank(message = "Escribe la nueva contraseña.") @Size(max = 72, message = "Máximo 72 caracteres.") String nuevaPassword) {}

    public record MensajeRespuesta(String mensaje) {}

    /** Usuario actual y banderas de onboarding (null = no aplica al rol). */
    public record PerfilActual(
            Long id,
            String nombre,
            String correo,
            RolUsuario rol,
            String estado,
            boolean primerAcceso,
            Boolean varkCompletado,
            Boolean accesibilidadConfigurada) {}

    public record SesionRespuesta(
            String token,
            String tipo,
            LocalDateTime expiraPorInactividad,
            PerfilActual usuario) {}
}
