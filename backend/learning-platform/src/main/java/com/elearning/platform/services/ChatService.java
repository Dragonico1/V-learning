package com.elearning.platform.services;

import com.elearning.platform.config.VlearningProperties;
import com.elearning.platform.dto.ChatDtos.MensajeDto;
import com.elearning.platform.entity.Curso;
import com.elearning.platform.entity.Mensaje;
import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.*;
import com.elearning.platform.events.MensajeChatEvent;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.CursoRepository;
import com.elearning.platform.repository.InscripcionRepository;
import com.elearning.platform.repository.MensajeRepository;
import com.elearning.platform.repository.UsuarioRepository;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.util.CorreccionEvaluacion;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RF-010 Comunidad del curso: solo miembros (estudiantes inscritos, su instructor, administradores).
 * Valida el contenido, guarda el historial, difunde en tiempo real y permite reportar.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final List<EstadoMensaje> VISIBLES = List.of(EstadoMensaje.ENVIADO, EstadoMensaje.REPORTADO);

    private final MensajeRepository mensajes;
    private final CursoRepository cursos;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;
    private final VlearningProperties props;
    private final ApplicationEventPublisher eventos;

    /** Verifica permisos según el rol. Se usa también en el handshake del WebSocket. */
    @Transactional(readOnly = true)
    public Curso exigirMiembro(Long cursoId, Long usuarioId, RolUsuario rol) {
        Curso curso = cursos.findById(cursoId).orElseThrow(() -> ApiException.noEncontrado("No encontramos ese curso."));
        boolean ok;
        if (rol == RolUsuario.ADMINISTRADOR) {
            ok = true;
        } else if (rol == RolUsuario.INSTRUCTOR) {
            ok = curso.getInstructor().getId().equals(usuarioId);
        } else {
            ok = curso.getEstado() == EstadoCurso.PUBLICADO && inscripciones.findByEstudianteIdAndCursoId(usuarioId, cursoId)
                    .filter(i -> i.getEstado() != EstadoInscripcion.CANCELADA).isPresent();
        }
        if (!ok) throw ApiException.prohibido("NO_ES_MIEMBRO", "Solo los miembros del curso pueden usar la comunidad.");
        return curso;
    }

    @Transactional(readOnly = true)
    public List<MensajeDto> historial(UsuarioPrincipal u, Long cursoId, int limite) {
        exigirMiembro(cursoId, u.id(), u.rol());
        int n = Math.min(Math.max(limite, 1), 200);
        List<Mensaje> lista = new ArrayList<>(mensajes.recientes(cursoId, VISIBLES, PageRequest.of(0, n)));
        Collections.reverse(lista); // del más antiguo al más reciente
        return lista.stream().map(ChatService::aDto).toList();
    }

    @Transactional
    public MensajeDto enviar(UsuarioPrincipal u, Long cursoId, String contenido, String ip) {
        Curso curso = exigirMiembro(cursoId, u.id(), u.rol());
        String texto = contenido == null ? "" : contenido.trim();
        validar(texto);
        Usuario autor = usuarios.getReferenceById(u.id());
        Mensaje m = new Mensaje();
        m.setUsuario(autor);
        m.setCurso(curso);
        m.setContenido(texto);
        m = mensajes.save(m);
        MensajeDto dto = new MensajeDto(m.getId(), cursoId, u.id(), u.nombre(), u.rol(), texto, m.getFechaEnvio(), m.getEstado());
        eventos.publishEvent(new MensajeChatEvent(cursoId, dto));
        return dto;
    }

    /** Marca como REPORTADO y avisa al instructor del curso y a los administradores. */
    @Transactional
    public void reportar(UsuarioPrincipal u, Long mensajeId, String ip) {
        Mensaje m = mensajes.findById(mensajeId).orElseThrow(() -> ApiException.noEncontrado("No encontramos ese mensaje."));
        Curso curso = m.getCurso();
        exigirMiembro(curso.getId(), u.id(), u.rol());
        if (m.getUsuario().getId().equals(u.id())) {
            throw ApiException.solicitudInvalida("REPORTE_PROPIO", "No puedes reportar tu propio mensaje.");
        }
        if (m.getEstado() == EstadoMensaje.ENVIADO) {
            m.setEstado(EstadoMensaje.REPORTADO);
            mensajes.save(m);
        }
        String aviso = "Se reportó un mensaje en «" + curso.getTitulo() + "»: «" + recortar(m.getContenido(), 120) + "».";
        notificaciones.notificar(curso.getInstructor().getId(), "Mensaje reportado", aviso, true);
        notificaciones.notificarAdministradores("Mensaje reportado", aviso);
        auditoria.registrar(u.id(), "MENSAJE_REPORTADO", "mensajes/" + mensajeId, ResultadoAuditoria.PERMITIDO, ip);
    }

    /** Moderación: el instructor del curso o un administrador oculta un mensaje reportado. */
    @Transactional
    public void ocultar(UsuarioPrincipal u, Long mensajeId, String ip) {
        Mensaje m = mensajes.findById(mensajeId).orElseThrow(() -> ApiException.noEncontrado("No encontramos ese mensaje."));
        if (u.rol() == RolUsuario.ESTUDIANTE) throw ApiException.prohibido("ACCESO_DENEGADO", "No tienes permiso para moderar.");
        exigirMiembro(m.getCurso().getId(), u.id(), u.rol());
        m.setEstado(EstadoMensaje.OCULTO);
        mensajes.save(m);
        auditoria.registrar(u.id(), "MENSAJE_OCULTADO", "mensajes/" + mensajeId, ResultadoAuditoria.PERMITIDO, ip);
    }

    // ------------------------------------------------------------------

    private void validar(String texto) {
        if (texto.isEmpty()) throw ApiException.solicitudInvalida("MENSAJE_VACIO", "Escribe un mensaje.");
        if (texto.length() > props.chat().longitudMaxima()) {
            throw ApiException.solicitudInvalida("MENSAJE_LARGO",
                    "El mensaje es muy largo. El máximo es " + props.chat().longitudMaxima() + " caracteres.");
        }
        String normalizado = " " + CorreccionEvaluacion.normalizar(texto) + " ";
        for (String palabra : props.chat().palabrasProhibidas()) {
            String p = CorreccionEvaluacion.normalizar(palabra);
            if (!p.isEmpty() && normalizado.contains(" " + p + " ")) {
                throw ApiException.solicitudInvalida("CONTENIDO_NO_PERMITIDO",
                        "Tu mensaje contiene lenguaje que no está permitido en la comunidad. Reescríbelo con respeto.");
            }
        }
    }

    private static String recortar(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    static MensajeDto aDto(Mensaje m) {
        return new MensajeDto(m.getId(), m.getCurso().getId(), m.getUsuario().getId(), m.getUsuario().getNombre(),
                m.getUsuario().getRol(), m.getContenido(), m.getFechaEnvio(), m.getEstado());
    }
}
