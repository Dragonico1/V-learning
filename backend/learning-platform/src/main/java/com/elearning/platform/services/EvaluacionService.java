package com.elearning.platform.services;

import com.elearning.platform.dto.EvaluacionDtos.*;
import com.elearning.platform.entity.*;
import com.elearning.platform.enums.*;
import com.elearning.platform.events.EvaluacionFinalizadaEvent;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.*;
import com.elearning.platform.util.AfinidadVark;
import com.elearning.platform.util.CorreccionEvaluacion;
import com.elearning.platform.util.CorreccionEvaluacion.OpcionDato;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RF-007 Realizar evaluación: orden por afinidad con el método, intento con guardado automático,
 * reanudación, tiempo adicional según el perfil, corrección, retroalimentación y repetición
 * conservando el histórico.
 */
@Service
@RequiredArgsConstructor
public class EvaluacionService {

    private static final long GRACIA_SEGUNDOS = 30;

    private final EvaluacionRepository evaluaciones;
    private final ModuloRepository modulos;
    private final PreguntaRepository preguntas;
    private final OpcionRespuestaRepository opciones;
    private final IntentoEvaluacionRepository intentos;
    private final RespuestaRepository respuestas;
    private final InscripcionRepository inscripciones;
    private final ProgresoRepository progresos;
    private final ContenidoRepository contenidos;
    private final EstudianteRepository estudiantes;
    private final PerfilAprendizajeRepository perfiles;
    private final AccesibilidadService accesibilidad;
    private final RetroalimentacionService retroalimentacion;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;

