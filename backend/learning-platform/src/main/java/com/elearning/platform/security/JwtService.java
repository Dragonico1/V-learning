package com.elearning.platform.security;

import com.elearning.platform.config.VlearningProperties;
import com.elearning.platform.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey clave;
    private final long horas;

    public JwtService(VlearningProperties props) {
        String secreto = props.jwt().secret();
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "La variable de entorno JWT_SECRET es obligatoria y debe tener al menos 32 caracteres.");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.horas = props.jwt().expiracionHoras();
    }

    public String generar(Usuario usuario, Long sesionId) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("rol", usuario.getRol().name())
                .claim("sid", sesionId)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(horas, ChronoUnit.HOURS)))
                .signWith(clave)
                .compact();
    }

    /** Devuelve los claims solo si la firma y la expiración son válidas. */
    public Optional<Claims> leer(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Instant expiracionMaxima() {
        return Instant.now().plus(horas, ChronoUnit.HOURS);
    }
}
