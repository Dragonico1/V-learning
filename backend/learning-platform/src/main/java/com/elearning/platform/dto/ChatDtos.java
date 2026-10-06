package com.elearning.platform.dto;

import com.elearning.platform.enums.EstadoMensaje;
import com.elearning.platform.enums.RolUsuario;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/** DTOs de la comunidad del curso (RF-010). */
public final class ChatDtos {

    private ChatDtos() {}

    public record MensajeRequest(@NotBlank(message = "Escribe un mensaje.") String contenido) {}

    /** Mensaje reportado pendiente de la decisión del instructor. */
    public record MensajeReportado(Long id, Long cursoId, String curso, Long autorId, String autor, RolUsuario rolAutor,
                                   String contenido, LocalDateTime fechaEnvio, LocalDateTime fechaReporte,
                                   java.util.List<String> reportadoPor) {}

    public record MensajeDto(Long id, Long cursoId, Long usuarioId, String autor, RolUsuario rol, String contenido,
                             LocalDateTime fechaEnvio, EstadoMensaje estado) {}
}
