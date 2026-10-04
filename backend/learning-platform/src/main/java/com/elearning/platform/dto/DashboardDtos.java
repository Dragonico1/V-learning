package com.elearning.platform.dto;

import com.elearning.platform.dto.CursoDtos.CursoResumen;
import com.elearning.platform.enums.Clasificacion;
import com.elearning.platform.enums.EstadoInscripcion;
import com.elearning.platform.enums.FormatoContenido;

import java.math.BigDecimal;
import java.util.List;

/** DTOs del dashboard de progreso (RF-006). Cada gráfica del frontend se apoya en estas tablas de datos. */
public final class DashboardDtos {

    private DashboardDtos() {}

    public record CursoProgreso(Long cursoId, String titulo, BigDecimal porcentaje, EstadoInscripcion estado,
                                long modulosCompletos, long modulosTotales, long contenidosCompletados,
                                long tiempoConsumidoSegundos) {}

    public record Calificacion(Long evaluacionId, String evaluacion, String curso, BigDecimal mejorPorcentaje,
                               long intentos, Clasificacion clasificacion) {}

    public record Pendiente(String tipo, Long id, String titulo, String curso) {}

    public record Sugerencia(Long contenidoId, String titulo, FormatoContenido formato, String curso, String motivo) {}

    public record Totales(long tiempoTotalSegundos, long contenidosCompletados, long evaluacionesRealizadas,
                          BigDecimal promedioCalificacion) {}

    public record DashboardRespuesta(boolean sinActividad, String mensaje, Totales totales, List<CursoProgreso> cursos,
                                     List<Calificacion> calificaciones, List<Pendiente> pendientes,
                                     List<Sugerencia> sugerencias, List<CursoResumen> cursosSugeridos) {}
}
