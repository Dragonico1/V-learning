package com.elearning.platform.services;

import com.elearning.platform.dto.VarkDtos.*;
import com.elearning.platform.entity.PerfilAprendizaje;
import com.elearning.platform.entity.RespuestaVarkParcial;
import com.elearning.platform.entity.ResultadoVark;
import com.elearning.platform.enums.EstiloVark;
import com.elearning.platform.enums.ResultadoAuditoria;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.EstudianteRepository;
import com.elearning.platform.repository.PerfilAprendizajeRepository;
import com.elearning.platform.repository.RespuestaVarkParcialRepository;
import com.elearning.platform.repository.ResultadoVarkRepository;
import com.elearning.platform.events.VarkCompletadoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RF-004 Identificar estilo de aprendizaje: cuestionario, ayuda por pregunta, guardado parcial,
 * cálculo y selección de método principal y secundario (RF-005, «cambio manual de estilo»).
 */
@Service
@RequiredArgsConstructor
public class VarkService {

    private final PerfilAprendizajeRepository perfiles;
    private final RespuestaVarkParcialRepository parciales;
    private final ResultadoVarkRepository resultados;
    private final EstudianteRepository estudiantes;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;

    public CuestionarioRespuesta cuestionario() {
        return new CuestionarioRespuesta(BancoVark.TOTAL, BancoVark.preguntas());
    }

    @Transactional(readOnly = true)
    public ParcialRespuesta parcial(Long estudianteId) {
        Map<Integer, EstiloVark> mapa = new LinkedHashMap<>();
        perfiles.findByEstudianteId(estudianteId).ifPresent(p ->
                parciales.findByPerfilAprendizajeIdOrderByNumeroPregunta(p.getId())
                        .forEach(r -> mapa.put(r.getNumeroPregunta(), r.getOpcion())));
        return new ParcialRespuesta(BancoVark.TOTAL, mapa.size(), mapa);
    }

    /** Guardado parcial: una fila por pregunta; permite retomar si se abandona. */
    @Transactional
    public ParcialRespuesta guardarRespuesta(Long estudianteId, int numero, EstiloVark opcion) {
        if (numero < 1 || numero > BancoVark.TOTAL) {
            throw ApiException.solicitudInvalida("PREGUNTA_INVALIDA",
                    "La pregunta debe estar entre 1 y " + BancoVark.TOTAL + ".");
        }
        PerfilAprendizaje perfil = perfilDe(estudianteId);
        RespuestaVarkParcial r = parciales.findById(
                        new com.elearning.platform.entity.RespuestaVarkParcialId(perfil.getId(), numero))
                .orElseGet(() -> {
                    RespuestaVarkParcial nueva = new RespuestaVarkParcial();
                    nueva.setPerfilAprendizajeId(perfil.getId());
                    nueva.setNumeroPregunta(numero);
                    return nueva;
                });
        r.setOpcion(opcion);
        parciales.save(r);
        return parcial(estudianteId);
    }

    /** Calcula el resultado con las 12 respuestas, lo guarda y limpia el avance parcial. */
    @Transactional
    public ResultadoVarkRespuesta enviar(Long estudianteId, String ip) {
        PerfilAprendizaje perfil = perfilDe(estudianteId);
        List<RespuestaVarkParcial> respuestas = parciales.findByPerfilAprendizajeIdOrderByNumeroPregunta(perfil.getId());
        if (respuestas.size() < BancoVark.TOTAL) {
            List<Integer> faltan = new ArrayList<>();
            for (int n = 1; n <= BancoVark.TOTAL; n++) {
                final int numero = n;
                if (respuestas.stream().noneMatch(x -> x.getNumeroPregunta() == numero)) faltan.add(n);
            }
            throw ApiException.solicitudInvalida("CUESTIONARIO_INCOMPLETO",
                    "Faltan respuestas en las preguntas: " + faltan + ".");
        }
        Map<EstiloVark, Integer> conteo = new EnumMap<>(EstiloVark.class);
        for (EstiloVark e : EstiloVark.values()) conteo.put(e, 0);
        respuestas.forEach(r -> conteo.merge(r.getOpcion(), 1, Integer::sum));

        boolean primeraVez = resultados.countByPerfilAprendizajeId(perfil.getId()) == 0;
        ResultadoVark res = new ResultadoVark();
        res.setPerfilAprendizaje(perfil);
        res.setVisual(conteo.get(EstiloVark.VISUAL));
        res.setAuditivo(conteo.get(EstiloVark.AUDITIVO));
        res.setLecturaEscritura(conteo.get(EstiloVark.LECTURA_ESCRITURA));
        res.setKinestesico(conteo.get(EstiloVark.KINESTESICO));
        EstiloVark predominante = res.calcularPredominante();
        res.setFechaRealizacion(java.time.LocalDateTime.now());
        res = resultados.save(res);

        perfil.actualizarEstilo(predominante);
        perfil.setVersionTest(BancoVark.VERSION);
        perfiles.save(perfil);
        parciales.borrarDelPerfil(perfil.getId());
        auditoria.registrar(estudianteId, "VARK_COMPLETADO", "perfiles-aprendizaje/" + perfil.getId(),
                ResultadoAuditoria.PERMITIDO, ip);
        if (primeraVez) eventos.publishEvent(new VarkCompletadoEvent(estudianteId));
        return aRespuesta(res, predominante);
    }

