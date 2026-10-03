package com.elearning.platform.security;

import com.elearning.platform.enums.ResultadoAuditoria;
import com.elearning.platform.services.AuditoriaService;
import com.elearning.platform.services.SeguridadService;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Registra en auditoría (RF-013) toda operación que modifica datos y todo acceso
 * denegado o con error. Las lecturas correctas no se registran para no saturar la tabla.
 * Los eventos de /api/auth los registra AuthService con su propio significado.
 */
@Slf4j
public class AuditoriaFilter extends OncePerRequestFilter {

    private final AuditoriaService auditoria;
    private final SeguridadService seguridad;

    public AuditoriaFilter(AuditoriaService auditoria, SeguridadService seguridad) {
        this.auditoria = auditoria;
        this.seguridad = seguridad;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || ruta.startsWith("/api/auth/")
                || !ruta.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            registrar(request, response);
        }
    }

    private void registrar(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (request.getAttribute(EntradaMaliciosaFilter.ATRIBUTO_RECHAZO) != null) return; // ya auditada
            int estado = response.getStatus();
            boolean lectura = "GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod());
            if (lectura && estado < 400) return;

            ResultadoAuditoria resultado = estado < 400 ? ResultadoAuditoria.PERMITIDO
                    : (estado == 401 || estado == 403) ? ResultadoAuditoria.DENEGADO : ResultadoAuditoria.ERROR;

            Long usuarioId = null;
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            if (a != null && a.getPrincipal() instanceof UsuarioPrincipal p) usuarioId = p.id();

            String ip = IpUtil.de(request);
            auditoria.registrar(usuarioId, "HTTP " + request.getMethod() + " (" + estado + ")",
                    request.getRequestURI(), resultado, ip);
            if (resultado == ResultadoAuditoria.DENEGADO) {
                seguridad.evaluarDenegaciones(usuarioId, ip);
            }
        } catch (RuntimeException e) {
            log.error("Fallo al auditar la petición: {}", e.getMessage());
        }
    }
}
