package com.elearning.platform.dto;

import com.elearning.platform.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** DTOs de evaluaciones (RF-007), retroalimentación (RF-008) y su autoría por el instructor. */
public final class EvaluacionDtos {

    private EvaluacionDtos() {}

    // ---------- Instructor ----------
    public record EvaluacionRequest(
            @NotBlank(message = "Escribe el título.") @Size(max = 200, message = "Máximo 200 caracteres.") String titulo,
            String descripcion,
            @NotNull(message = "Elige el tipo.") TipoEvaluacion tipo,
            @NotNull(message = "Indica el puntaje máximo.") @DecimalMin(value = "0.01", message = "Debe ser mayor que 0.") BigDecimal puntajeMaximo,
            @Min(value = 1, message = "Mínimo 1 minuto.") Integer tiempoLimite,
            Boolean alternativaAccesible) {}

    public record OpcionRequest(
            @NotBlank(message = "Escribe el texto de la opción.") String texto,
            boolean correcta) {}

    public record PreguntaRequest(
            @NotBlank(message = "Escribe el enunciado.") String enunciado,
            @NotNull(message = "Elige el tipo de pregunta.") TipoPregunta tipo,
            @NotNull(message = "Indica el puntaje.") @DecimalMin(value = "0", message = "No puede ser negativo.") BigDecimal puntaje,
            @Min(value = 1, message = "El orden empieza en 1.") Integer orden,
            @NotEmpty(message = "Agrega las opciones.") @Size(max = 10, message = "Máximo 10 opciones.") List<@Valid OpcionRequest> opciones) {}

    public record EvaluacionCreada(Long id, String titulo, TipoEvaluacion tipo, BigDecimal puntajeMaximo,
                                   Integer tiempoLimite, boolean alternativaAccesible, int preguntas) {}

    public record PreguntaCreada(Long id, int orden, TipoPregunta tipo, BigDecimal puntaje) {}

    // ---------- Estudiante ----------
    public record EvaluacionEstudiante(Long id, String titulo, String descripcion, TipoEvaluacion tipo,
                                       BigDecimal puntajeMaximo, Integer tiempoLimiteMinutos, boolean calificable,
                                       String aviso, boolean habilitada, String motivoNoHabilitada, long intentos,
                                       BigDecimal mejorPorcentaje, Long intentoEnProgresoId) {}

    public record OpcionVista(Long id, String texto) {}

    public record PreguntaVista(Long id, String enunciado, TipoPregunta tipo, BigDecimal puntaje, int orden,
                                List<OpcionVista> opciones) {}

    public record IntentoVista(Long id, Long evaluacionId, String titulo, TipoEvaluacion tipo, EstadoIntento estado,
                               LocalDateTime fechaInicio, Integer tiempoLimiteMinutos, Long segundosRestantes,
                               List<PreguntaVista> preguntas, Map<Long, String> respuestas, boolean reanudado) {}

    public record RespuestaRequest(@Size(max = 2000, message = "Máximo 2000 caracteres.") String valor) {}

    public record GuardadoRespuesta(Long preguntaId, boolean guardada, Long segundosRestantes) {}

    public record IntentoHistorial(Long id, EstadoIntento estado, LocalDateTime fechaInicio,
                                   LocalDateTime fechaFinalizacion, BigDecimal puntaje, BigDecimal porcentaje,
                                   Clasificacion clasificacion) {}

    public record DetallePregunta(Long preguntaId, String enunciado, TipoPregunta tipo, BigDecimal puntaje,
                                  BigDecimal obtenido, boolean correcta, String tuRespuesta, List<String> respuestaCorrecta) {}

    public record Recomendacion(Long contenidoId, String titulo, FormatoContenido formato, String motivo) {}

    public record ResultadoIntento(Long intentoId, Long evaluacionId, String titulo, BigDecimal puntaje,
                                   BigDecimal puntajeMaximo, BigDecimal porcentaje, Clasificacion clasificacion,
                                   String mensaje, List<Recomendacion> recomendaciones, List<DetallePregunta> detalle,
                                   BigDecimal mejorPorcentajePrevio, boolean mejoro) {}
}
