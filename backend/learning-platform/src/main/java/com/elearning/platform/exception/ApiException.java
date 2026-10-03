package com.elearning.platform.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Error de negocio con código estable para el frontend y mensaje claro para la persona. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;

    public ApiException(HttpStatus estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje);
    }

    public static ApiException solicitudInvalida(String codigo, String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, codigo, mensaje);
    }

    public static ApiException conflicto(String codigo, String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, codigo, mensaje);
    }

    public static ApiException prohibido(String codigo, String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, codigo, mensaje);
    }

    public static ApiException noAutenticado(String codigo, String mensaje) {
        return new ApiException(HttpStatus.UNAUTHORIZED, codigo, mensaje);
    }
}
