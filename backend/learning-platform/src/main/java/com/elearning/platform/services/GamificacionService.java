package com.elearning.platform.services;

import com.elearning.platform.dto.GamificacionDtos.*;
import com.elearning.platform.entity.*;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.*;
import com.elearning.platform.util.EventosGamificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RF-009: calcula puntos con las reglas vigentes, sube de nivel (cada 500 pts), desbloquea insignias,
 * notifica y respeta el modo privado del ranking.
 */
@Service
@RequiredArgsConstructor
public class GamificacionService {

    private final PerfilGamificacionRepository perfiles;
    private final InsigniaRepository insignias;
    private final InsigniaObtenidaRepository obtenidas;
    private final ReglaGamificacionRepository reglas;
    private final EstudianteRepository estudiantes;
    private final NotificacionService notificaciones;

    /** Puntos del evento: la regla del administrador si existe; si no, el valor por defecto. */
    @Transactional(readOnly = true)
    public int puntosDe(String evento) {
        Optional<ReglaGamificacion> regla = reglas.findFirstByEventoOrderByIdAsc(evento);
        if (regla.isPresent()) return regla.get().isActivo() ? regla.get().getPuntos() : 0;
        return EventosGamificacion.POR_DEFECTO.getOrDefault(evento, 0);
    }

    @Transactional
    public PerfilGamificacion perfilDe(Long estudianteId) {
        return perfiles.findByEstudianteId(estudianteId).orElseGet(() -> {
            PerfilGamificacion p = new PerfilGamificacion();
            p.setEstudiante(estudiantes.getReferenceById(estudianteId));
            return perfiles.save(p);
        });
    }

    /** Suma puntos, recalcula nivel y evalúa insignias; notifica nivel e insignias nuevas. */
    @Transactional
    public void otorgar(Long estudianteId, int puntos, String motivo) {
        if (puntos <= 0) return;
        PerfilGamificacion p = perfilDe(estudianteId);
        int nivelAntes = p.getNivel();
        p.agregarPuntos(puntos);
        p.setNivel(EventosGamificacion.nivelPara(p.getPuntos()));
        perfiles.save(p);
        notificaciones.notificar(estudianteId, "Ganaste " + puntos + " puntos", motivo, false);

        if (p.getNivel() > nivelAntes) {
            notificaciones.notificar(estudianteId, "¡Subiste al nivel " + p.getNivel() + "!",
                    "Llevas " + p.getPuntos() + " puntos. Sigue así.", false);
        }
        Set<Long> yaTiene = obtenidas.findByPerfilGamificacionId(p.getId()).stream()
                .map(InsigniaObtenida::getInsigniaId).collect(Collectors.toSet());
        for (Insignia ins : insignias.findAllByOrderByPuntosRequeridosAscIdAsc()) {
            if (!yaTiene.contains(ins.getId()) && ins.cumpleRequisito(p.getPuntos())) {
                InsigniaObtenida o = new InsigniaObtenida();
                o.setPerfilGamificacionId(p.getId());
                o.setInsigniaId(ins.getId());
                obtenidas.save(o);
                notificaciones.notificar(estudianteId, "Nueva insignia: " + ins.getNombre(),
                        ins.getDescripcion() == null ? "Desbloqueaste una insignia." : ins.getDescripcion(), false);
            }
        }
    }

    @Transactional
    public LogrosRespuesta logros(Long estudianteId) {
        PerfilGamificacion p = perfilDe(estudianteId);
        Map<Long, Insignia> catalogo = insignias.findAllByOrderByPuntosRequeridosAscIdAsc().stream()
                .collect(Collectors.toMap(Insignia::getId, i -> i, (a, b) -> a, LinkedHashMap::new));
        List<InsigniaObtenida> mias = obtenidas.findByPerfilGamificacionId(p.getId());
        Set<Long> ids = mias.stream().map(InsigniaObtenida::getInsigniaId).collect(Collectors.toSet());

        List<InsigniaObtenidaDto> obt = mias.stream().filter(o -> catalogo.containsKey(o.getInsigniaId()))
                .sorted(Comparator.comparing(InsigniaObtenida::getFechaObtencion))
                .map(o -> {
                    Insignia i = catalogo.get(o.getInsigniaId());
                    return new InsigniaObtenidaDto(i.getId(), i.getNombre(), i.getDescripcion(), o.getFechaObtencion());
                }).toList();
        List<InsigniaPendienteDto> pend = catalogo.values().stream().filter(i -> !ids.contains(i.getId()))
                .map(i -> new InsigniaPendienteDto(i.getId(), i.getNombre(), i.getDescripcion(), i.getPuntosRequeridos(),
                        Math.max(0, i.getPuntosRequeridos() - p.getPuntos()))).toList();

        int siguiente = p.getNivel() * EventosGamificacion.PUNTOS_POR_NIVEL;
        int dentroDelNivel = p.getPuntos() - (p.getNivel() - 1) * EventosGamificacion.PUNTOS_POR_NIVEL;
        int pct = Math.min(100, Math.max(0, dentroDelNivel * 100 / EventosGamificacion.PUNTOS_POR_NIVEL));
        return new LogrosRespuesta(p.getPuntos(), p.getNivel(), siguiente, pct, p.isVisibleRanking(), obt, pend);
    }

