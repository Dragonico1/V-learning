package com.elearning.platform.dto;

import java.time.LocalDateTime;
import java.util.Map;

/** Cuerpo estándar de error. Los mensajes están en lenguaje claro. */
public record ErrorRespuesta(
        String codigo,
        String mensaje,
        Map<String, String> detalles,
        LocalDateTime timestamp,
        String ruta) {

    public static ErrorRespuesta de(String codigo, String mensaje, String ruta) {
        return new ErrorRespuesta(codigo, mensaje, null, LocalDateTime.now(), ruta);
    }
}
