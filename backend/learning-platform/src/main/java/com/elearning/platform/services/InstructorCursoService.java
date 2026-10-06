package com.elearning.platform.services;

import com.elearning.platform.dto.CursoDtos.*;
import com.elearning.platform.entity.*;
import com.elearning.platform.enums.*;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.*;
import com.elearning.platform.util.TextoApoyo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestión del curso por el instructor: estructura, recursos accesibles y publicación conforme
 * (RF-005, RF-018, RF-019, RF-020).
 */
@Service
@RequiredArgsConstructor
public class InstructorCursoService {

    private final AccesoService acceso;
    private final CursoRepository cursos;
    private final ModuloRepository modulos;
    private final ContenidoRepository contenidos;
    private final RecursoAccesibleRepository recursos;
    private final EvaluacionRepository evaluaciones;
    private final InstructorRepository instructores;
    private final AuditoriaService auditoria;
    private final NotificacionService notificaciones;

    @Transactional(readOnly = true)
    public List<CursoResumen> listar(Long instructorId) {
        return cursos.delInstructor(instructorId).stream().map(c -> resumen(c)).toList();
    }

    @Transactional
    public CursoResumen crear(Long instructorId, CursoRequest req, String ip) {
        Curso c = new Curso();
        c.setInstructor(instructores.getReferenceById(instructorId));
        c.setTitulo(req.titulo().trim());
        c.setDescripcion(req.descripcion());
        c = cursos.save(c);
        auditoria.registrar(instructorId, "CURSO_CREADO", "cursos/" + c.getId(), ResultadoAuditoria.PERMITIDO, ip);
        return new CursoResumen(c.getId(), c.getTitulo(), c.getDescripcion(), null, c.getEstado(), false, BigDecimal.ZERO);
    }

    @Transactional
    public CursoResumen actualizar(Long cursoId, Long instructorId, CursoRequest req, String ip) {
        Curso c = acceso.cursoPropio(cursoId, instructorId);
        c.setTitulo(req.titulo().trim());
        c.setDescripcion(req.descripcion());
        cursos.save(c);
        auditoria.registrar(instructorId, "CURSO_ACTUALIZADO", "cursos/" + cursoId, ResultadoAuditoria.PERMITIDO, ip);
        return new CursoResumen(c.getId(), c.getTitulo(), c.getDescripcion(), null, c.getEstado(), false, BigDecimal.ZERO);
    }

    /** Vista completa del curso para su instructor: todos los contenidos y su conformidad. */
    @Transactional(readOnly = true)
    public CursoDetalle detalle(Long cursoId, Long instructorId) {
        Curso curso = acceso.cursoPropio(cursoId, instructorId);
        List<Modulo> mods = modulos.findByCursoIdOrderByOrden(cursoId);
        List<Long> ids = mods.stream().map(Modulo::getId).toList();
        List<Contenido> conts = ids.isEmpty() ? List.of() : contenidos.findByModuloIdInOrderByIdAsc(ids);
        Map<Long, List<RecursoAccesible>> recPorContenido = conts.isEmpty() ? Map.of()
                : recursos.findByContenidoIdIn(conts.stream().map(Contenido::getId).toList()).stream()
                .collect(Collectors.groupingBy(r -> r.getContenido().getId()));
        Map<Long, List<Evaluacion>> evalPorModulo = ids.isEmpty() ? Map.of()
                : evaluaciones.findByModuloIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(e -> e.getModulo().getId()));

