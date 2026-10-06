package com.elearning.platform.services;

import com.elearning.platform.dto.ContenidoDtos.*;
import com.elearning.platform.dto.CursoDtos.RecursoRespuesta;
import com.elearning.platform.entity.Contenido;
import com.elearning.platform.entity.Curso;
import com.elearning.platform.entity.Progreso;
import com.elearning.platform.entity.RecursoAccesible;
import com.elearning.platform.enums.*;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.util.TextoApoyo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lección (RF-005) con formatos alternativos (RF-019), texto alternativo (RF-018), lengua de señas
 * (RF-020) y apoyo cognitivo (RF-022).
 */
@Service
@RequiredArgsConstructor
public class ContenidoService {

    static final String ALT_GENERICO = "Imagen sin descripción disponible";

    private final ContenidoRepository contenidos;
    private final RecursoAccesibleRepository recursos;
    private final InscripcionRepository inscripciones;
    private final ProgresoRepository progresos;
    private final AccesibilidadService accesibilidad;
    private final NotificacionService notificaciones;

    @Transactional
    public ContenidoVista ver(UsuarioPrincipal u, Long contenidoId) {
        Contenido c = cargarConAcceso(u, contenidoId);
        Curso curso = c.getModulo().getCurso();
        List<RecursoAccesible> recs = recursos.findByContenidoIdOrderByIdAsc(contenidoId);
        Set<TipoRecursoAccesible> tipos = ConformidadAccesibilidad.disponibles(recs);
        boolean esEstudiante = u.rol() == RolUsuario.ESTUDIANTE;
        Set<CategoriaAccesibilidad> categorias = esEstudiante ? accesibilidad.categoriasDe(u.id())
                : java.util.EnumSet.noneOf(CategoriaAccesibilidad.class);

        // RF-018: imágenes sin texto alternativo -> descripción genérica temporal y aviso al instructor
        List<String> advertencias = new ArrayList<>();
        String cuerpo = c.getCuerpo();
        int sinAlt = TextoApoyo.imagenesSinTextoAlternativo(cuerpo);
        if (sinAlt > 0) {
            cuerpo = TextoApoyo.conAltGenerico(cuerpo, ALT_GENERICO);
            advertencias.add("Algunas imágenes no tienen descripción todavía; se muestra una descripción genérica.");
            if (esEstudiante) {
                notificaciones.notificarUnaVezAlDia(curso.getInstructor().getId(),
                        "Falta texto alternativo en «" + c.getTitulo() + "»",
                        "El contenido «" + c.getTitulo() + "» tiene " + sinAlt
                                + " imagen(es) sin texto alternativo. Agrega la descripción para lectores de pantalla.", "/cursos/" + curso.getId() + "/editar");
            }
        }

        // RF-020: lengua de señas
        boolean lse = tipos.contains(TipoRecursoAccesible.LENGUA_SENAS);
        String mensajeLse = (!lse && categorias.contains(CategoriaAccesibilidad.AUDITIVA))
                ? "Este contenido aún no tiene interpretación en lengua de señas. Usa los subtítulos y la transcripción."
                : null;

        Progreso p = esEstudiante ? progresos.findByEstudianteIdAndContenidoId(u.id(), contenidoId).orElse(null) : null;
        return new ContenidoVista(c.getId(), curso.getId(), c.getTitulo(), c.getDescripcion(), c.getFormato(),
                c.getUrlRecurso(), cuerpo, c.getDuracionMinutos(),
                recs.stream().map(r -> new RecursoRespuesta(r.getId(), r.getTipo(), r.getUrl(), r.getDescripcion(), r.isDisponible())).toList(),
                opcionesAlternativas(c, tipos), lse, mensajeLse, categorias.contains(CategoriaAccesibilidad.COGNITIVA),
                advertencias,
                p == null ? EstadoProgreso.NO_INICIADO : p.getEstado(),
                p == null ? BigDecimal.ZERO : p.getPorcentaje(),
                p == null ? 0L : p.getTiempoConsumido());
    }