    /** Modo privado: el estudiante conserva puntos y nivel pero no figura en la tabla pública. */
    @Transactional
    public LogrosRespuesta cambiarPrivacidad(Long estudianteId, boolean mostrar) {
        PerfilGamificacion p = perfilDe(estudianteId);
        p.setVisibleRanking(mostrar);
        perfiles.save(p);
        return logros(estudianteId);
    }

    static int proporcional(int puntosBase, BigDecimal porcentajeMejora) {
        return BigDecimal.valueOf(puntosBase).multiply(porcentajeMejora)
                .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP).intValue();
    }

    // ------------------ Administración ------------------

    @Transactional(readOnly = true)
    public List<ReglaRespuesta> listarReglas() {
        return reglas.findAllByOrderByEventoAscIdAsc().stream().map(GamificacionService::aRespuesta).toList();
    }

    @Transactional
    public ReglaRespuesta guardarRegla(Long id, ReglaRequest req, Administrador admin) {
        if (!EventosGamificacion.POR_DEFECTO.containsKey(req.evento())) {
            throw ApiException.solicitudInvalida("EVENTO_NO_VALIDO",
                    "El evento debe ser uno de: " + String.join(", ", EventosGamificacion.POR_DEFECTO.keySet()) + ".");
        }
        ReglaGamificacion r = id == null ? new ReglaGamificacion()
                : reglas.findById(id).orElseThrow(() -> ApiException.noEncontrado("No encontramos esa regla."));
        r.setNombre(req.nombre().trim());
        r.setDescripcion(req.descripcion());
        r.setEvento(req.evento());
        r.setPuntos(req.puntos());
        r.setActivo(!Boolean.FALSE.equals(req.activo()));
        r.setAdministrador(admin);
        return aRespuesta(reglas.save(r));
    }

    @Transactional(readOnly = true)
    public List<InsigniaRespuesta> listarInsignias() {
        return insignias.findAllByOrderByPuntosRequeridosAscIdAsc().stream().map(GamificacionService::aRespuesta).toList();
    }

    @Transactional
    public InsigniaRespuesta guardarInsignia(Long id, InsigniaRequest req) {
        Insignia i = id == null ? new Insignia()
                : insignias.findById(id).orElseThrow(() -> ApiException.noEncontrado("No encontramos esa insignia."));
        boolean nombreCambia = id == null || !i.getNombre().equalsIgnoreCase(req.nombre().trim());
        if (nombreCambia && insignias.existsByNombreIgnoreCase(req.nombre().trim())) {
            throw ApiException.conflicto("INSIGNIA_DUPLICADA", "Ya existe una insignia con ese nombre.");
        }
        i.setNombre(req.nombre().trim());
        i.setDescripcion(req.descripcion());
        i.setPuntosRequeridos(req.puntosRequeridos());
        return aRespuesta(insignias.save(i));
    }

    private static ReglaRespuesta aRespuesta(ReglaGamificacion r) {
        return new ReglaRespuesta(r.getId(), r.getNombre(), r.getDescripcion(), r.getEvento(), r.getPuntos(), r.isActivo(),
                r.getFechaActualizacion());
    }

    private static InsigniaRespuesta aRespuesta(Insignia i) {
        return new InsigniaRespuesta(i.getId(), i.getNombre(), i.getDescripcion(), i.getPuntosRequeridos());
    }
}