    @Transactional
    public List<EvaluacionEstudiante> listarPorModulo(Long estudianteId, Long moduloId) {
        Modulo modulo = modulos.findById(moduloId).orElseThrow(() -> ApiException.noEncontrado("No encontramos ese módulo."));
        exigirInscrito(estudianteId, modulo);
        EstiloVark principal = perfiles.findByEstudianteId(estudianteId).map(PerfilAprendizaje::getEstiloPredominante).orElse(null);
        boolean motora = accesibilidad.categoriasDe(estudianteId).contains(CategoriaAccesibilidad.MOTORA);
        int extra = accesibilidad.configuracionDe(estudianteId).tiempoAdicional();
        String bloqueo = motivoModuloIncompleto(estudianteId, modulo);

        List<Evaluacion> lista = new ArrayList<>(evaluaciones.findByModuloIdOrderByIdAsc(moduloId));
        lista.sort(Comparator.comparingInt((Evaluacion e) -> AfinidadVark.rangoEvaluacion(e.getTipo(), principal))
                .thenComparing(Evaluacion::getId));
        List<EvaluacionEstudiante> resultado = new ArrayList<>();
        for (Evaluacion e : lista) {
            boolean calificable = !(motora && !e.isAlternativaAccesible());
            if (!calificable) avisarSinAlternativa(modulo, e);
            List<IntentoEvaluacion> hist = intentos.findByEvaluacionIdAndEstudianteIdOrderByFechaInicioDesc(e.getId(), estudianteId);
            BigDecimal mejor = hist.stream().filter(i -> i.getEstado() == EstadoIntento.FINALIZADO && i.getPuntaje() != null)
                    .map(i -> CorreccionEvaluacion.porcentaje(i.getPuntaje(), e.getPuntajeMaximo()))
                    .max(Comparator.naturalOrder()).orElse(null);
            Long enProgreso = hist.stream().filter(i -> i.getEstado() == EstadoIntento.EN_PROGRESO)
                    .map(IntentoEvaluacion::getId).findFirst().orElse(null);
            resultado.add(new EvaluacionEstudiante(e.getId(), e.getTitulo(), e.getDescripcion(), e.getTipo(),
                    e.getPuntajeMaximo(), minutosEfectivos(e, extra), calificable,
                    calificable ? null : "No calificada por ahora: aún no tiene una alternativa accesible.",
                    bloqueo == null && calificable, bloqueo, hist.size(), mejor, enProgreso));
        }
        return resultado;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public IntentoVista iniciarIntento(Long estudianteId, Long evaluacionId, String ip) {
        Evaluacion e = evaluaciones.findById(evaluacionId).orElseThrow(() -> ApiException.noEncontrado("No encontramos esa evaluación."));
        Modulo modulo = e.getModulo();
        exigirInscrito(estudianteId, modulo);
        boolean motora = accesibilidad.categoriasDe(estudianteId).contains(CategoriaAccesibilidad.MOTORA);
        if (motora && !e.isAlternativaAccesible()) {
            avisarSinAlternativa(modulo, e);
            throw ApiException.conflicto("EVALUACION_NO_CALIFICABLE",
                    "Esta actividad no se calificará por ahora porque aún no tiene una alternativa accesible. Avisamos al instructor.");
        }
        String bloqueo = motivoModuloIncompleto(estudianteId, modulo);
        if (bloqueo != null) throw ApiException.conflicto("MODULO_INCOMPLETO", bloqueo);
        List<Pregunta> pregs = preguntas.findByEvaluacionIdOrderByOrden(evaluacionId);
        if (pregs.isEmpty()) throw ApiException.conflicto("EVALUACION_SIN_PREGUNTAS", "Esta evaluación aún no tiene preguntas.");

        int extra = accesibilidad.configuracionDe(estudianteId).tiempoAdicional();
        Optional<IntentoEvaluacion> abierto = intentos.findFirstByEvaluacionIdAndEstudianteIdAndEstadoOrderByFechaInicioDesc(
                evaluacionId, estudianteId, EstadoIntento.EN_PROGRESO);
        if (abierto.isPresent()) {
            IntentoEvaluacion previo = abierto.get();
            if (!vencido(previo, e, extra)) {
                return vista(previo, e, pregs, extra, true);
            }
            finalizarInterno(previo, e, estudianteId); // se agotó el tiempo: se califica lo guardado
        }
        IntentoEvaluacion nuevo = new IntentoEvaluacion();
        nuevo.setEvaluacion(e);
        nuevo.setEstudiante(estudiantes.getReferenceById(estudianteId));
        nuevo = intentos.save(nuevo);
        auditoria.registrar(estudianteId, "INTENTO_INICIADO", "evaluaciones/" + evaluacionId, ResultadoAuditoria.PERMITIDO, ip);
        return vista(nuevo, e, pregs, extra, false);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public GuardadoRespuesta guardarRespuesta(Long estudianteId, Long intentoId, Long preguntaId, String valor) {
        IntentoEvaluacion i = intentoPropio(estudianteId, intentoId);
        Evaluacion e = i.getEvaluacion();
        int extra = accesibilidad.configuracionDe(estudianteId).tiempoAdicional();
        if (i.getEstado() != EstadoIntento.EN_PROGRESO) {
            throw ApiException.conflicto("INTENTO_CERRADO", "Este intento ya terminó.");
        }
        if (vencido(i, e, extra)) {
            finalizarInterno(i, e, estudianteId);
            throw ApiException.conflicto("TIEMPO_AGOTADO", "Se acabó el tiempo. Calificamos lo que alcanzaste a responder.");
        }
        Pregunta p = preguntas.findById(preguntaId).filter(x -> x.getEvaluacion().getId().equals(e.getId()))
                .orElseThrow(() -> ApiException.noEncontrado("Esa pregunta no pertenece a esta evaluación."));
        String limpio = valor == null ? null : valor.trim();
        validarValor(p, limpio);
        Respuesta r = respuestas.findByIntentoIdAndPreguntaId(intentoId, preguntaId).orElseGet(() -> {
            Respuesta nueva = new Respuesta();
            nueva.setIntento(i);
            nueva.setPregunta(p);
            return nueva;
        });
        r.setValor(limpio == null || limpio.isEmpty() ? null : limpio);
        respuestas.save(r);
        return new GuardadoRespuesta(preguntaId, true, segundosRestantes(i, e, extra));
    }

    @Transactional
    public ResultadoIntento finalizar(Long estudianteId, Long intentoId, String ip) {
        IntentoEvaluacion i = intentoPropio(estudianteId, intentoId);
        if (i.getEstado() == EstadoIntento.EN_PROGRESO) {
            finalizarInterno(i, i.getEvaluacion(), estudianteId);
            auditoria.registrar(estudianteId, "INTENTO_FINALIZADO", "intentos/" + intentoId, ResultadoAuditoria.PERMITIDO, ip);
        }
        return resultado(estudianteId, intentoId);
    }

    @Transactional(readOnly = true)
    public List<IntentoHistorial> historial(Long estudianteId, Long evaluacionId) {
        Evaluacion e = evaluaciones.findById(evaluacionId).orElseThrow(() -> ApiException.noEncontrado("No encontramos esa evaluación."));
        exigirInscrito(estudianteId, e.getModulo());
        return intentos.findByEvaluacionIdAndEstudianteIdOrderByFechaInicioDesc(evaluacionId, estudianteId).stream().map(i -> {
            BigDecimal pct = i.getPuntaje() == null ? null : CorreccionEvaluacion.porcentaje(i.getPuntaje(), e.getPuntajeMaximo());
            return new IntentoHistorial(i.getId(), i.getEstado(), i.getFechaInicio(), i.getFechaFinalizacion(), i.getPuntaje(),
                    pct, pct == null ? null : CorreccionEvaluacion.clasificar(pct));
        }).toList();
    }

    @Transactional
    public ResultadoIntento resultado(Long estudianteId, Long intentoId) {
        IntentoEvaluacion i = intentoPropio(estudianteId, intentoId);
        if (i.getEstado() == EstadoIntento.EN_PROGRESO) {
            throw ApiException.conflicto("INTENTO_EN_PROGRESO", "Termina el intento para ver el resultado.");
        }
        Evaluacion e = i.getEvaluacion();
        BigDecimal puntaje = i.getPuntaje() == null ? BigDecimal.ZERO : i.getPuntaje();
        BigDecimal pct = CorreccionEvaluacion.porcentaje(puntaje, e.getPuntajeMaximo());
        List<Pregunta> pregs = preguntas.findByEvaluacionIdOrderByOrden(e.getId());
        Map<Long, List<OpcionRespuesta>> ops = opcionesPorPregunta(pregs);
        Map<Long, Respuesta> resp = respuestas.findByIntentoId(i.getId()).stream()
                .collect(Collectors.toMap(r -> r.getPregunta().getId(), r -> r, (a, b) -> a));

        List<DetallePregunta> detalle = new ArrayList<>();
        for (Pregunta p : pregs) {
            Respuesta r = resp.get(p.getId());
            List<OpcionRespuesta> lista = ops.getOrDefault(p.getId(), List.of());
            detalle.add(new DetallePregunta(p.getId(), p.getEnunciado(), p.getTipo(), p.getPuntaje(),
                    r == null || r.getPuntajeObtenido() == null ? BigDecimal.ZERO : r.getPuntajeObtenido(),
                    r != null && Boolean.TRUE.equals(r.getEsCorrecta()),
                    textoRespuesta(p, r == null ? null : r.getValor(), lista),
                    lista.stream().filter(OpcionRespuesta::isCorrecta).map(OpcionRespuesta::getTexto).toList()));
        }
        BigDecimal previo = intentos.mejorPuntajePrevio(e.getId(), estudianteId, i.getId());
        BigDecimal pctPrevio = previo == null ? null : CorreccionEvaluacion.porcentaje(previo, e.getPuntajeMaximo());
        RetroalimentacionService.Retro retro = retroalimentacion.generar(estudianteId, e, pct);
        return new ResultadoIntento(i.getId(), e.getId(), e.getTitulo(), puntaje, e.getPuntajeMaximo(), pct,
                retro.clasificacion(), retro.mensaje(), retro.recomendaciones(), detalle, pctPrevio,
                pctPrevio != null && pct.compareTo(pctPrevio) > 0);
    }

    // ------------------------------------------------------------------

    private void finalizarInterno(IntentoEvaluacion i, Evaluacion e, Long estudianteId) {
        List<Pregunta> pregs = preguntas.findByEvaluacionIdOrderByOrden(e.getId());
        Map<Long, List<OpcionRespuesta>> ops = opcionesPorPregunta(pregs);
        Map<Long, Respuesta> resp = respuestas.findByIntentoId(i.getId()).stream()
                .collect(Collectors.toMap(r -> r.getPregunta().getId(), r -> r, (a, b) -> a));
        BigDecimal obtenidoTotal = BigDecimal.ZERO, posible = BigDecimal.ZERO;
        for (Pregunta p : pregs) {
            Respuesta r = resp.get(p.getId());
            if (r == null) {
                r = new Respuesta();
                r.setIntento(i);
                r.setPregunta(p);
            }
            List<OpcionDato> datos = ops.getOrDefault(p.getId(), List.of()).stream()
                    .map(o -> new OpcionDato(o.getId(), o.getTexto(), o.isCorrecta())).toList();
            CorreccionEvaluacion.Resultado res = CorreccionEvaluacion.corregir(p.getTipo(), p.getPuntaje(), datos, r.getValor());
            r.setEsCorrecta(res.completa());
            r.setPuntajeObtenido(res.obtenido());
            respuestas.save(r);
            obtenidoTotal = obtenidoTotal.add(res.obtenido());
            posible = posible.add(p.getPuntaje());
        }
        BigDecimal puntaje = posible.signum() == 0 ? BigDecimal.ZERO
                : obtenidoTotal.multiply(e.getPuntajeMaximo()).divide(posible, 2, RoundingMode.HALF_UP);
        i.setPuntaje(puntaje);
        i.setEstado(EstadoIntento.FINALIZADO);
        i.setFechaFinalizacion(LocalDateTime.now());
        intentos.save(i);

        BigDecimal previo = intentos.mejorPuntajePrevio(e.getId(), estudianteId, i.getId());
        BigDecimal pct = CorreccionEvaluacion.porcentaje(puntaje, e.getPuntajeMaximo());
        BigDecimal pctPrevio = previo == null ? BigDecimal.ZERO : CorreccionEvaluacion.porcentaje(previo, e.getPuntajeMaximo());
        eventos.publishEvent(new EvaluacionFinalizadaEvent(estudianteId, e.getId(), e.getModulo().getCurso().getId(), pct, pctPrevio));
    }

    private IntentoVista vista(IntentoEvaluacion i, Evaluacion e, List<Pregunta> pregs, int extra, boolean reanudado) {
        Map<Long, List<OpcionRespuesta>> ops = opcionesPorPregunta(pregs);
        List<PreguntaVista> pv = pregs.stream().map(p -> new PreguntaVista(p.getId(), p.getEnunciado(), p.getTipo(),
                p.getPuntaje(), p.getOrden(),
                p.getTipo() == TipoPregunta.ABIERTA ? List.of() // no se exponen las respuestas aceptadas
                        : ops.getOrDefault(p.getId(), List.of()).stream().map(o -> new OpcionVista(o.getId(), o.getTexto())).toList())).toList();
        Map<Long, String> guardadas = new LinkedHashMap<>();
        respuestas.findByIntentoId(i.getId()).forEach(r -> {
            if (r.getValor() != null) guardadas.put(r.getPregunta().getId(), r.getValor());
        });
        return new IntentoVista(i.getId(), e.getId(), e.getTitulo(), e.getTipo(), i.getEstado(), i.getFechaInicio(),
                minutosEfectivos(e, extra), segundosRestantes(i, e, extra), pv, guardadas, reanudado);
    }

    private Map<Long, List<OpcionRespuesta>> opcionesPorPregunta(List<Pregunta> pregs) {
        if (pregs.isEmpty()) return Map.of();
        return opciones.findByPreguntaIdInOrderByIdAsc(pregs.stream().map(Pregunta::getId).toList()).stream()
                .collect(Collectors.groupingBy(o -> o.getPregunta().getId()));
    }

    private void validarValor(Pregunta p, String valor) {
        if (valor == null || valor.isEmpty() || p.getTipo() == TipoPregunta.ABIERTA) return;
        Set<Long> validas = opciones.findByPreguntaIdOrderByIdAsc(p.getId()).stream().map(OpcionRespuesta::getId).collect(Collectors.toSet());
        String[] partes = p.getTipo() == TipoPregunta.SELECCION_MULTIPLE ? valor.split(",") : new String[]{valor};
        for (String parte : partes) {
            try {
                if (!validas.contains(Long.valueOf(parte.trim()))) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                throw ApiException.solicitudInvalida("OPCION_INVALIDA", "Elige una de las opciones de la pregunta.");
            }
        }
    }

    private String textoRespuesta(Pregunta p, String valor, List<OpcionRespuesta> lista) {
        if (valor == null || p.getTipo() == TipoPregunta.ABIERTA) return valor;
        Set<String> ids = Arrays.stream(valor.split(",")).map(String::trim).collect(Collectors.toSet());
        return lista.stream().filter(o -> ids.contains(String.valueOf(o.getId()))).map(OpcionRespuesta::getTexto)
                .collect(Collectors.joining("; "));
    }

    private IntentoEvaluacion intentoPropio(Long estudianteId, Long intentoId) {
        IntentoEvaluacion i = intentos.findById(intentoId).orElseThrow(() -> ApiException.noEncontrado("No encontramos ese intento."));
        if (!i.getEstudiante().getId().equals(estudianteId)) {
            throw ApiException.prohibido("INTENTO_AJENO", "Ese intento pertenece a otra persona.");
        }
        return i;
    }

    private void exigirInscrito(Long estudianteId, Modulo modulo) {
        Curso curso = modulo.getCurso();
        boolean ok = curso.getEstado() == EstadoCurso.PUBLICADO && inscripciones
                .findByEstudianteIdAndCursoId(estudianteId, curso.getId())
                .filter(x -> x.getEstado() != EstadoInscripcion.CANCELADA).isPresent();
        if (!ok) throw ApiException.prohibido("NO_INSCRITO", "Inscríbete en el curso para acceder a sus evaluaciones.");
    }

    /** null si la evaluación está habilitada: se completó al menos un contenido o el módulo no tiene contenidos. */
    private String motivoModuloIncompleto(Long estudianteId, Modulo modulo) {
        boolean hayContenido = !contenidos.findByModuloIdInAndPublicadoTrueOrderByIdAsc(List.of(modulo.getId())).isEmpty();
        if (hayContenido && progresos.contarCompletadosEnModulo(estudianteId, modulo.getId()) == 0) {
            return "Completa al menos un contenido del módulo antes de presentar la evaluación.";
        }
        return null;
    }

    private void avisarSinAlternativa(Modulo modulo, Evaluacion e) {
        notificaciones.notificarUnaVezAlDia(modulo.getCurso().getInstructor().getId(),
                "Actividad sin alternativa accesible: «" + e.getTitulo() + "»",
                "La evaluación «" + e.getTitulo() + "» no tiene alternativa accesible. Para estudiantes con necesidades motoras no se calificará hasta que la agregues.");
    }

    static Integer minutosEfectivos(Evaluacion e, int extraPorcentaje) {
        if (e.getTiempoLimite() == null) return null;
        return (int) Math.ceil(e.getTiempoLimite() * (100 + extraPorcentaje) / 100.0);
    }

    private Long segundosRestantes(IntentoEvaluacion i, Evaluacion e, int extra) {
        Integer min = minutosEfectivos(e, extra);
        if (min == null) return null;
        long transcurrido = Duration.between(i.getFechaInicio(), LocalDateTime.now()).getSeconds();
        return Math.max(0, min * 60L - transcurrido);
    }

    private boolean vencido(IntentoEvaluacion i, Evaluacion e, int extra) {
        Integer min = minutosEfectivos(e, extra);
        if (min == null) return false;
        return Duration.between(i.getFechaInicio(), LocalDateTime.now()).getSeconds() > min * 60L + GRACIA_SEGUNDOS;
    }
}