    /** RF-022: pasos cortos, resumen, glosario y versión simplificada. */
    @Transactional
    public ApoyoCognitivoRespuesta apoyoCognitivo(UsuarioPrincipal u, Long contenidoId) {
        Contenido c = cargarConAcceso(u, contenidoId);
        String cuerpo = c.getCuerpo() == null ? "" : c.getCuerpo();
        List<String> mensajes = new ArrayList<>();
        if (cuerpo.isBlank()) {
            mensajes.add("Este contenido todavía no tiene texto para simplificar.");
        }

        List<TextoApoyo.Unidad> unidades = TextoApoyo.dividir(cuerpo);
        List<String> pasos = TextoApoyo.pasos(cuerpo, unidades);
        TextoApoyo.Resumen resumen = TextoApoyo.resumir(cuerpo);
        if (!cuerpo.isBlank() && !resumen.aceptable()) {
            mensajes.add("No pudimos crear un resumen claro; te mostramos el texto original.");
            notificaciones.notificarUnaVezAlDia(c.getModulo().getCurso().getInstructor().getId(),
                    "Resumen automático no aceptable: «" + c.getTitulo() + "»",
                    "El resumen automático de «" + c.getTitulo() + "» no alcanzó la calidad mínima (texto muy corto o poco reducible). "
                            + "Se conservó el original. Considera cargar una versión simplificada.", "/cursos/" + c.getModulo().getCurso().getId() + "/editar");
        }
        Optional<String> simplificada = recursos.findByContenidoIdOrderByIdAsc(contenidoId).stream()
                .filter(r -> r.isDisponible() && r.getTipo() == TipoRecursoAccesible.VERSION_SIMPLIFICADA)
                .map(RecursoAccesible::getDescripcion).findFirst();
        if (simplificada.isEmpty()) {
            mensajes.add("El instructor aún no cargó una versión en lenguaje simplificado. Usa los pasos y el resumen.");
        }
        return new ApoyoCognitivoRespuesta(
                unidades.stream().map(x -> new UnidadApoyo(x.numero(), x.titulo(), x.texto())).toList(),
                pasos, resumen.texto(), resumen.aceptable(),
                TextoApoyo.glosario(cuerpo).stream().map(t -> new TerminoGlosario(t.termino(), t.definicion())).toList(),
                TextoApoyo.conceptosClave(cuerpo, 6), simplificada.orElse(null), mensajes);
    }

    // ------------------------------------------------------------------

    /** Estudiante: contenido publicado y curso inscrito. Instructor: dueño. Administrador: cualquiera. */
    private Contenido cargarConAcceso(UsuarioPrincipal u, Long contenidoId) {
        Contenido c = contenidos.findById(contenidoId)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos ese contenido."));
        Long cursoId = c.getModulo().getCurso().getId();
        if (u.rol() == RolUsuario.ESTUDIANTE) {
            if (!c.isPublicado() || c.getModulo().getCurso().getEstado() != EstadoCurso.PUBLICADO) {
                throw ApiException.noEncontrado("No encontramos ese contenido.");
            }
            boolean inscrito = inscripciones.findByEstudianteIdAndCursoId(u.id(), cursoId)
                    .filter(i -> i.getEstado() != EstadoInscripcion.CANCELADA).isPresent();
            if (!inscrito) {
                throw ApiException.prohibido("NO_INSCRITO", "Inscríbete en el curso para ver este contenido.");
            }
        } else if (u.rol() == RolUsuario.INSTRUCTOR
                && !c.getModulo().getCurso().getInstructor().getId().equals(u.id())) {
            throw ApiException.prohibido("CURSO_AJENO", "Este curso pertenece a otro instructor.");
        }
        return c;
    }

    /** RF-019: formas disponibles de consumir el mismo contenido. */
    private static List<String> opcionesAlternativas(Contenido c, Set<TipoRecursoAccesible> tipos) {
        List<String> o = new ArrayList<>();
        if (c.getFormato() == FormatoContenido.VIDEO) {
            o.add(tipos.contains(TipoRecursoAccesible.SUBTITULO) ? "VIDEO_SUBTITULADO" : "VIDEO");
        } else if (c.getFormato() == FormatoContenido.PODCAST) {
            o.add("AUDIO");
        } else if (c.getFormato() == FormatoContenido.SIMULACION) {
            o.add("SIMULACION");
        }
        if (tipos.contains(TipoRecursoAccesible.TRANSCRIPCION)) o.add("TRANSCRIPCION");
        if (c.getCuerpo() != null && !c.getCuerpo().isBlank()) o.add("TEXTO_EQUIVALENTE");
        return o;
    }
}