    @Transactional(readOnly = true)
    public MetodosRespuesta metodos(Long estudianteId) {
        Optional<PerfilAprendizaje> perfil = perfiles.findByEstudianteId(estudianteId);
        if (perfil.isEmpty()) return new MetodosRespuesta(null, null, null);
        PerfilAprendizaje p = perfil.get();
        ResultadoVarkRespuesta ultimo = resultados
                .findFirstByPerfilAprendizajeIdOrderByFechaRealizacionDescIdDesc(p.getId())
                .map(r -> aRespuesta(r, r.calcularPredominante())).orElse(null);
        return new MetodosRespuesta(p.getEstiloPredominante(), p.getMetodoSecundario(), ultimo);
    }

    /** Cambio manual del método principal y del segundo (reordena los contenidos, RF-005). */
    @Transactional
    public MetodosRespuesta cambiarMetodos(Long estudianteId, EstiloVark principal, EstiloVark secundario, String ip) {
        if (secundario != null && secundario == principal) {
            throw ApiException.solicitudInvalida("METODOS_IGUALES", "El segundo método debe ser distinto del principal.");
        }
        PerfilAprendizaje perfil = perfilDe(estudianteId);
        perfil.setEstiloPredominante(principal);
        perfil.setMetodoSecundario(secundario);
        perfil.setFechaEvaluacion(java.time.LocalDateTime.now());
        perfiles.save(perfil);
        auditoria.registrar(estudianteId, "VARK_METODOS_CAMBIADOS", principal + "/" + secundario,
                ResultadoAuditoria.PERMITIDO, ip);
        return metodos(estudianteId);
    }

    // ------------------------------------------------------------------

    private PerfilAprendizaje perfilDe(Long estudianteId) {
        return perfiles.findByEstudianteId(estudianteId).orElseGet(() -> {
            PerfilAprendizaje nuevo = new PerfilAprendizaje();
            nuevo.setEstudiante(estudiantes.getReferenceById(estudianteId));
            return perfiles.save(nuevo);
        });
    }

    /** Segundo estilo con más puntos (mayor a 0 y distinto del principal); null si no hay. */
    private static EstiloVark segundo(ResultadoVark r, EstiloVark principal) {
        Map<EstiloVark, Integer> m = new EnumMap<>(EstiloVark.class);
        m.put(EstiloVark.VISUAL, r.getVisual());
        m.put(EstiloVark.AUDITIVO, r.getAuditivo());
        m.put(EstiloVark.LECTURA_ESCRITURA, r.getLecturaEscritura());
        m.put(EstiloVark.KINESTESICO, r.getKinestesico());
        return m.entrySet().stream()
                .filter(e -> e.getKey() != principal && e.getValue() > 0)
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey).orElse(null);
    }

    private static ResultadoVarkRespuesta aRespuesta(ResultadoVark r, EstiloVark predominante) {
        return new ResultadoVarkRespuesta(r.getVisual(), r.getAuditivo(), r.getLecturaEscritura(), r.getKinestesico(),
                predominante, segundo(r, predominante), r.getFechaRealizacion());
    }
}
