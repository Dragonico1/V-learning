package com.elearning.platform.services;

import com.elearning.platform.config.VlearningProperties;
import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.ResultadoAuditoria;
import com.elearning.platform.repository.RegistroAuditoriaRepository;
import com.elearning.platform.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Reacciona a actividad sospechosa (RF-013): entradas con inyección/XSS y
 * accesos denegados repetidos. Bloquea temporalmente al usuario y alerta a
 * los administradores.
 */
@Service
@RequiredArgsConstructor
public class SeguridadService {

    static final String ACCION_ENTRADA_MALICIOSA = "ENTRADA_MALICIOSA";
    static final int MAX_ENTRADAS_MALICIOSAS = 3;
    static final int MAX_DENEGACIONES = 10;
    static final int VENTANA_MINUTOS = 10;

    private final AuditoriaService auditoria;
    private final RegistroAuditoriaRepository registros;
    private final UsuarioRepository usuarios;
    private final SesionService sesiones;
    private final NotificacionService notificaciones;
    private final VlearningProperties props;

    /** tipo: "XSS" o "SQL". usuarioId puede ser null (petición anónima). */
    public void reportarEntradaMaliciosa(Long usuarioId, String ip, String ruta, String tipo) {
        auditoria.registrar(usuarioId, ACCION_ENTRADA_MALICIOSA + "_" + tipo, ruta, ResultadoAuditoria.DENEGADO, ip);
        if (usuarioId == null) return;

        notificaciones.notificarAdministradores("Entrada sospechosa detectada",
                "Se rechazó una solicitud con posible " + tipo + " del usuario #" + usuarioId + " en " + ruta + ".");

        long intentos = registros.countByUsuarioIdAndAccionStartingWithAndFechaAfter(
                usuarioId, ACCION_ENTRADA_MALICIOSA, LocalDateTime.now().minusMinutes(VENTANA_MINUTOS));
        if (intentos >= MAX_ENTRADAS_MALICIOSAS) {
            bloquearYAlertar(usuarioId, "entradas sospechosas repetidas", ip);
        }
    }

    /** Se invoca tras cada acceso denegado (401/403) de un usuario autenticado. */
    public void evaluarDenegaciones(Long usuarioId, String ip) {
        if (usuarioId == null) return;
        long denegadas = registros.countByUsuarioIdAndResultadoAndFechaAfter(
                usuarioId, ResultadoAuditoria.DENEGADO, LocalDateTime.now().minusMinutes(VENTANA_MINUTOS));
        if (denegadas == MAX_DENEGACIONES) { // == para alertar una sola vez por ráfaga
            bloquearYAlertar(usuarioId, "accesos denegados repetidos", ip);
        }
    }

    @Transactional
    public void bloquearYAlertar(Long usuarioId, String motivo, String ip) {
        Usuario u = usuarios.findById(usuarioId).orElse(null);
        if (u == null) return;
        u.bloquear(LocalDateTime.now().plusMinutes(props.bloqueo().bloqueoMinutos()));
        usuarios.save(u);
        sesiones.cerrarTodas(usuarioId);
        auditoria.registrar(usuarioId, "USUARIO_BLOQUEADO_AUTOMATICO", motivo, ResultadoAuditoria.PERMITIDO, ip);
        notificaciones.notificarAdministradores("Usuario bloqueado automáticamente",
                "Se bloqueó temporalmente a " + u.getCorreoInstitucional() + " por " + motivo + ".");
    }
}
