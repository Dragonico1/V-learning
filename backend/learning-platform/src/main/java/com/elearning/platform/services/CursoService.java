package com.elearning.platform.services;

import com.elearning.platform.dto.CursoDtos.*;
import com.elearning.platform.entity.*;
import com.elearning.platform.enums.*;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.*;
import com.elearning.platform.util.AfinidadVark;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RF-005 Entregar contenido personalizado: catálogo, inscripción y detalle del curso con los
 * materiales ordenados por el método VARK (principal → secundario → resto). Si un módulo no tiene
 * formato afín se muestra el formato por defecto (texto estructurado) y se avisa al instructor.
 */
@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository cursos;
    private final ModuloRepository modulos;
    private final ContenidoRepository contenidos;
    private final InscripcionRepository inscripciones;
    private final ProgresoRepository progresos;
    private final EvaluacionRepository evaluaciones;
    private final PerfilAprendizajeRepository perfiles;
    private final EstudianteRepository estudiantes;
    private final UsuarioRepository usuarios;
    private final AccesibilidadService accesibilidad;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;
    private final AccesoService acceso;

    @Transactional(readOnly = true)
    public List<CursoResumen> catalogo(Long estudianteId) {
        Map<Long, Inscripcion> mias = misInscripciones(estudianteId);
        return cursos.publicados(EstadoCurso.PUBLICADO).stream().map(c -> resumen(c, mias.get(c.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<CursoResumen> misCursos(Long estudianteId) {
        return inscripciones.delEstudiante(estudianteId).stream()
                .filter(i -> i.getEstado() != EstadoInscripcion.CANCELADA)
                .map(i -> resumen(i.getCurso(), i)).toList();
    }

    @Transactional
    public CursoDetalle detalle(Long cursoId, Long estudianteId) {
        Curso curso = cursos.findById(cursoId).filter(c -> c.getEstado() == EstadoCurso.PUBLICADO)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos ese curso."));
        Inscripcion insc = inscripciones.findByEstudianteIdAndCursoId(estudianteId, cursoId)
                .filter(i -> i.getEstado() != EstadoInscripcion.CANCELADA).orElse(null);

        Optional<PerfilAprendizaje> perfil = perfiles.findByEstudianteId(estudianteId);
        EstiloVark principal = perfil.map(PerfilAprendizaje::getEstiloPredominante).orElse(null);
        EstiloVark secundario = perfil.map(PerfilAprendizaje::getMetodoSecundario).orElse(null);
        boolean motora = accesibilidad.categoriasDe(estudianteId).contains(CategoriaAccesibilidad.MOTORA);

        List<Modulo> mods = modulos.findByCursoIdOrderByOrden(cursoId);
        List<Long> modIds = mods.stream().map(Modulo::getId).toList();
        List<Contenido> conts = modIds.isEmpty() ? List.of()
                : contenidos.findByModuloIdInAndPublicadoTrueOrderByIdAsc(modIds);
        Map<Long, Progreso> prog = (insc == null || conts.isEmpty()) ? Map.of()
                : progresos.findByEstudianteIdAndContenidoIdIn(estudianteId, conts.stream().map(Contenido::getId).toList())
                .stream().collect(Collectors.toMap(p -> p.getContenido().getId(), p -> p, (a, b) -> a));
        Map<Long, List<Evaluacion>> evalPorModulo = modIds.isEmpty() ? Map.of()
                : evaluaciones.findByModuloIdInOrderByIdAsc(modIds).stream()
                .collect(Collectors.groupingBy(e -> e.getModulo().getId()));

        List<ModuloItem> items = new ArrayList<>();
        for (Modulo m : mods) {
            List<Contenido> delModulo = conts.stream().filter(c -> c.getModulo().getId().equals(m.getId())).toList();
            List<Contenido> ordenados = new ArrayList<>(delModulo);
            final EstiloVark p1 = principal, p2 = secundario;
            ordenados.sort(Comparator
                    .comparingInt((Contenido c) -> AfinidadVark.rangoContenido(c.getFormato(), p1, p2))
                    .thenComparing(Contenido::getId));

            boolean haySinAfin = principal != null && !delModulo.isEmpty()
                    && delModulo.stream().noneMatch(c -> AfinidadVark.rangoContenido(c.getFormato(), p1, p2) <= 1);
            if (haySinAfin) avisarSinFormatoAfin(curso, m, principal);

            boolean completo = false;
            List<ContenidoItem> cis = new ArrayList<>();
            for (Contenido c : ordenados) {
                Progreso pr = prog.get(c.getId());
                if (pr != null && pr.getEstado() == EstadoProgreso.COMPLETADO) completo = true;
                boolean afin = principal != null && AfinidadVark.rangoContenido(c.getFormato(), p1, p2) <= 1;
                cis.add(new ContenidoItem(c.getId(), c.getTitulo(), c.getFormato(), c.getDuracionMinutos(), true, afin,
                        pr == null ? EstadoProgreso.NO_INICIADO : pr.getEstado(),
                        pr == null ? BigDecimal.ZERO : pr.getPorcentaje(), null, null));
            }
            items.add(new ModuloItem(m.getId(), m.getTitulo(), m.getDescripcion(), m.getOrden(), haySinAfin, completo,
                    cis, evaluacionesDelModulo(evalPorModulo.getOrDefault(m.getId(), List.of()), principal, motora, curso)));
        }
        return new CursoDetalle(curso.getId(), curso.getTitulo(), curso.getDescripcion(),
                curso.getInstructor().getNombre(), curso.getEstado(), insc != null,
                insc == null ? BigDecimal.ZERO : insc.getPorcentajeCompletado(), principal, secundario, items);
    }

    /** Administrador: todos los cursos, en cualquier estado. */
    @Transactional(readOnly = true)
    public List<CursoResumen> todos() {
        return cursos.todosConInstructor().stream().map(c -> resumen(c, null)).toList();
    }

    @Transactional
    public CursoResumen inscribirme(Long cursoId, Long estudianteId, String ip) {
        Curso curso = cursos.findById(cursoId).filter(c -> c.getEstado() == EstadoCurso.PUBLICADO)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos ese curso."));
        Inscripcion i = inscribir(curso, estudianteId);
        auditoria.registrar(estudianteId, "INSCRIPCION", "cursos/" + cursoId, ResultadoAuditoria.PERMITIDO, ip);
        return resumen(curso, i);
    }

    /** Inscripción masiva por correo (instructor dueño del curso o administrador). */
    @Transactional
    public InscripcionResultado inscribirEstudiantes(Long cursoId, List<String> correos, Long actorId,
                                                     boolean esAdministrador, String ip) {
        Curso curso = esAdministrador
                ? cursos.findById(cursoId).orElseThrow(() -> ApiException.noEncontrado("No encontramos ese curso."))
                : acceso.cursoPropio(cursoId, actorId);
        if (curso.getEstado() != EstadoCurso.PUBLICADO) {
            throw ApiException.conflicto("CURSO_NO_PUBLICADO", "Solo se puede inscribir en cursos publicados.");
        }
        List<String> inscritos = new ArrayList<>(), ya = new ArrayList<>(), noEncontrados = new ArrayList<>();
        for (String correo : new LinkedHashSet<>(correos.stream().map(c -> c.trim().toLowerCase(Locale.ROOT)).toList())) {
            Usuario u = usuarios.findByCorreoInstitucionalIgnoreCase(correo).orElse(null);
            if (!(u instanceof Estudiante)) {
                noEncontrados.add(correo);
                continue;
            }
            Optional<Inscripcion> previa = inscripciones.findByEstudianteIdAndCursoId(u.getId(), cursoId);
            if (previa.isPresent() && previa.get().getEstado() != EstadoInscripcion.CANCELADA) {
                ya.add(correo);
                continue;
            }
            inscribir(curso, u.getId());
            notificaciones.notificar(u, "Te inscribieron en un curso",
                    "Ya puedes entrar al curso «" + curso.getTitulo() + "».", false, "/cursos/" + curso.getId());
            inscritos.add(correo);
        }
        auditoria.registrar(actorId, "INSCRIPCION_MASIVA", "cursos/" + cursoId + " (" + inscritos.size() + ")",
                ResultadoAuditoria.PERMITIDO, ip);
        return new InscripcionResultado(inscritos, ya, noEncontrados);
    }

    // ------------------------------------------------------------------

    private Inscripcion inscribir(Curso curso, Long estudianteId) {
        Optional<Inscripcion> previa = inscripciones.findByEstudianteIdAndCursoId(estudianteId, curso.getId());
        if (previa.isPresent()) {
            Inscripcion i = previa.get();
            if (i.getEstado() == EstadoInscripcion.CANCELADA) {
                i.activar();
                inscripciones.save(i);
            }
            return i;
        }
        Inscripcion i = new Inscripcion();
        i.setEstudiante(estudiantes.getReferenceById(estudianteId));
        i.setCurso(curso);
        return inscripciones.save(i);
    }

    private Map<Long, Inscripcion> misInscripciones(Long estudianteId) {
        return inscripciones.delEstudiante(estudianteId).stream()
                .filter(i -> i.getEstado() != EstadoInscripcion.CANCELADA)
                .collect(Collectors.toMap(i -> i.getCurso().getId(), i -> i, (a, b) -> a));
    }

    private static CursoResumen resumen(Curso c, Inscripcion i) {
        return new CursoResumen(c.getId(), c.getTitulo(), c.getDescripcion(), c.getInstructor().getNombre(),
                c.getEstado(), i != null, i == null ? BigDecimal.ZERO : i.getPorcentajeCompletado());
    }

    /** Evaluaciones ordenadas por afinidad con el método (RF-007) y marcadas si no son calificables (RF-021). */
    private List<EvaluacionItem> evaluacionesDelModulo(List<Evaluacion> lista, EstiloVark principal, boolean motora,
                                                       Curso curso) {
        List<Evaluacion> ordenadas = new ArrayList<>(lista);
        ordenadas.sort(Comparator.comparingInt((Evaluacion e) -> AfinidadVark.rangoEvaluacion(e.getTipo(), principal))
                .thenComparing(Evaluacion::getId));
        List<EvaluacionItem> items = new ArrayList<>();
        for (Evaluacion e : ordenadas) {
            boolean calificable = !(motora && !e.isAlternativaAccesible());
            if (!calificable) {
                notificaciones.notificarUnaVezAlDia(curso.getInstructor().getId(),
                        "Actividad sin alternativa accesible: «" + e.getTitulo() + "»",
                        "La evaluación «" + e.getTitulo() + "» del curso «" + curso.getTitulo()
                                + "» no tiene alternativa accesible. Para estudiantes con necesidades motoras no se calificará hasta que la agregues.", "/cursos/" + curso.getId() + "/editar");
            }
            items.add(new EvaluacionItem(e.getId(), e.getTitulo(), e.getTipo(), calificable,
                    calificable ? null : "No calificada por ahora: aún no tiene una alternativa accesible.",
                    e.getFechaLimite(), e.getPuntajeMaximo(), e.getTiempoLimite(), e.getDescripcion()));
        }
        return items;
    }

    private void avisarSinFormatoAfin(Curso curso, Modulo modulo, EstiloVark principal) {
        FormatoContenido formato = AfinidadVark.formatoDe(principal);
        notificaciones.notificarUnaVezAlDia(curso.getInstructor().getId(),
                "Sin formato afín en «" + modulo.getTitulo() + "»",
                "En el curso «" + curso.getTitulo() + "», el módulo «" + modulo.getTitulo()
                        + "» no tiene contenido publicado en formato " + formato
                        + ", que prefiere parte de tu grupo. Se les muestra el texto estructurado por defecto.", "/cursos/" + curso.getId() + "/editar");
    }
}
