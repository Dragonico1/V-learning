package com.elearning.platform.services;

import com.elearning.platform.config.VlearningProperties;
import com.elearning.platform.entity.Sesion;
import com.elearning.platform.entity.Usuario;
import com.elearning.platform.repository.SesionRepository;
import com.elearning.platform.security.JwtService;
import com.elearning.platform.util.Hashes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Sesiones (RF-002, RF-013). El JWT lleva el id de la sesión; en la base solo se
 * guarda su hash SHA-256. fecha_expiracion se desliza con la actividad
 * (inactividad configurable); el JWT tiene un tope absoluto.
 */
@Service
@RequiredArgsConstructor
public class SesionService {

    public record TokenEmitido(String token, LocalDateTime expiraPorInactividad) {}

    private final SesionRepository repositorio;
    private final JwtService jwt;
    private final VlearningProperties props;

    @Transactional
    public TokenEmitido iniciar(Usuario usuario, String ip) {
        LocalDateTime expira = LocalDateTime.now().plusMinutes(props.sesion().inactividadMinutos());
        Sesion sesion = new Sesion();
        sesion.setUsuarioId(usuario.getId());
        sesion.setTokenHash("pendiente");
        sesion.setFechaExpiracion(expira);
        sesion.setIpOrigen(ip);
        sesion = repositorio.save(sesion);

        String token = jwt.generar(usuario, sesion.getId());
        sesion.setTokenHash(Hashes.sha256Hex(token));
        return new TokenEmitido(token, expira);
    }

    /** Valida la sesión del token y desliza su expiración. Vacío si no es válida. */
    @Transactional
    public Optional<Sesion> validar(Long sesionId, Long usuarioId, String token) {
        Optional<Sesion> encontrada = repositorio.findById(sesionId);
        if (encontrada.isEmpty()) return Optional.empty();
        Sesion s = encontrada.get();
        if (!s.getUsuarioId().equals(usuarioId)) return Optional.empty();
        if (!Hashes.iguales(s.getTokenHash(), Hashes.sha256Hex(token))) return Optional.empty();
        if (!s.esValida()) return Optional.empty();

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime nueva = ahora.plusMinutes(props.sesion().inactividadMinutos());
        // Evita escribir en cada petición: solo si pasó más de un minuto desde la última extensión.
        if (nueva.minusSeconds(60).isAfter(s.getFechaExpiracion())) {
            s.setFechaExpiracion(nueva);
        }
        return Optional.of(s);
    }

    @Transactional
    public void cerrar(Long sesionId) {
        repositorio.findById(sesionId).ifPresent(Sesion::expirar);
    }

    @Transactional
    public int cerrarTodas(Long usuarioId) {
        return repositorio.expirarTodasDelUsuario(usuarioId);
    }
}
