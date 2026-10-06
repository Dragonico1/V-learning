package com.elearning.platform.dto;

import com.elearning.platform.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs de cursos, módulos, contenidos y recursos accesibles (RF-005, RF-018..022). */
public final class CursoDtos {

    private CursoDtos() {}

    public record CursoResumen(Long id, String titulo, String descripcion, String instructor, EstadoCurso estado,
                               boolean inscrito, BigDecimal porcentaje) {}

    public record CursoRequest(
            @NotBlank(message = "Escribe el título del curso.") @Size(max = 200, message = "Máximo 200 caracteres.") String titulo,
            String descripcion) {}

    public record ModuloRequest(
            @NotBlank(message = "Escribe el título del módulo.") @Size(max = 200, message = "Máximo 200 caracteres.") String titulo,
            String descripcion,
            @Min(value = 1, message = "El orden empieza en 1.") Integer orden) {}

    public record ContenidoRequest(
            @NotBlank(message = "Escribe el título del contenido.") @Size(max = 200, message = "Máximo 200 caracteres.") String titulo,
            String descripcion,
            @Min(value = 0, message = "La duración no puede ser negativa.") Integer duracionMinutos,
            @NotNull(message = "Elige el formato.") FormatoContenido formato,
            @Size(max = 500, message = "Máximo 500 caracteres.") String urlRecurso,
            String cuerpo) {}

    /** Para SUBTITULO, AUDIO y LENGUA_SENAS se exige url; para el resto, el texto va en descripcion. */
    public record RecursoRequest(
            @NotNull(message = "Elige el tipo de recurso.") TipoRecursoAccesible tipo,
            @Size(max = 500, message = "Máximo 500 caracteres.") String url,
            String descripcion) {}

    public record RecursoRespuesta(Long id, TipoRecursoAccesible tipo, String url, String descripcion, boolean disponible) {}

    public record ContenidoItem(Long id, String titulo, FormatoContenido formato, Integer duracionMinutos,
                                boolean publicado, boolean afin, EstadoProgreso estado, BigDecimal porcentaje,
                                Boolean conforme, List<String> faltantes) {}

    public record EvaluacionItem(Long id, String titulo, TipoEvaluacion tipo, boolean calificable, String aviso,
                                 LocalDateTime fechaLimite, BigDecimal puntajeMaximo, Integer tiempoLimite,
                                 String descripcion) {}

    public record ModuloItem(Long id, String titulo, String descripcion, Integer orden, boolean sinFormatoAfin,
                             boolean completo, List<ContenidoItem> contenidos, List<EvaluacionItem> evaluaciones) {}

    public record CursoDetalle(Long id, String titulo, String descripcion, String instructor, EstadoCurso estado,
                               boolean inscrito, BigDecimal porcentaje, EstiloVark metodoPrincipal,
                               EstiloVark metodoSecundario, List<ModuloItem> modulos) {}

    public record InscribirRequest(
            @NotEmpty(message = "Indica al menos un correo.") @Size(max = 200, message = "Máximo 200 correos por solicitud.")
            List<@NotBlank(message = "Hay un correo vacío.") String> correos) {}

    public record InscripcionResultado(List<String> inscritos, List<String> yaInscritos, List<String> noEncontrados) {}

    public record ConformidadRespuesta(boolean conforme, List<String> faltantes, List<String> advertencias) {}

    public record PublicacionRespuesta(Long id, boolean publicado, List<String> advertencias) {}
}
