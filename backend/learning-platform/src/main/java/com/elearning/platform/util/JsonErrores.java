package com.elearning.platform.util;

import com.elearning.platform.dto.ErrorRespuesta;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Escribe errores JSON desde filtros (fuera de los controladores). */
public final class JsonErrores {

    private JsonErrores() {}

    public static void escribir(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                                int estado, String codigo, String mensaje) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(estado);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(),
                ErrorRespuesta.de(codigo, mensaje, request.getRequestURI()));
    }
}
