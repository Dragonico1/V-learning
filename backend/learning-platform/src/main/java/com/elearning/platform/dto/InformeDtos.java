package com.elearning.platform.dto;

import com.elearning.platform.enums.FormatoInforme;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs de informes (RF-012 y exportación de progreso RF-006). */
public final class InformeDtos {

    private InformeDtos() {}

    public record InformeRequest(@NotNull(message = "Elige el formato del informe.") FormatoInforme formato,
                                 Long estudianteId, Long cursoId, LocalDate desde, LocalDate hasta) {}

    public record VistaInforme(String titulo, List<String> contexto, List<String> columnas, List<List<String>> filas,
                               List<String> notas, boolean sinDatos, String mensaje) {}

    public record InformeHistorial(Long id, LocalDateTime fechaGeneracion, FormatoInforme formato, String filtros) {}

    public record ArchivoGenerado(String nombre, String tipoContenido, byte[] contenido) {}
}
