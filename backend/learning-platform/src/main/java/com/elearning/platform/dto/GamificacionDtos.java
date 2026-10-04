package com.elearning.platform.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** DTOs de gamificación (RF-009). */
public final class GamificacionDtos {

    private GamificacionDtos() {}

    public record InsigniaObtenidaDto(Long id, String nombre, String descripcion, LocalDateTime fechaObtencion) {}

    public record InsigniaPendienteDto(Long id, String nombre, String descripcion, int puntosRequeridos, int faltan) {}

    public record LogrosRespuesta(int puntos, int nivel, int puntosSiguienteNivel, int progresoNivelPorcentaje,
                                  boolean mostrarEnRanking, List<InsigniaObtenidaDto> insignias,
                                  List<InsigniaPendienteDto> pendientes) {}

    public record PrivacidadRequest(@NotNull(message = "Indica si quieres aparecer en la clasificación.") Boolean mostrarEnRanking) {}

    public record PosicionDto(int posicion, String nombre, int puntos, boolean esYo) {}

    public record RankingRespuesta(Long cursoId, String periodo, LocalDateTime actualizadoEn, List<PosicionDto> posiciones,
                                   Integer miPosicion, boolean modoPrivado) {}

    // ----- Administración -----
    public record ReglaRequest(
            @NotBlank(message = "Escribe el nombre.") @Size(max = 150, message = "Máximo 150 caracteres.") String nombre,
            String descripcion,
            @NotBlank(message = "Elige el evento.") String evento,
            @NotNull(message = "Indica los puntos.") @Min(value = 0, message = "No puede ser negativo.") Integer puntos,
            Boolean activo) {}

    public record ReglaRespuesta(Long id, String nombre, String descripcion, String evento, int puntos, boolean activo,
                                 LocalDateTime fechaActualizacion) {}

    public record InsigniaRequest(
            @NotBlank(message = "Escribe el nombre.") @Size(max = 150, message = "Máximo 150 caracteres.") String nombre,
            String descripcion,
            @NotNull(message = "Indica los puntos requeridos.") @Min(value = 0, message = "No puede ser negativo.") Integer puntosRequeridos) {}

    public record InsigniaRespuesta(Long id, String nombre, String descripcion, int puntosRequeridos) {}
}
