package com.elearning.platform.services;

import com.elearning.platform.config.OllamaProperties;
import com.elearning.platform.dto.AccesibilidadDtos.ConfiguracionDto;
import com.elearning.platform.dto.TutorDtos.RespuestaTutorDto;
import com.elearning.platform.entity.*;
import com.elearning.platform.enums.*;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.*;
import com.elearning.platform.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * RF-024 Tutor de IA. El contexto es la lección, el avance y el perfil de accesibilidad (no el estilo VARK).
 * No se mantiene ninguna transacción abierta mientras se espera al modelo; si Ollama falla no se guarda nada.
 */
@Service
@RequiredArgsConstructor
public class TutorService {

    static final String SIN_RESPUESTA = "SIN_RESPUESTA";
    static final String MENSAJE_SIN_RESPUESTA = "No tengo una respuesta confiable para esa pregunta con el material de esta lección. "
            + "Te recomiendo escribirle a tu instructor en la comunidad del curso para que te ayude.";
    private static final int MAX_CUERPO = 3000;

    private final OllamaClient ollama;
    private final OllamaProperties props;
    private final TransactionTemplate tx;
    private final ContenidoRepository contenidos;
    private final InscripcionRepository inscripciones;
    private final ProgresoRepository progresos;
    private final AccesibilidadService accesibilidad;
    private final TutorIARepository tutores;
    private final ConsultaTutorRepository consultas;
    private final RespuestaTutorRepository respuestas;
    private final EstudianteRepository estudiantes;
    private final AuditoriaService auditoria;

    private record Contexto(String sistema, Long contenidoId) {}

