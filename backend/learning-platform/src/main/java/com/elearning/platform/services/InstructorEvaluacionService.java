package com.elearning.platform.services;

import com.elearning.platform.dto.EvaluacionDtos.*;
import com.elearning.platform.entity.Evaluacion;
import com.elearning.platform.entity.Modulo;
import com.elearning.platform.entity.OpcionRespuesta;
import com.elearning.platform.entity.Pregunta;
import com.elearning.platform.enums.ResultadoAuditoria;
import com.elearning.platform.enums.TipoPregunta;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.EvaluacionRepository;
import com.elearning.platform.repository.IntentoEvaluacionRepository;
import com.elearning.platform.repository.OpcionRespuestaRepository;
import com.elearning.platform.repository.PreguntaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Autoría de evaluaciones (instructor). Una evaluación con intentos no se reescribe ni se borra;
 * lo único que sí puede cambiar es la bandera de alternativa accesible (RF-021).
 */
@Service
@RequiredArgsConstructor
public class InstructorEvaluacionService {

    private final AccesoService acceso;
    private final EvaluacionRepository evaluaciones;
    private final PreguntaRepository preguntas;
    private final OpcionRespuestaRepository opciones;
    private final IntentoEvaluacionRepository intentos;
    private final AuditoriaService auditoria;

    @Transactional
    public EvaluacionCreada crear(Long moduloId, Long instructorId, EvaluacionRequest req, String ip) {
        Modulo m = acceso.moduloPropio(moduloId, instructorId);
        Evaluacion e = new Evaluacion();
        e.setModulo(m);
        aplicar(e, req);
        e = evaluaciones.save(e);
        auditoria.registrar(instructorId, "EVALUACION_CREADA", "evaluaciones/" + e.getId(), ResultadoAuditoria.PERMITIDO, ip);
        return aRespuesta(e, 0);
    }

    @Transactional
    public EvaluacionCreada actualizar(Long evaluacionId, Long instructorId, EvaluacionRequest req, String ip) {
        Evaluacion e = propia(evaluacionId, instructorId);
        boolean conIntentos = intentos.countByEvaluacionId(evaluacionId) > 0;
        if (conIntentos) {
            boolean cambiaOtraCosa = !e.getTitulo().equals(req.titulo().trim()) || e.getTipo() != req.tipo()
                    || e.getPuntajeMaximo().compareTo(req.puntajeMaximo()) != 0
                    || !java.util.Objects.equals(e.getTiempoLimite(), req.tiempoLimite())
                    || !java.util.Objects.equals(e.getDescripcion(), req.descripcion());
            if (cambiaOtraCosa) {
                throw ApiException.conflicto("EVALUACION_CON_INTENTOS",
                        "Esta evaluación ya tiene intentos: solo puedes cambiar si tiene alternativa accesible.");
            }
            e.setAlternativaAccesible(!Boolean.FALSE.equals(req.alternativaAccesible()));
        } else {
            aplicar(e, req);
        }
        evaluaciones.save(e);
        auditoria.registrar(instructorId, "EVALUACION_ACTUALIZADA", "evaluaciones/" + evaluacionId, ResultadoAuditoria.PERMITIDO, ip);
        return aRespuesta(e, preguntas.findByEvaluacionIdOrderByOrden(evaluacionId).size());
    }

    @Transactional
    public void eliminar(Long evaluacionId, Long instructorId, String ip) {
        Evaluacion e = propia(evaluacionId, instructorId);
        if (intentos.countByEvaluacionId(evaluacionId) > 0) {
            throw ApiException.conflicto("EVALUACION_CON_INTENTOS", "No se puede borrar una evaluación que ya tiene intentos.");
        }
        evaluaciones.delete(e); // las preguntas y opciones caen por ON DELETE CASCADE
        auditoria.registrar(instructorId, "EVALUACION_ELIMINADA", "evaluaciones/" + evaluacionId, ResultadoAuditoria.PERMITIDO, ip);
    }

