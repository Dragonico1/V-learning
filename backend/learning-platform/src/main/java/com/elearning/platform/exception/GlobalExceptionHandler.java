package com.elearning.platform.exception;

import com.elearning.platform.dto.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorRespuesta> api(ApiException e, HttpServletRequest req) {
        return ResponseEntity.status(e.getEstado())
                .body(ErrorRespuesta.de(e.getCodigo(), e.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> detalles = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> detalles.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorRespuesta("DATOS_INVALIDOS",
                "Revisa los campos marcados y vuelve a intentarlo.", detalles, LocalDateTime.now(), req.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorRespuesta> restricciones(ConstraintViolationException e, HttpServletRequest req) {
        Map<String, String> detalles = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> detalles.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return ResponseEntity.badRequest().body(new ErrorRespuesta("DATOS_INVALIDOS",
                "Revisa los datos enviados.", detalles, LocalDateTime.now(), req.getRequestURI()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorRespuesta> solicitudMalFormada(Exception e, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ErrorRespuesta.de("SOLICITUD_INVALIDA",
                "No pudimos entender la solicitud. Revisa los datos enviados.", req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorRespuesta> denegado(AccessDeniedException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorRespuesta.de("ACCESO_DENEGADO",
                "No tienes permiso para realizar esta acción.", req.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorRespuesta> noAutenticado(AuthenticationException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorRespuesta.de("NO_AUTENTICADO",
                "Tu sesión no es válida. Inicia sesión de nuevo.", req.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> metodo(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ErrorRespuesta.de("METODO_NO_PERMITIDO",
                "Esta operación no está disponible.", req.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> recurso(NoResourceFoundException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorRespuesta.de("NO_ENCONTRADO",
                "No encontramos lo que buscas.", req.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("Violación de integridad en {}: {}", req.getRequestURI(), e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorRespuesta.de("CONFLICTO_DE_DATOS",
                "No se pudo guardar: el dato ya existe o está en uso.", req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception e, HttpServletRequest req) {
        log.error("Error inesperado en {}", req.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorRespuesta.de("ERROR_INTERNO",
                "Ocurrió un problema de nuestro lado. Inténtalo de nuevo en unos minutos.", req.getRequestURI()));
    }
}
