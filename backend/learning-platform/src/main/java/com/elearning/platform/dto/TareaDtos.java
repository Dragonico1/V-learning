package com.elearning.platform.dto;

import com.elearning.platform.enums.EstadoEntrega;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTOs de tareas calificables por módulo. */
public final class TareaDtos {

    private TareaDtos() {}

    // ---------- Instructor ----------
    public record TareaRequest(
            @NotBlank(message = "Escribe el título.") @Size(max = 200, message = "Máximo 200 caracteres.") String titulo,
            String descripcion,
            @NotNull(message = "Indica el puntaje máximo.") @DecimalMin(value = "0.01", message = "Debe ser mayor que 0.") BigDecimal puntajeMaximo,
            LocalDateTime fechaLimite) {}

    public record TareaInstructor(Long id, String titulo, String descripcion, BigDecimal puntajeMaximo,
                                  LocalDateTime fechaLimite, long entregas, long calificadas) {}

    public record EntregaVista(Long id, Long estudianteId, String estudiante, String correo, String texto, String enlace,
                               LocalDateTime fechaEntrega, EstadoEntrega estado, BigDecimal puntaje,
                               String retroalimentacion, LocalDateTime fechaCalificacion) {}

    public record CalificacionRequest(
            @NotNull(message = "Indica el puntaje.") @DecimalMin(value = "0", message = "No puede ser negativo.") BigDecimal puntaje,
            @Size(max = 4000, message = "Máximo 4000 caracteres.") String retroalimentacion) {}

    // ---------- Estudiante ----------
    public record TareaEstudiante(Long id, String titulo, String descripcion, BigDecimal puntajeMaximo,
                                  LocalDateTime fechaLimite, boolean vencida, EstadoEntrega estado, String texto,
                                  String enlace, LocalDateTime fechaEntrega, BigDecimal puntaje,
                                  String retroalimentacion, LocalDateTime fechaCalificacion) {}

    public record EntregaRequest(
            @Size(max = 10000, message = "Máximo 10000 caracteres.") String texto,
            @Size(max = 500, message = "Máximo 500 caracteres.") String enlace) {}
}
