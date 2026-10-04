package com.elearning.platform.services;

import com.elearning.platform.config.VlearningProperties;
import com.elearning.platform.entity.CodigoOtp;
import com.elearning.platform.entity.Usuario;
import com.elearning.platform.repository.CodigoOtpRepository;
import com.elearning.platform.util.Hashes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/** Segundo factor (RF-002): código de 6 dígitos, válido 5 minutos, guardado solo como hash. */
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final CodigoOtpRepository repositorio;
    private final VlearningProperties props;

    /** Genera un código nuevo e invalida los anteriores. Devuelve el código en claro para enviarlo. */
    @Transactional
    public String generar(Usuario usuario, String ip) {
        repositorio.invalidarPendientes(usuario.getId());
        String codigo = String.format("%06d", ALEATORIO.nextInt(1_000_000));
        CodigoOtp otp = new CodigoOtp();
        otp.setUsuarioId(usuario.getId());
        otp.setCodigoHash(hash(usuario.getId(), codigo));
        otp.setFechaExpiracion(LocalDateTime.now().plusMinutes(props.otp().minutosValidez()));
        otp.setIpOrigen(ip);
        repositorio.save(otp);
        return codigo;
    }

    /** true si el código es correcto y vigente; lo consume. */
    @Transactional
    public boolean verificar(Usuario usuario, String codigo) {
        Optional<CodigoOtp> pendiente = repositorio.findFirstByUsuarioIdAndUsadoFalseOrderByFechaCreacionDesc(usuario.getId());
        if (pendiente.isEmpty() || !pendiente.get().vigente()) return false;
        CodigoOtp otp = pendiente.get();
        if (!Hashes.iguales(otp.getCodigoHash(), hash(usuario.getId(), codigo))) return false;
        otp.setUsado(true);
        return true;
    }

    @Transactional
    public void invalidar(Usuario usuario) {
        repositorio.invalidarPendientes(usuario.getId());
    }

    /** Fecha de creación del último código pendiente (para contar intentos fallidos desde entonces). */
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> inicioCodigoVigente(Usuario usuario) {
        return repositorio.findFirstByUsuarioIdAndUsadoFalseOrderByFechaCreacionDesc(usuario.getId())
                .map(CodigoOtp::getFechaCreacion);
    }

    /** HMAC-like: el secreto del servidor evita recuperar el código de 6 dígitos a partir de la base. */
    private String hash(Long usuarioId, String codigo) {
        return Hashes.sha256Hex(usuarioId + ":" + codigo + ":" + props.jwt().secret());
    }
}
