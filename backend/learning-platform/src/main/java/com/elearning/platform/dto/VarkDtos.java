package com.elearning.platform.dto;

import com.elearning.platform.enums.EstiloVark;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** DTOs del cuestionario VARK (RF-004) y de la selección de métodos (RF-005). */
public final class VarkDtos {

    private VarkDtos() {}

    /** id de la opción = nombre del estilo (VISUAL, AUDITIVO, ...). */
    public record OpcionVark(EstiloVark id, String texto) {}

    public record PreguntaVark(int numero, String enunciado, String ayuda, List<OpcionVark> opciones) {}

    public record CuestionarioRespuesta(int total, List<PreguntaVark> preguntas) {}

    public record RespuestaParcialRequest(@NotNull(message = "Elige una opción.") EstiloVark opcion) {}

    public record ParcialRespuesta(int total, int respondidas, Map<Integer, EstiloVark> respuestas) {}

    public record ResultadoVarkRespuesta(
            int visual,
            int auditivo,
            int lecturaEscritura,
            int kinestesico,
            EstiloVark estiloPredominante,
            EstiloVark segundoSugerido,
            LocalDateTime fecha) {}

    public record MetodosRequest(
            @NotNull(message = "Elige el método principal.") EstiloVark principal,
            EstiloVark secundario) {}

    public record MetodosRespuesta(EstiloVark principal, EstiloVark secundario, ResultadoVarkRespuesta ultimoResultado) {}
}
