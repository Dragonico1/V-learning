package com.elearning.platform.services;

import com.elearning.platform.dto.CursoDtos.CursoResumen;
import com.elearning.platform.dto.DashboardDtos.*;
import com.elearning.platform.entity.*;
import com.elearning.platform.enums.*;
import com.elearning.platform.repository.*;
import com.elearning.platform.util.AfinidadVark;
import com.elearning.platform.util.CorreccionEvaluacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RF-006 Dashboard de progreso: avance por curso, calificaciones, pendientes y próximos contenidos
 * sugeridos según el menor desempeño. Sin actividad: mensaje motivador y accesos a cursos.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final String MENSAJE_MOTIVADOR =
            "Aún no tienes actividad, y eso está bien: cada gran avance empieza con un primer paso. Elige un curso y empieza con un contenido corto.";

    private final InscripcionRepository inscripciones;
    private final ProgresoRepository progresos;
    private final IntentoEvaluacionRepository intentos;
    private final ModuloRepository modulos;
    private final ContenidoRepository contenidos;
    private final EvaluacionRepository evaluaciones;
    private final CursoRepository cursos;
    private final PerfilAprendizajeRepository perfiles;
    private final TareaRepository tareas;
    private final EntregaTareaRepository entregas;

    @Transactional(readOnly = true)
    public DashboardRespuesta dashboard(Long estudianteId) {
        List<Inscripcion> insc = inscripciones.delEstudiante(estudianteId).stream()
                .filter(i -> i.getEstado() != EstadoInscripcion.CANCELADA).toList();
        List<Progreso> prog = progresos.delEstudianteConCurso(estudianteId);
        List<IntentoEvaluacion> finalizados = intentos.finalizadosDelEstudiante(estudianteId);

        Set<Long> cursosVigentes = insc.stream().map(i -> i.getCurso().getId()).collect(Collectors.toSet());
        List<EntregaTarea> misEntregas = entregas.delEstudiante(estudianteId).stream()
                .filter(e -> cursosVigentes.contains(e.getTarea().getModulo().getCurso().getId())).toList();

        boolean sinActividad = prog.isEmpty() && finalizados.isEmpty() && misEntregas.isEmpty();
        List<CursoResumen> sugeridos = sinActividad ? cursosParaEmpezar(insc) : List.of();

        // --- avance por curso
        List<CursoProgreso> cursosProg = new ArrayList<>();
        for (Inscripcion i : insc) {
            Long cursoId = i.getCurso().getId();
            long tiempo = prog.stream().filter(p -> p.getContenido().getModulo().getCurso().getId().equals(cursoId))
                    .mapToLong(Progreso::getTiempoConsumido).sum();
            long completados = prog.stream().filter(p -> p.getContenido().getModulo().getCurso().getId().equals(cursoId)
                    && p.getEstado() == EstadoProgreso.COMPLETADO && p.getContenido().isPublicado()).count();
            cursosProg.add(new CursoProgreso(cursoId, i.getCurso().getTitulo(), i.getPorcentajeCompletado(), i.getEstado(),
                    progresos.contarModulosCompletados(estudianteId, cursoId),
                    contenidos.contarModulosConContenidoPublicado(cursoId), completados, tiempo));
        }

        // --- calificaciones: mejor intento por evaluación
        Map<Long, List<IntentoEvaluacion>> porEvaluacion = finalizados.stream()
                .collect(Collectors.groupingBy(i -> i.getEvaluacion().getId(), LinkedHashMap::new, Collectors.toList()));
        List<Calificacion> califs = new ArrayList<>();
        Map<Long, BigDecimal> mejorPct = new HashMap<>();
        for (List<IntentoEvaluacion> lista : porEvaluacion.values()) {
            Evaluacion e = lista.get(0).getEvaluacion();
            BigDecimal mejor = lista.stream().filter(i -> i.getPuntaje() != null)
                    .map(i -> CorreccionEvaluacion.porcentaje(i.getPuntaje(), e.getPuntajeMaximo()))
                    .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            mejorPct.put(e.getId(), mejor);
            califs.add(new Calificacion(e.getId(), e.getTitulo(), e.getModulo().getCurso().getTitulo(), mejor,
                    lista.size(), CorreccionEvaluacion.clasificar(mejor), "EVALUACION"));
        }
        // Las tareas calificadas por el instructor también suman a la nota (porcentaje sobre su puntaje máximo).
        long tareasCalificadas = 0;
        for (EntregaTarea et : misEntregas) {
            if (et.getEstado() != EstadoEntrega.CALIFICADA || et.getPuntaje() == null) continue;
            Tarea t = et.getTarea();
            BigDecimal pct = CorreccionEvaluacion.porcentaje(et.getPuntaje(), t.getPuntajeMaximo());
            califs.add(new Calificacion(t.getId(), t.getTitulo(), t.getModulo().getCurso().getTitulo(), pct, 1,
                    CorreccionEvaluacion.clasificar(pct), "TAREA"));
            tareasCalificadas++;
        }
        BigDecimal promedio = califs.isEmpty() ? BigDecimal.ZERO
                : califs.stream().map(Calificacion::mejorPorcentaje).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(califs.size()), 2, RoundingMode.HALF_UP);

        // --- pendientes (cursos activos): evaluaciones sin intento finalizado y contenidos sin completar
        Set<Long> completadosIds = prog.stream().filter(p -> p.getEstado() == EstadoProgreso.COMPLETADO)
                .map(p -> p.getContenido().getId()).collect(Collectors.toSet());
        List<Pendiente> pendientes = new ArrayList<>();
        List<Sugerencia> sugerencias = new ArrayList<>();
        Optional<PerfilAprendizaje> perfil = perfiles.findByEstudianteId(estudianteId);
        EstiloVark principal = perfil.map(PerfilAprendizaje::getEstiloPredominante).orElse(null);
        EstiloVark secundario = perfil.map(PerfilAprendizaje::getMetodoSecundario).orElse(null);

        Map<Long, Modulo> moduloDe = new LinkedHashMap<>();
        for (Inscripcion i : insc) {
            if (i.getEstado() != EstadoInscripcion.ACTIVA) continue;
            for (Modulo m : modulos.findByCursoIdOrderByOrden(i.getCurso().getId())) moduloDe.put(m.getId(), m);
        }
        if (!moduloDe.isEmpty()) {
            List<Contenido> publicados = contenidos.findByModuloIdInAndPublicadoTrueOrderByIdAsc(moduloDe.keySet());
            for (Evaluacion e : evaluaciones.findByModuloIdInOrderByIdAsc(moduloDe.keySet())) {
                if (!porEvaluacion.containsKey(e.getId())) {
                    pendientes.add(new Pendiente("EVALUACION", e.getId(), e.getTitulo(), e.getModulo().getCurso().getTitulo(),
                            e.getModulo().getCurso().getId(), e.getModulo().getId()));
                }
            }
            Set<Long> tareasEntregadas = misEntregas.stream().map(et -> et.getTarea().getId()).collect(Collectors.toSet());
            for (Tarea t : tareas.deLosModulos(moduloDe.keySet())) {
                if (!tareasEntregadas.contains(t.getId())) {
                    pendientes.add(new Pendiente("TAREA", t.getId(), t.getTitulo(), t.getModulo().getCurso().getTitulo(),
                            t.getModulo().getCurso().getId(), t.getModulo().getId()));
                }
            }
            for (Contenido c : publicados) {
                if (!completadosIds.contains(c.getId())) {
                    pendientes.add(new Pendiente("CONTENIDO", c.getId(), c.getTitulo(), c.getModulo().getCurso().getTitulo(),
                            c.getModulo().getCurso().getId(), c.getModulo().getId()));
                }
            }
            // --- sugerencias: módulos con la evaluación de menor porcentaje (< 85) primero
            List<Calificacion> peores = califs.stream().filter(c -> "EVALUACION".equals(c.tipo()) && c.mejorPorcentaje().compareTo(CorreccionEvaluacion.UMBRAL_EXCELENTE) < 0)
                    .sorted(Comparator.comparing(Calificacion::mejorPorcentaje)).toList();
            for (Calificacion c : peores) {
                Evaluacion e = evaluaciones.findById(c.evaluacionId()).orElse(null);
                if (e == null) continue;
                publicados.stream().filter(x -> x.getModulo().getId().equals(e.getModulo().getId()))
                        .sorted(Comparator.comparingInt((Contenido x) -> completadosIds.contains(x.getId()) ? 1 : 0)
                                .thenComparingInt(x -> AfinidadVark.rangoContenido(x.getFormato(), principal, secundario)))
                        .limit(2).forEach(x -> sugerencias.add(new Sugerencia(x.getId(), x.getTitulo(), x.getFormato(),
                                x.getModulo().getCurso().getTitulo(),
                                "Tu resultado en «" + e.getTitulo() + "» fue " + c.mejorPorcentaje() + " %: conviene reforzar este tema.")));
                if (sugerencias.size() >= 4) break;
            }
            if (sugerencias.size() < 4) {
                Set<Long> yaSugeridos = sugerencias.stream().map(Sugerencia::contenidoId).collect(Collectors.toSet());
                publicados.stream().filter(x -> !completadosIds.contains(x.getId()) && !yaSugeridos.contains(x.getId()))
                        .sorted(Comparator.comparingInt(x -> AfinidadVark.rangoContenido(x.getFormato(), principal, secundario)))
                        .limit(4 - sugerencias.size())
                        .forEach(x -> sugerencias.add(new Sugerencia(x.getId(), x.getTitulo(), x.getFormato(),
                                x.getModulo().getCurso().getTitulo(), "Es lo siguiente por avanzar en tus cursos.")));
            }
        }

        Totales totales = new Totales(prog.stream().mapToLong(Progreso::getTiempoConsumido).sum(),
                prog.stream().filter(p -> p.getEstado() == EstadoProgreso.COMPLETADO).count(), finalizados.size(), promedio, tareasCalificadas);
        return new DashboardRespuesta(sinActividad, sinActividad ? MENSAJE_MOTIVADOR : null, totales, cursosProg, califs,
                pendientes, sugerencias, sugeridos);
    }

    private List<CursoResumen> cursosParaEmpezar(List<Inscripcion> insc) {
        Set<Long> inscritos = insc.stream().map(i -> i.getCurso().getId()).collect(Collectors.toSet());
        List<CursoResumen> lista = new ArrayList<>();
        for (Inscripcion i : insc) {
            lista.add(new CursoResumen(i.getCurso().getId(), i.getCurso().getTitulo(), i.getCurso().getDescripcion(),
                    i.getCurso().getInstructor().getNombre(), i.getCurso().getEstado(), true, i.getPorcentajeCompletado()));
        }
        for (Curso c : cursos.publicados(EstadoCurso.PUBLICADO)) {
            if (lista.size() >= 3) break;
            if (!inscritos.contains(c.getId())) {
                lista.add(new CursoResumen(c.getId(), c.getTitulo(), c.getDescripcion(), c.getInstructor().getNombre(),
                        c.getEstado(), false, BigDecimal.ZERO));
            }
        }
        return lista.stream().limit(3).toList();
    }
}
