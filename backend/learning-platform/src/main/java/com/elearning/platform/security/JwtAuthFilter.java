package com.elearning.platform.security;

import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.EstadoUsuario;
import com.elearning.platform.repository.UsuarioRepository;
import com.elearning.platform.services.SesionService;
import com.elearning.platform.util.JsonErrores;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Autentica con el JWT y comprueba que la sesión siga activa en la base (RF-002, RF-013).
 * No es un @Component: lo crea SecurityConfig para que se ejecute una sola vez, dentro de la cadena.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Con la contraseña temporal solo se puede cambiarla o salir. */
    private static final Set<String> RUTAS_PRIMER_ACCESO =
            Set.of("/api/auth/primer-acceso", "/api/auth/logout", "/api/auth/me");

    private final JwtService jwt;
    private final SesionService sesiones;
    private final UsuarioRepository usuarios;
    private final ObjectMapper mapper;

    public JwtAuthFilter(JwtService jwt, SesionService sesiones, UsuarioRepository usuarios, ObjectMapper mapper) {
        this.jwt = jwt;
        this.sesiones = sesiones;
        this.usuarios = usuarios;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            String token = cabecera.substring(7).trim();
            Optional<UsuarioPrincipal> principal = autenticar(token);
            if (principal.isPresent()) {
                UsuarioPrincipal p = principal.get();
                Usuario u = usuarios.findById(p.id()).orElse(null);
                if (u != null && u.getEstado() == EstadoUsuario.PENDIENTE_PRIMER_ACCESO
                        && !RUTAS_PRIMER_ACCESO.contains(request.getRequestURI())) {
                    JsonErrores.escribir(mapper, request, response, 403, "PRIMER_ACCESO_REQUERIDO",
                            "Antes de continuar debes cambiar tu contraseña temporal.");
                    return;
                }
                var autenticacion = new UsernamePasswordAuthenticationToken(p, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + p.rol().name())));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            }
        }
        chain.doFilter(request, response);
    }

    private Optional<UsuarioPrincipal> autenticar(String token) {
        Optional<Claims> claims = jwt.leer(token);
        if (claims.isEmpty() || claims.get().getSubject() == null || claims.get().get("sid") == null) {
            return Optional.empty();
        }
        Long usuarioId;
        Long sesionId;
        try {
            usuarioId = Long.valueOf(claims.get().getSubject());
            sesionId = ((Number) claims.get().get("sid")).longValue();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        if (sesiones.validar(sesionId, usuarioId, token).isEmpty()) return Optional.empty();

        Usuario u = usuarios.findById(usuarioId).orElse(null);
        if (u == null) return Optional.empty();
        boolean habilitado = u.getEstado() == EstadoUsuario.ACTIVO
                || u.getEstado() == EstadoUsuario.PENDIENTE_PRIMER_ACCESO;
        if (!habilitado || u.estaBloqueadoTemporalmente()) return Optional.empty();
        return Optional.of(new UsuarioPrincipal(u.getId(), u.getCorreoInstitucional(), u.getNombre(), u.getRol(), sesionId));
    }
}
