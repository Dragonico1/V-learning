package com.elearning.platform.services;

import com.elearning.platform.dto.TareaDtos.*;
import com.elearning.platform.entity.*;
import com.elearning.platform.enums.*;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tareas o actividades calificables por módulo. El instructor las crea y las califica;
 * el estudiante entrega texto y/o un enlace, y puede reenviar hasta que la tarea sea calificada.
 */
@Service
@RequiredArgsConstructor
public class TareaService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AccesoService acceso;
    private final TareaRepository tareas;
    private final EntregaTareaRepository entregas;
    private final ModuloRepository modulos;
    private final EstudianteRepository estudiantes;
    private final InscripcionRepository inscripciones;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;

    // =====================================================================
    // Instructor
    // =====================================================================

    @Transactional(readOnly = true)
    public List<TareaInstructor> listarInstructor(Long moduloId, Long instructorId) {
        acceso.moduloPropio(moduloId, instructorId);
        return tareas.findByModuloIdOrderByIdAsc(moduloId).stream().map(this::aVistaInstructor).toList();
    }

    @Transactional
    public TareaInstructor crear(Long moduloId, Long instructorId, TareaRequest req, String ip) {
        Modulo m = acceso.moduloPropio(moduloId, instructorId);
        Tarea t = new Tarea();
        t.setModulo(m);
        aplicar(t, req);
        t = tareas.save(t);
        auditoria.registrar(instructorId, "TAREA_CREADA", "tareas/" + t.getId(), ResultadoAuditoria.PERMITIDO, ip);
        if (m.getCurso().getEstado() == EstadoCurso.PUBLICADO) {
            String limite = t.getFechaLimite() == null ? "" : " Fecha límite: " + FECHA.format(t.getFechaLimite()) + ".";
            notificaciones.notificarInscritos(m.getCurso().getId(), "Nueva tarea: " + t.getTitulo(),
                    "En «" + m.getCurso().getTitulo() + "» (módulo " + m.getTitulo() + ") hay una tarea calificable de "
                            + t.getPuntajeMaximo().stripTrailingZeros().toPlainString() + " puntos." + limite,
                    "/cursos/" + m.getCurso().getId() + "#m-" + m.getId());
        }
        return aVistaInstructor(t);
    }

    @Transactional
    public TareaInstructor actualizar(Long tareaId, Long instructorId, TareaRequest req, String ip) {
        Tarea t = propia(tareaId, instructorId);
        if (req.puntajeMaximo().compareTo(t.getPuntajeMaximo()) < 0 && entregas.countByTareaIdAndEstado(tareaId, EstadoEntrega.CALIFICADA) > 0) {
            throw ApiException.conflicto("TAREA_CON_CALIFICACIONES",
                    "Esta tarea ya tiene entregas calificadas: no puedes bajar su puntaje máximo.");
        }
        aplicar(t, req);
        tareas.save(t);
        auditoria.registrar(instructorId, "TAREA_ACTUALIZADA", "tareas/" + tareaId, ResultadoAuditoria.PERMITIDO, ip);
        return aVistaInstructor(t);
    }

    @Transactional
    public void eliminar(Long tareaId, Long instructorId, String ip) {
        Tarea t = propia(tareaId, instructorId);
        if (entregas.countByTareaId(tareaId) > 0) {
            throw ApiException.conflicto("TAREA_CON_ENTREGAS", "No se puede borrar una tarea que ya tiene entregas.");
        }
        tareas.delete(t);
        auditoria.registrar(instructorId, "TAREA_ELIMINADA", "tareas/" + tareaId, ResultadoAuditoria.PERMITIDO, ip);
    }

    @Transactional(readOnly = true)
    public List<EntregaVista> entregasDe(Long tareaId, Long instructorId) {
        propia(tareaId, instructorId);
        return entregas.findByTareaIdOrderByFechaEntregaAsc(tareaId).stream()
                .map(e -> new EntregaVista(e.getId(), e.getEstudiante().getId(), e.getEstudiante().getNombre(),
                        e.getEstudiante().getCorreoInstitucional(), e.getTexto(), e.getEnlace(), e.getFechaEntrega(),
                        e.getEstado(), e.getPuntaje(), e.getRetroalimentacion(), e.getFechaCalificacion()))
                .toList();
    }

    @Transactional
    public EntregaVista calificar(Long entregaId, Long instructorId, CalificacionRequest req, String ip) {
        EntregaTarea e = entregas.findById(entregaId).orElseThrow(() -> ApiException.noEncontrado("No encontramos esa entrega."));
        Tarea t = e.getTarea();
        acceso.moduloPropio(t.getModulo().getId(), instructorId);
        if (req.puntaje().compareTo(t.getPuntajeMaximo()) > 0) {
            throw ApiException.solicitudInvalida("PUNTAJE_EXCEDE_MAXIMO",
                    "El puntaje no puede superar " + t.getPuntajeMaximo().stripTrailingZeros().toPlainString() + ".");
        }
        e.setPuntaje(req.puntaje());
        e.setRetroalimentacion(req.retroalimentacion() == null || req.retroalimentacion().isBlank() ? null : req.retroalimentacion().trim());
        e.setEstado(EstadoEntrega.CALIFICADA);
        e.setFechaCalificacion(LocalDateTime.now());
        entregas.save(e);
        auditoria.registrar(instructorId, "TAREA_CALIFICADA", "entregas_tarea/" + entregaId, ResultadoAuditoria.PERMITIDO, ip);
        notificaciones.notificar(e.getEstudiante(), NotificacionService.recortar("Tarea calificada: " + t.getTitulo(), 190),
                "Obtuviste " + e.getPuntaje().stripTrailingZeros().toPlainString() + " de "
                        + t.getPuntajeMaximo().stripTrailingZeros().toPlainString() + " puntos en «" + t.getTitulo() + "».", false,
                "/cursos/" + t.getModulo().getCurso().getId() + "#m-" + t.getModulo().getId());
        return new EntregaVista(e.getId(), e.getEstudiante().getId(), e.getEstudiante().getNombre(),
                e.getEstudiante().getCorreoInstitucional(), e.getTexto(), e.getEnlace(), e.getFechaEntrega(),
                e.getEstado(), e.getPuntaje(), e.getRetroalimentacion(), e.getFechaCalificacion());
    }

    // =====================================================================
    // Estudiante
    // =====================================================================

    @Transactional(readOnly = true)
    public List<TareaEstudiante> listarEstudiante(Long estudianteId, Long moduloId) {
        Modulo m = modulos.findById(moduloId).orElseThrow(() -> ApiException.noEncontrado("No encontramos ese módulo."));
        exigirInscrito(estudianteId, m);
        List<Tarea> lista = tareas.findByModuloIdOrderByIdAsc(moduloId);
        if (lista.isEmpty()) return List.of();
        Map<Long, EntregaTarea> porTarea = entregas
                .findByTareaIdInAndEstudianteId(lista.stream().map(Tarea::getId).toList(), estudianteId).stream()
                .collect(Collectors.toMap(e -> e.getTarea().getId(), Function.identity()));
        List<TareaEstudiante> salida = new ArrayList<>();
        for (Tarea t : lista) salida.add(aVistaEstudiante(t, porTarea.get(t.getId())));
        return salida;
    }

    @Transactional
    public TareaEstudiante entregar(Long estudianteId, Long tareaId, EntregaRequest req, String ip) {
        Tarea t = tareas.findById(tareaId).orElseThrow(() -> ApiException.noEncontrado("No encontramos esa tarea."));
        exigirInscrito(estudianteId, t.getModulo());
        String texto = req.texto() == null ? "" : req.texto().trim();
        String enlace = req.enlace() == null ? "" : req.enlace().trim();
        if (texto.isEmpty() && enlace.isEmpty()) {
            throw ApiException.solicitudInvalida("ENTREGA_VACIA", "Escribe tu respuesta o agrega un enlace a tu trabajo.");
        }
        if (!enlace.isEmpty() && !enlace.matches("(?i)^https?://\\S+$")) {
            throw ApiException.solicitudInvalida("ENLACE_INVALIDO", "El enlace debe empezar por http:// o https://");
        }
        if (t.getFechaLimite() != null && LocalDateTime.now().isAfter(t.getFechaLimite())) {
            throw ApiException.conflicto("PLAZO_VENCIDO", "El plazo de entrega de esta tarea ya venció.");
        }
        EntregaTarea e = entregas.findByTareaIdAndEstudianteId(tareaId, estudianteId).orElse(null);
        if (e != null && e.getEstado() == EstadoEntrega.CALIFICADA) {
            throw ApiException.conflicto("TAREA_YA_CALIFICADA", "Esta tarea ya fue calificada: no se puede reenviar.");
        }
        boolean primera = e == null;
        if (primera) {
            e = new EntregaTarea();
            e.setTarea(t);
            e.setEstudiante(estudiantes.getReferenceById(estudianteId));
        }
        e.setTexto(texto.isEmpty() ? null : texto);
        e.setEnlace(enlace.isEmpty() ? null : enlace);
        e.setFechaEntrega(LocalDateTime.now());
        e = entregas.save(e);
        auditoria.registrar(estudianteId, primera ? "TAREA_ENTREGADA" : "TAREA_REENVIADA", "tareas/" + tareaId, ResultadoAuditoria.PERMITIDO, ip);
        Long instructorId = t.getModulo().getCurso().getInstructor().getId();
        notificaciones.notificarUnaVezAlDia(instructorId, NotificacionService.recortar("Nueva entrega: " + t.getTitulo(), 190),
                "Un estudiante entregó «" + t.getTitulo() + "» en «" + t.getModulo().getCurso().getTitulo() + "». Ya puedes calificarla.",
                "/cursos/" + t.getModulo().getCurso().getId() + "/editar?pestana=evaluaciones#m-" + t.getModulo().getId());
        return aVistaEstudiante(t, e);
    }

    // =====================================================================

    private Tarea propia(Long tareaId, Long instructorId) {
        Tarea t = tareas.findById(tareaId).orElseThrow(() -> ApiException.noEncontrado("No encontramos esa tarea."));
        acceso.moduloPropio(t.getModulo().getId(), instructorId);
        return t;
    }

    private void exigirInscrito(Long estudianteId, Modulo modulo) {
        Curso curso = modulo.getCurso();
        boolean ok = curso.getEstado() == EstadoCurso.PUBLICADO && inscripciones
                .findByEstudianteIdAndCursoId(estudianteId, curso.getId())
                .filter(x -> x.getEstado() != EstadoInscripcion.CANCELADA).isPresent();
        if (!ok) throw ApiException.prohibido("NO_INSCRITO", "Inscríbete en el curso para acceder a sus tareas.");
    }

    private static void aplicar(Tarea t, TareaRequest req) {
        t.setTitulo(req.titulo().trim());
        t.setDescripcion(req.descripcion() == null || req.descripcion().isBlank() ? null : req.descripcion().trim());
        t.setPuntajeMaximo(req.puntajeMaximo());
        t.setFechaLimite(req.fechaLimite());
    }

    private TareaInstructor aVistaInstructor(Tarea t) {
        return new TareaInstructor(t.getId(), t.getTitulo(), t.getDescripcion(), t.getPuntajeMaximo(), t.getFechaLimite(),
                entregas.countByTareaId(t.getId()), entregas.countByTareaIdAndEstado(t.getId(), EstadoEntrega.CALIFICADA));
    }

    private static TareaEstudiante aVistaEstudiante(Tarea t, EntregaTarea e) {
        boolean vencida = t.getFechaLimite() != null && LocalDateTime.now().isAfter(t.getFechaLimite());
        return new TareaEstudiante(t.getId(), t.getTitulo(), t.getDescripcion(), t.getPuntajeMaximo(), t.getFechaLimite(), vencida,
                e == null ? null : e.getEstado(), e == null ? null : e.getTexto(), e == null ? null : e.getEnlace(),
                e == null ? null : e.getFechaEntrega(), e == null ? null : e.getPuntaje(),
                e == null ? null : e.getRetroalimentacion(), e == null ? null : e.getFechaCalificacion());
    }
}