    @Transactional
    public PreguntaCreada agregarPregunta(Long evaluacionId, Long instructorId, PreguntaRequest req, String ip) {
        Evaluacion e = propia(evaluacionId, instructorId);
        if (intentos.countByEvaluacionId(evaluacionId) > 0) {
            throw ApiException.conflicto("EVALUACION_CON_INTENTOS", "Esta evaluación ya tiene intentos: no se pueden agregar preguntas.");
        }
        validarOpciones(req);
        int orden = req.orden() != null ? req.orden() : preguntas.maxOrden(evaluacionId) + 1;
        if (preguntas.existsByEvaluacionIdAndOrden(evaluacionId, orden)) {
            throw ApiException.conflicto("ORDEN_DUPLICADO", "Ya hay una pregunta en la posición " + orden + ".");
        }
        Pregunta p = new Pregunta();
        p.setEvaluacion(e);
        p.setEnunciado(req.enunciado().trim());
        p.setTipo(req.tipo());
        p.setPuntaje(req.puntaje());
        p.setOrden(orden);
        p = preguntas.save(p);
        for (OpcionRequest o : req.opciones()) {
            OpcionRespuesta op = new OpcionRespuesta();
            op.setPregunta(p);
            op.setTexto(o.texto().trim());
            op.setCorrecta(o.correcta());
            opciones.save(op);
        }
        auditoria.registrar(instructorId, "PREGUNTA_CREADA", "preguntas/" + p.getId(), ResultadoAuditoria.PERMITIDO, ip);
        return new PreguntaCreada(p.getId(), p.getOrden(), p.getTipo(), p.getPuntaje());
    }

    // ------------------------------------------------------------------

    private Evaluacion propia(Long evaluacionId, Long instructorId) {
        Evaluacion e = evaluaciones.findById(evaluacionId).orElseThrow(() -> ApiException.noEncontrado("No encontramos esa evaluación."));
        acceso.moduloPropio(e.getModulo().getId(), instructorId);
        return e;
    }

    private static void aplicar(Evaluacion e, EvaluacionRequest req) {
        e.setTitulo(req.titulo().trim());
        e.setDescripcion(req.descripcion());
        e.setTipo(req.tipo());
        e.setPuntajeMaximo(req.puntajeMaximo());
        e.setTiempoLimite(req.tiempoLimite());
        e.setAlternativaAccesible(!Boolean.FALSE.equals(req.alternativaAccesible()));
    }

    private static EvaluacionCreada aRespuesta(Evaluacion e, int preguntas) {
        return new EvaluacionCreada(e.getId(), e.getTitulo(), e.getTipo(), e.getPuntajeMaximo(), e.getTiempoLimite(),
                e.isAlternativaAccesible(), preguntas);
    }

    private static void validarOpciones(PreguntaRequest req) {
        List<OpcionRequest> ops = req.opciones();
        long correctas = ops.stream().filter(OpcionRequest::correcta).count();
        TipoPregunta t = req.tipo();
        if (t == TipoPregunta.SELECCION_UNICA && (ops.size() < 2 || correctas != 1)) {
            throw ApiException.solicitudInvalida("OPCIONES_INVALIDAS", "La selección única necesita al menos 2 opciones y exactamente 1 correcta.");
        }
        if (t == TipoPregunta.SELECCION_MULTIPLE && (ops.size() < 2 || correctas < 1)) {
            throw ApiException.solicitudInvalida("OPCIONES_INVALIDAS", "La selección múltiple necesita al menos 2 opciones y 1 o más correctas.");
        }
        if (t == TipoPregunta.VERDADERO_FALSO && (ops.size() != 2 || correctas != 1)) {
            throw ApiException.solicitudInvalida("OPCIONES_INVALIDAS", "Verdadero/falso necesita exactamente 2 opciones y 1 correcta.");
        }
        if (t == TipoPregunta.ABIERTA && (correctas < 1 || correctas != ops.size())) {
            throw ApiException.solicitudInvalida("OPCIONES_INVALIDAS",
                    "La pregunta abierta es de respuesta corta: agrega las respuestas aceptadas, todas marcadas como correctas.");
        }
    }
}
