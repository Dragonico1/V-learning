package com.elearning.platform.dto;

import com.elearning.platform.dto.CursoDtos.RecursoRespuesta;
import com.elearning.platform.enums.EstadoProgreso;
import com.elearning.platform.enums.FormatoContenido;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.List;

/** DTOs de la lección, el apoyo cognitivo y el progreso (RF-005, RF-019, RF-022). */
public final class ContenidoDtos {

    private ContenidoDtos() {}

    /**
     * opcionesAlternativas: formas de consumir el mismo contenido (RF-019): VIDEO_SUBTITULADO, AUDIO,
     * TRANSCRIPCION o TEXTO_EQUIVALENTE, según lo que esté disponible.
     */
    public record ContenidoVista(
            Long id,
            Long cursoId,
            String titulo,
            String descripcion,
            FormatoContenido formato,
            String urlRecurso,
            String cuerpo,
            Integer duracionMinutos,
            List<RecursoRespuesta> recursos,
            List<String> opcionesAlternativas,
            boolean lenguaSenasDisponible,
            String mensajeLenguaSenas,
            boolean apoyoCognitivo,
            List<String> advertencias,
            EstadoProgreso estado,
            BigDecimal porcentaje,
            Long tiempoConsumido) {}

    public record UnidadApoyo(int numero, String titulo, String texto) {}

    public record TerminoGlosario(String termino, String definicion) {}

    public record ApoyoCognitivoRespuesta(
            List<UnidadApoyo> unidades,
            List<String> pasos,
            String resumen,
            boolean resumenAceptable,
            List<TerminoGlosario> glosario,
            List<String> conceptosClave,
            String versionSimplificada,
            List<String> mensajes) {}

    public record ProgresoRequest(
            @Min(value = 0, message = "Los segundos no pueden ser negativos.")
            @Max(value = 86400, message = "Máximo 86400 segundos por registro.") Long segundos,
            @DecimalMin(value = "0", message = "El porcentaje va de 0 a 100.")
            @DecimalMax(value = "100", message = "El porcentaje va de 0 a 100.") BigDecimal porcentaje,
            Boolean completado) {}

    public record ProgresoRespuesta(
            EstadoProgreso estado,
            BigDecimal porcentaje,
            Long tiempoConsumido,
            BigDecimal cursoPorcentaje,
            boolean moduloCompleto,
            boolean cursoFinalizado) {}
}