    public RespuestaTutorDto consultar(UsuarioPrincipal u, String pregunta, Long contenidoId, String ip) {
        String texto = pregunta == null ? "" : pregunta.trim();
        if (texto.isEmpty()) throw ApiException.solicitudInvalida("PREGUNTA_VACIA", "Escribe tu pregunta.");

        Contexto ctx = tx.execute(s -> construirContexto(u, contenidoId));          // 1. lectura
        String bruto = ollama.chat(ctx.sistema(), texto);                           // 2. sin transacción
        String limpio = limpiar(bruto);
        boolean sinRespuesta = limpio.isEmpty() || limpio.toUpperCase(Locale.ROOT).contains(SIN_RESPUESTA);
        String respuesta = sinRespuesta ? MENSAJE_SIN_RESPUESTA : limpio;
        TipoRespuestaTutor tipo = clasificar(texto);

        RespuestaTutorDto dto = tx.execute(s -> guardar(u.id(), ctx.contenidoId(), texto, respuesta, tipo)); // 3. escritura
        auditoria.registrar(u.id(), "CONSULTA_TUTOR", "tutor/consultas", ResultadoAuditoria.PERMITIDO, ip);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<RespuestaTutorDto> historial(Long estudianteId, int limite) {
        int n = Math.min(Math.max(limite, 1), 100);
        return respuestas.historial(estudianteId, PageRequest.of(0, n)).stream().map(TutorService::aDto).toList();
    }

    @Transactional
    public RespuestaTutorDto marcarUtil(Long estudianteId, Long respuestaId, boolean util) {
        RespuestaTutor r = respuestas.conConsulta(respuestaId)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos esa respuesta."));
        if (!r.getConsulta().getEstudiante().getId().equals(estudianteId)) {
            throw ApiException.noEncontrado("No encontramos esa respuesta.");
        }
        r.setUtil(util);
        return aDto(respuestas.save(r));
    }

    // ------------------------------------------------------------------

    private Contexto construirContexto(UsuarioPrincipal u, Long contenidoId) {
        StringBuilder s = new StringBuilder();
        s.append("Eres el tutor de V-Learning, una plataforma educativa inclusiva. Responde en español, con amabilidad y precisión. ")
                .append("Responde SOLO con texto plano: no uses markdown, asteriscos, almohadillas, tablas ni emojis. ")
                .append("Usa únicamente la información de la lección y tu conocimiento general del tema. ")
                .append("Si no puedes responder con seguridad, responde exactamente ").append(SIN_RESPUESTA).append(". ")
                .append("Mantén la respuesta por debajo de 200 palabras.\n");

        Long idContenido = null;
        if (contenidoId != null) {
            Contenido c = contenidos.findById(contenidoId).orElseThrow(() -> ApiException.noEncontrado("No encontramos ese contenido."));
            Long cursoId = c.getModulo().getCurso().getId();
            boolean inscrito = c.isPublicado() && inscripciones.findByEstudianteIdAndCursoId(u.id(), cursoId)
                    .filter(i -> i.getEstado() != EstadoInscripcion.CANCELADA).isPresent();
            if (!inscrito) throw ApiException.prohibido("NO_INSCRITO", "Inscríbete en el curso para consultar sobre este contenido.");
            idContenido = c.getId();
            s.append("\nLección actual: ").append(c.getTitulo()).append(" (curso «").append(c.getModulo().getCurso().getTitulo()).append("»).\n");
            if (c.getDescripcion() != null) s.append("Descripción: ").append(c.getDescripcion()).append("\n");
            if (c.getCuerpo() != null && !c.getCuerpo().isBlank()) {
                String cuerpo = c.getCuerpo().length() > MAX_CUERPO ? c.getCuerpo().substring(0, MAX_CUERPO) : c.getCuerpo();
                s.append("Texto de la lección:\n").append(cuerpo).append("\n");
            }
            progresos.findByEstudianteIdAndContenidoId(u.id(), c.getId()).ifPresent(p ->
                    s.append("Avance de la persona en esta lección: ").append(p.getEstado()).append(" (").append(p.getPorcentaje()).append("%).\n"));
        }

        Set<CategoriaAccesibilidad> cats = accesibilidad.categoriasDe(u.id());
        ConfiguracionDto cfg = accesibilidad.configuracionDe(u.id());
        if (cats.contains(CategoriaAccesibilidad.COGNITIVA)) {
            s.append("Adapta la respuesta: frases cortas, lenguaje sencillo, pasos numerados con palabras (primero, luego) y una idea por frase.\n");
        }
        if (cats.contains(CategoriaAccesibilidad.VISUAL) || cfg.lectorPantalla()) {
            s.append("La persona usa lector de pantalla: describe todo con palabras, sin símbolos decorativos ni referencias a colores o posiciones visuales.\n");
        }
        if (cats.contains(CategoriaAccesibilidad.AUDITIVA)) {
            s.append("No hagas referencia a audio ni a sonidos; ofrece explicaciones escritas completas.\n");
        }
        return new Contexto(s.toString(), idContenido);
    }

    private RespuestaTutorDto guardar(Long estudianteId, Long contenidoId, String pregunta, String respuesta, TipoRespuestaTutor tipo) {
        TutorIA tutor = tutores.findFirstByActivoTrueOrderByIdAsc().orElseGet(() -> {
            TutorIA t = new TutorIA();
            t.setNombre("Tutor V-Learning");
            t.setModelo(props.model());
            return tutores.save(t);
        });
        ConsultaTutor c = new ConsultaTutor();
        c.setEstudiante(estudiantes.getReferenceById(estudianteId));
        c.setTutor(tutor);
        if (contenidoId != null) c.setContenido(contenidos.getReferenceById(contenidoId));
        c.setPregunta(pregunta);
        c = consultas.save(c);
        RespuestaTutor r = new RespuestaTutor();
        r.setConsulta(c);
        r.setContenido(respuesta);
        r.setTipo(tipo);
        r = respuestas.save(r);
        return new RespuestaTutorDto(r.getId(), c.getId(), pregunta, respuesta, tipo, r.getFecha(), false, contenidoId);
    }

    static TipoRespuestaTutor clasificar(String pregunta) {
        String p = pregunta.toLowerCase(Locale.ROOT);
        if (p.contains("ejemplo")) return TipoRespuestaTutor.EJEMPLO;
        if (p.contains("resum") || p.contains("sintetiz")) return TipoRespuestaTutor.RESUMEN;
        if (p.contains("actividad") || p.contains("ejercicio") || p.contains("practic") || p.contains("práctic")) return TipoRespuestaTutor.ACTIVIDAD;
        return TipoRespuestaTutor.EXPLICACION;
    }

    /** Quita restos de markdown para que los lectores de pantalla no lean símbolos. */
    static String limpiar(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("(?m)^\\s*#{1,6}\\s*", "").replaceAll("(?m)^\\s*[-*•]\\s+", "")
                .replaceAll("[*_`]+", "").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static RespuestaTutorDto aDto(RespuestaTutor r) {
        ConsultaTutor c = r.getConsulta();
        return new RespuestaTutorDto(r.getId(), c.getId(), c.getPregunta(), r.getContenido(), r.getTipo(), r.getFecha(),
                r.isUtil(), c.getContenido() == null ? null : c.getContenido().getId());
    }
}
