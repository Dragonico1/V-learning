package com.elearning.platform.services;

import com.elearning.platform.dto.EvaluacionDtos.Recomendacion;
import com.elearning.platform.entity.Contenido;
import com.elearning.platform.entity.Evaluacion;
import com.elearning.platform.entity.PerfilAprendizaje;
import com.elearning.platform.entity.Progreso;
import com.elearning.platform.enums.Clasificacion;
import com.elearning.platform.enums.EstadoProgreso;
import com.elearning.platform.enums.EstiloVark;
import com.elearning.platform.repository.ContenidoRepository;
import com.elearning.platform.repository.PerfilAprendizajeRepository;
import com.elearning.platform.repository.ProgresoRepository;
import com.elearning.platform.util.AfinidadVark;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RF-008: mensaje según la clasificación y recomendaciones de refuerzo priorizadas por el método
 * VARK. Sin recursos de refuerzo: mensaje genérico y aviso al instructor.
 */
@Service
@RequiredArgsConstructor
public class RetroalimentacionService {

    public record Retro(Clasificacion clasificacion, String mensaje, List<Recomendacion> recomendaciones) {}

    private final ContenidoRepository contenidos;
    private final ProgresoRepository progresos;
    private final PerfilAprendizajeRepository perfiles;
    private final NotificacionService notificaciones;

    @Transactional
    public Retro generar(Long estudianteId, Evaluacion evaluacion, java.math.BigDecimal porcentaje) {
        Clasificacion clase = com.elearning.platform.util.CorreccionEvaluacion.clasificar(porcentaje);
        if (clase == Clasificacion.EXCELENTE) {
            return new Retro(clase, "¡Excelente trabajo! Dominas este tema. Puedes avanzar al siguiente módulo.", List.of());
        }
        String base = clase == Clasificacion.ACEPTABLE
                ? "Buen avance. Ya entiendes lo principal; repasa estos materiales para afianzar lo que falta."
                : "Este tema todavía necesita refuerzo, y está bien: puedes repetir la evaluación cuando quieras. Empieza por estos materiales.";

        List<Contenido> publicados = contenidos.findByModuloIdInAndPublicadoTrueOrderByIdAsc(List.of(evaluacion.getModulo().getId()));
        if (publicados.isEmpty()) {
            notificaciones.notificarUnaVezAlDia(evaluacion.getModulo().getCurso().getInstructor().getId(),
                    "Faltan recursos de refuerzo en «" + evaluacion.getModulo().getTitulo() + "»",
                    "Un estudiante obtuvo un resultado " + clase + " en «" + evaluacion.getTitulo()
                            + "» y el módulo no tiene contenidos publicados para reforzar.");
            return new Retro(clase, "Repasa los apuntes del módulo e inténtalo de nuevo cuando te sientas listo o lista. "
                    + "Avisamos al equipo para que agregue más materiales de apoyo.", List.of());
        }
        Optional<PerfilAprendizaje> perfil = perfiles.findByEstudianteId(estudianteId);
        EstiloVark principal = perfil.map(PerfilAprendizaje::getEstiloPredominante).orElse(null);
        EstiloVark secundario = perfil.map(PerfilAprendizaje::getMetodoSecundario).orElse(null);
        Map<Long, Progreso> prog = progresos.findByEstudianteIdAndContenidoIdIn(estudianteId,
                        publicados.stream().map(Contenido::getId).toList())
                .stream().collect(Collectors.toMap(p -> p.getContenido().getId(), p -> p, (a, b) -> a));

        List<Contenido> orden = new ArrayList<>(publicados);
        orden.sort(Comparator
                .comparingInt((Contenido c) -> completado(prog.get(c.getId())) ? 1 : 0)
                .thenComparingInt(c -> AfinidadVark.rangoContenido(c.getFormato(), principal, secundario))
                .thenComparing(Contenido::getId));
        int max = clase == Clasificacion.INSUFICIENTE ? 4 : 3;
        List<Recomendacion> recs = new ArrayList<>();
        for (Contenido c : orden.subList(0, Math.min(max, orden.size()))) {
            int rango = AfinidadVark.rangoContenido(c.getFormato(), principal, secundario);
            String motivo = rango == 0 ? "Formato afín a tu método principal"
                    : rango == 1 ? "Formato afín a tu segundo método"
                    : "Para reforzar el tema";
            recs.add(new Recomendacion(c.getId(), c.getTitulo(), c.getFormato(), motivo));
        }
        return new Retro(clase, base, recs);
    }

    private static boolean completado(Progreso p) {
        return p != null && p.getEstado() == EstadoProgreso.COMPLETADO;
    }
}
