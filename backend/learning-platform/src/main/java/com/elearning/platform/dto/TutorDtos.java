package com.elearning.platform.dto;

import com.elearning.platform.enums.TipoRespuestaTutor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** DTOs del tutor de IA (RF-024). */
public final class TutorDtos {

    private TutorDtos() {}

    public record ConsultaRequest(
            @NotBlank(message = "Escribe tu pregunta.")
            @Size(max = 1000, message = "La pregunta es muy larga. Máximo 1000 caracteres.") String pregunta,
            Long contenidoId) {}

    public record UtilRequest(@NotNull(message = "Indica si te fue útil.") Boolean util) {}

    public record RespuestaTutorDto(Long id, Long consultaId, String pregunta, String respuesta, TipoRespuestaTutor tipo,
                                    LocalDateTime fecha, boolean util, Long contenidoId) {}
}
