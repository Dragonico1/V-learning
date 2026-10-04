package com.elearning.platform.services;

import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.EstadoUsuario;
import com.elearning.platform.repository.UsuarioRepository;
import com.elearning.platform.security.JwtService;
import com.elearning.platform.security.UsuarioPrincipal;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * Autentica el WebSocket con el mismo JWT y la misma sesión de base de datos que la API REST y
 * comprueba que la persona sea miembro del curso antes de abrir el canal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USUARIO = "usuario";
    public static final String ATTR_CURSO = "cursoId";

    private final JwtService jwt;
    private final SesionService sesiones;
    private final UsuarioRepository usuarios;
    private final ChatService chat;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                                   Map<String, Object> attributes) {
        try {
            String ruta = request.getURI().getPath();
            Long cursoId = Long.valueOf(ruta.substring(ruta.lastIndexOf('/') + 1));
            String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
            if (token == null || token.isBlank()) return rechazar(response, HttpStatus.UNAUTHORIZED);

            Optional<Claims> claims = jwt.leer(token);
            if (claims.isEmpty() || claims.get().get("sid") == null) return rechazar(response, HttpStatus.UNAUTHORIZED);
            Long usuarioId = Long.valueOf(claims.get().getSubject());
            Long sesionId = ((Number) claims.get().get("sid")).longValue();
            if (sesiones.validar(sesionId, usuarioId, token).isEmpty()) return rechazar(response, HttpStatus.UNAUTHORIZED);

            Usuario u = usuarios.findById(usuarioId).orElse(null);
            if (u == null || u.getEstado() != EstadoUsuario.ACTIVO || u.estaBloqueadoTemporalmente()) {
                return rechazar(response, HttpStatus.UNAUTHORIZED);
            }
            chat.exigirMiembro(cursoId, u.getId(), u.getRol());
            attributes.put(ATTR_USUARIO, new UsuarioPrincipal(u.getId(), u.getCorreoInstitucional(), u.getNombre(), u.getRol(), sesionId));
            attributes.put(ATTR_CURSO, cursoId);
            return true;
        } catch (RuntimeException e) {
            log.debug("Handshake rechazado: {}", e.getMessage());
            return rechazar(response, HttpStatus.FORBIDDEN);
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                               Exception exception) {
        // nada
    }

    private static boolean rechazar(ServerHttpResponse response, HttpStatus estado) {
        response.setStatusCode(estado);
        return false;
    }
}