        List<ModuloItem> items = new ArrayList<>();
        for (Modulo m : mods) {
            List<ContenidoItem> cis = new ArrayList<>();
            for (Contenido c : conts) {
                if (!c.getModulo().getId().equals(m.getId())) continue;
                List<String> faltan = ConformidadAccesibilidad.faltantes(c, recPorContenido.getOrDefault(c.getId(), List.of()));
                cis.add(new ContenidoItem(c.getId(), c.getTitulo(), c.getFormato(), c.getDuracionMinutos(),
                        c.isPublicado(), false, null, null, faltan.isEmpty(), faltan));
            }
            List<EvaluacionItem> evs = evalPorModulo.getOrDefault(m.getId(), List.of()).stream()
                    .map(e -> new EvaluacionItem(e.getId(), e.getTitulo(), e.getTipo(), e.isAlternativaAccesible(),
                            e.isAlternativaAccesible() ? null : "Falta la alternativa accesible para perfil motor."))
                    .toList();
            items.add(new ModuloItem(m.getId(), m.getTitulo(), m.getDescripcion(), m.getOrden(), false, false, cis, evs));
        }
        return new CursoDetalle(curso.getId(), curso.getTitulo(), curso.getDescripcion(),
                curso.getInstructor().getNombre(), curso.getEstado(), false, BigDecimal.ZERO, null, null, items);
    }

    @Transactional
    public ModuloItem crearModulo(Long cursoId, Long instructorId, ModuloRequest req, String ip) {
        Curso curso = acceso.cursoPropio(cursoId, instructorId);
        int orden = req.orden() != null ? req.orden() : modulos.maxOrden(cursoId) + 1;
        if (modulos.existsByCursoIdAndOrden(cursoId, orden)) {
            throw ApiException.conflicto("ORDEN_DUPLICADO", "Ya hay un módulo en la posición " + orden + ".");
        }
        Modulo m = new Modulo();
        m.setCurso(curso);
        m.setTitulo(req.titulo().trim());
        m.setDescripcion(req.descripcion());
        m.setOrden(orden);
        m = modulos.save(m);
        auditoria.registrar(instructorId, "MODULO_CREADO", "modulos/" + m.getId(), ResultadoAuditoria.PERMITIDO, ip);
        return new ModuloItem(m.getId(), m.getTitulo(), m.getDescripcion(), m.getOrden(), false, false, List.of(), List.of());
    }

    @Transactional
    public ModuloItem actualizarModulo(Long moduloId, Long instructorId, ModuloRequest req, String ip) {
        Modulo m = acceso.moduloPropio(moduloId, instructorId);
        if (req.orden() != null && !req.orden().equals(m.getOrden())) {
            if (modulos.existsByCursoIdAndOrden(m.getCurso().getId(), req.orden())) {
                throw ApiException.conflicto("ORDEN_DUPLICADO", "Ya hay un módulo en la posición " + req.orden() + ".");
            }
            m.setOrden(req.orden());
        }
        m.setTitulo(req.titulo().trim());
        m.setDescripcion(req.descripcion());
        modulos.save(m);
        auditoria.registrar(instructorId, "MODULO_ACTUALIZADO", "modulos/" + moduloId, ResultadoAuditoria.PERMITIDO, ip);
        return new ModuloItem(m.getId(), m.getTitulo(), m.getDescripcion(), m.getOrden(), false, false, List.of(), List.of());
    }

    @Transactional
    public ContenidoItem crearContenido(Long moduloId, Long instructorId, ContenidoRequest req, String ip) {
        Modulo m = acceso.moduloPropio(moduloId, instructorId);
        validarUrl(req.urlRecurso());
        Contenido c = new Contenido();
        c.setModulo(m);
        aplicar(c, req);
        c = contenidos.save(c);
        auditoria.registrar(instructorId, "CONTENIDO_CREADO", "contenidos/" + c.getId(), ResultadoAuditoria.PERMITIDO, ip);
        return item(c, List.of());
    }

    @Transactional
    public ContenidoItem actualizarContenido(Long contenidoId, Long instructorId, ContenidoRequest req, String ip) {
        Contenido c = acceso.contenidoPropio(contenidoId, instructorId);
        validarUrl(req.urlRecurso());
        aplicar(c, req);
        contenidos.save(c);
        auditoria.registrar(instructorId, "CONTENIDO_ACTUALIZADO", "contenidos/" + contenidoId, ResultadoAuditoria.PERMITIDO, ip);
        return item(c, recursos.findByContenidoIdOrderByIdAsc(contenidoId));
    }

    @Transactional(readOnly = true)
    public List<RecursoRespuesta> listarRecursos(Long contenidoId, Long instructorId) {
        acceso.contenidoPropio(contenidoId, instructorId);
        return recursos.findByContenidoIdOrderByIdAsc(contenidoId).stream().map(InstructorCursoService::aRespuesta).toList();
    }

    @Transactional
    public RecursoRespuesta agregarRecurso(Long contenidoId, Long instructorId, RecursoRequest req, String ip) {
        Contenido c = acceso.contenidoPropio(contenidoId, instructorId);
        boolean requiereUrl = req.tipo() == TipoRecursoAccesible.SUBTITULO || req.tipo() == TipoRecursoAccesible.AUDIO
                || req.tipo() == TipoRecursoAccesible.LENGUA_SENAS;
        boolean sinUrl = req.url() == null || req.url().isBlank();
        boolean sinTexto = req.descripcion() == null || req.descripcion().isBlank();
        if (requiereUrl && sinUrl) {
            throw ApiException.solicitudInvalida("URL_REQUERIDA", "Este tipo de recurso necesita un enlace al archivo.");
        }
        if (!requiereUrl && sinTexto) {
            throw ApiException.solicitudInvalida("TEXTO_REQUERIDO", "Escribe el texto en la descripción del recurso.");
        }
        if (!sinUrl) validarUrl(req.url());
        RecursoAccesible r = new RecursoAccesible();
        r.setContenido(c);
        r.setTipo(req.tipo());
        r.setUrl(sinUrl ? "interno:texto" : req.url().trim()); // la columna url es obligatoria
        r.setDescripcion(req.descripcion());
        r.setDisponible(true);
        r = recursos.save(r);
        auditoria.registrar(instructorId, "RECURSO_ACCESIBLE_AGREGADO", "contenidos/" + contenidoId + " " + req.tipo(),
                ResultadoAuditoria.PERMITIDO, ip);
        return aRespuesta(r);
    }

    @Transactional
    public void eliminarRecurso(Long recursoId, Long instructorId, String ip) {
        RecursoAccesible r = recursos.findById(recursoId)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos ese recurso."));
        acceso.contenidoPropio(r.getContenido().getId(), instructorId);
        recursos.delete(r);
        auditoria.registrar(instructorId, "RECURSO_ACCESIBLE_ELIMINADO", "recursos/" + recursoId, ResultadoAuditoria.PERMITIDO, ip);
    }

    @Transactional(readOnly = true)
    public ConformidadRespuesta conformidad(Long contenidoId, Long instructorId) {
        Contenido c = acceso.contenidoPropio(contenidoId, instructorId);
        List<String> faltan = ConformidadAccesibilidad.faltantes(c, recursos.findByContenidoIdOrderByIdAsc(contenidoId));
        return new ConformidadRespuesta(faltan.isEmpty(), faltan, ConformidadAccesibilidad.advertencias(c));
    }

    /** RF-019: un contenido no conforme se rechaza y el intento queda auditado. */
    @Transactional(noRollbackFor = ApiException.class)
    public PublicacionRespuesta publicarContenido(Long contenidoId, Long instructorId, String ip) {
        Contenido c = acceso.contenidoPropio(contenidoId, instructorId);
        List<String> faltan = ConformidadAccesibilidad.faltantes(c, recursos.findByContenidoIdOrderByIdAsc(contenidoId));
        if (!faltan.isEmpty()) {
            auditoria.registrar(instructorId, "PUBLICACION_RECHAZADA_NO_CONFORME", "contenidos/" + contenidoId,
                    ResultadoAuditoria.DENEGADO, ip);
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTENIDO_NO_CONFORME",
                    "No se puede publicar: " + String.join(" ", faltan));
        }
        boolean eraVisible = c.isPublicado();
        c.publicar();
        contenidos.save(c);
        auditoria.registrar(instructorId, "CONTENIDO_PUBLICADO", "contenidos/" + contenidoId, ResultadoAuditoria.PERMITIDO, ip);
        Curso curso = c.getModulo().getCurso();
        if (!eraVisible && curso.getEstado() == EstadoCurso.PUBLICADO) {
            notificaciones.notificarInscritos(curso.getId(), "Nuevo contenido: " + c.getTitulo(),
                    "Hay un contenido nuevo en «" + curso.getTitulo() + "» (módulo " + c.getModulo().getTitulo() + "): «" + c.getTitulo() + "».");
        }
        return new PublicacionRespuesta(c.getId(), true, ConformidadAccesibilidad.advertencias(c));
    }

    @Transactional
    public PublicacionRespuesta despublicarContenido(Long contenidoId, Long instructorId, String ip) {
        Contenido c = acceso.contenidoPropio(contenidoId, instructorId);
        c.despublicar();
        contenidos.save(c);
        auditoria.registrar(instructorId, "CONTENIDO_DESPUBLICADO", "contenidos/" + contenidoId, ResultadoAuditoria.PERMITIDO, ip);
        return new PublicacionRespuesta(c.getId(), false, List.of());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public CursoResumen publicarCurso(Long cursoId, Long instructorId, String ip) {
        Curso c = acceso.cursoPropio(cursoId, instructorId);
        if (contenidos.contarPublicadosDelCurso(cursoId) == 0) {
            throw ApiException.conflicto("CURSO_SIN_CONTENIDO",
                    "Para publicar el curso publica al menos un contenido conforme.");
        }
        c.publicar();
        cursos.save(c);
        auditoria.registrar(instructorId, "CURSO_PUBLICADO", "cursos/" + cursoId, ResultadoAuditoria.PERMITIDO, ip);
        return new CursoResumen(c.getId(), c.getTitulo(), c.getDescripcion(), null, c.getEstado(), false, BigDecimal.ZERO);
    }

    @Transactional
    public CursoResumen archivarCurso(Long cursoId, Long instructorId, String ip) {
        Curso c = acceso.cursoPropio(cursoId, instructorId);
        c.archivar();
        cursos.save(c);
        auditoria.registrar(instructorId, "CURSO_ARCHIVADO", "cursos/" + cursoId, ResultadoAuditoria.PERMITIDO, ip);
        return new CursoResumen(c.getId(), c.getTitulo(), c.getDescripcion(), null, c.getEstado(), false, BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------

    private CursoResumen resumen(Curso c) {
        return new CursoResumen(c.getId(), c.getTitulo(), c.getDescripcion(), c.getInstructor().getNombre(),
                c.getEstado(), false, BigDecimal.ZERO);
    }

    private void aplicar(Contenido c, ContenidoRequest req) {
        c.setTitulo(req.titulo().trim());
        c.setDescripcion(req.descripcion());
        c.setDuracionMinutos(req.duracionMinutos());
        c.setFormato(req.formato());
        c.setUrlRecurso(req.urlRecurso() == null || req.urlRecurso().isBlank() ? null : req.urlRecurso().trim());
        c.setCuerpo(req.cuerpo());
    }

    private ContenidoItem item(Contenido c, List<RecursoAccesible> recs) {
        List<String> faltan = ConformidadAccesibilidad.faltantes(c, recs);
        return new ContenidoItem(c.getId(), c.getTitulo(), c.getFormato(), c.getDuracionMinutos(), c.isPublicado(),
                false, null, null, faltan.isEmpty(), faltan);
    }

    private static RecursoRespuesta aRespuesta(RecursoAccesible r) {
        return new RecursoRespuesta(r.getId(), r.getTipo(), r.getUrl(), r.getDescripcion(), r.isDisponible());
    }

    /** Solo http(s) o rutas internas: evita javascript: y similares. */
    private static void validarUrl(String url) {
        if (url == null || url.isBlank()) return;
        String u = url.trim().toLowerCase();
        if (!(u.startsWith("https://") || u.startsWith("http://") || u.startsWith("/"))) {
            throw ApiException.solicitudInvalida("URL_NO_PERMITIDA", "El enlace debe empezar con https://, http:// o /.");
        }
    }
}
