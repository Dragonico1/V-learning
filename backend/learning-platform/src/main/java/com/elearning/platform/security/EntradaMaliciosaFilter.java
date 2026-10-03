package com.elearning.platform.security;

import com.elearning.platform.services.SeguridadService;
import com.elearning.platform.util.IpUtil;
import com.elearning.platform.util.JsonErrores;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detecta patrones de inyección SQL y XSS en parámetros y cuerpo JSON (RF-013, extensión).
 * Es una defensa adicional: la defensa principal son las consultas parametrizadas de JPA
 * y el escapado automático de React.
 */
public class EntradaMaliciosaFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_RECHAZO = "vlearning.entradaRechazada";
    private static final int LIMITE_CUERPO = 1_000_000; // 1 MB

    private static final int CI = Pattern.CASE_INSENSITIVE;
    private static final List<Pattern> XSS = List.of(
            Pattern.compile("<\\s*script", CI),
            Pattern.compile("<\\s*(iframe|object|embed)\\b", CI),
            Pattern.compile("javascript\\s*:", CI),
            Pattern.compile("<[^>]*\\bon(error|load|click|mouseover|focus|mouseenter)\\s*=", CI));
    private static final List<Pattern> SQL = List.of(
            Pattern.compile("\\bunion\\b\\s+(all\\s+)?select\\b", CI),
            Pattern.compile(";\\s*(drop|alter|truncate)\\s+\\w+", CI),
            Pattern.compile(";\\s*(delete\\s+from|insert\\s+into|update\\s+\\w+\\s+set)\\b", CI),
            Pattern.compile("'\\s*(or|and)\\s+'?\\d+'?\\s*=\\s*'?\\d+", CI),
            Pattern.compile("\\b(xp_cmdshell|sp_executesql)\\b|waitfor\\s+delay", CI),
            Pattern.compile("'\\s*;\\s*--", CI));
    private static final Pattern ESCAPE_UNICODE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

    private final SeguridadService seguridad;
    private final ObjectMapper mapper;

    public EntradaMaliciosaFilter(SeguridadService seguridad, ObjectMapper mapper) {
        this.seguridad = seguridad;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tipo = null;
        for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
            for (String valor : e.getValue()) {
                tipo = detectar(valor);
                if (tipo != null) break;
            }
            if (tipo != null) break;
        }

        HttpServletRequest actual = request;
        if (tipo == null && tieneCuerpoJson(request)) {
            byte[] bytes = request.getInputStream().readNBytes(LIMITE_CUERPO + 1);
            if (bytes.length > LIMITE_CUERPO) {
                JsonErrores.escribir(mapper, request, response, 413, "CUERPO_DEMASIADO_GRANDE",
                        "El contenido enviado es demasiado grande.");
                return;
            }
            tipo = detectar(new String(bytes, StandardCharsets.UTF_8));
            actual = new CachedBodyRequestWrapper(request, bytes);
        }

        if (tipo != null) {
            request.setAttribute(ATRIBUTO_RECHAZO, Boolean.TRUE);
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            Long usuarioId = (a != null && a.getPrincipal() instanceof UsuarioPrincipal p) ? p.id() : null;
            seguridad.reportarEntradaMaliciosa(usuarioId, IpUtil.de(request), request.getRequestURI(), tipo);
            JsonErrores.escribir(mapper, request, response, 400, "ENTRADA_RECHAZADA",
                    "Detectamos contenido no permitido en tu solicitud. Quita los símbolos especiales y vuelve a intentarlo.");
            return;
        }
        chain.doFilter(actual, response);
    }

    private static boolean tieneCuerpoJson(HttpServletRequest request) {
        String metodo = request.getMethod();
        boolean conCuerpo = "POST".equals(metodo) || "PUT".equals(metodo) || "PATCH".equals(metodo);
        String tipo = request.getContentType();
        return conCuerpo && tipo != null && tipo.toLowerCase().contains("json");
    }

    /** Devuelve "XSS", "SQL" o null. */
    static String detectar(String texto) {
        if (texto == null || texto.isEmpty()) return null;
        String limpio = desescapar(texto);
        for (Pattern p : XSS) if (p.matcher(limpio).find()) return "XSS";
        for (Pattern p : SQL) if (p.matcher(limpio).find()) return "SQL";
        return null;
    }

    /** Resuelve secuencias \\uXXXX del JSON para que no sirvan de evasión. */
    private static String desescapar(String texto) {
        Matcher m = ESCAPE_UNICODE.matcher(texto);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(m.group(1), 16))));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
