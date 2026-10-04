package com.elearning.platform.dto;

import com.elearning.platform.enums.CanalNotificacion;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/** DTOs de notificaciones (RF-011). */
public final class NotificacionDtos {

    private NotificacionDtos() {}

    public record NotificacionDto(Long id, String titulo, String mensaje, LocalDateTime fechaCreacion,
                                  LocalDateTime fechaLectura, CanalNotificacion canal, boolean leida) {}

    public record ListaNotificaciones(long noLeidas, List<NotificacionDto> notificaciones) {}

    /** Opciones: "1_DIA", "3_DIAS", "1_SEMANA". */
    public record PosponerRequest(@NotNull(message = "Elige por cuánto tiempo posponer.") String opcion) {}

    public record PreferenciasRequest(@NotNull Boolean habilitadas, CanalNotificacion canal) {}

    public record PreferenciasDto(boolean habilitadas, CanalNotificacion canal) {}
}
