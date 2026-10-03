package com.elearning.platform.config;

import com.elearning.platform.dto.ErrorRespuesta;
import com.elearning.platform.repository.UsuarioRepository;
import com.elearning.platform.security.AuditoriaFilter;
import com.elearning.platform.security.EntradaMaliciosaFilter;
import com.elearning.platform.security.JwtAuthFilter;
import com.elearning.platform.security.JwtService;
import com.elearning.platform.services.AuditoriaService;
import com.elearning.platform.services.SeguridadService;
import com.elearning.platform.services.SesionService;
import com.elearning.platform.util.JsonErrores;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Seguridad (RF-002, RF-013): JWT sin estado + sesión verificada en base de datos,
 * RBAC con @PreAuthorize, CORS por configuración y errores 401/403 en JSON.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, SesionService sesiones,
                                    UsuarioRepository usuarios, AuditoriaService auditoria,
                                    SeguridadService seguridad, ObjectMapper mapper,
                                    VlearningProperties props) throws Exception {
        JwtAuthFilter filtroJwt = new JwtAuthFilter(jwt, sesiones, usuarios, mapper);
        AuditoriaFilter filtroAuditoria = new AuditoriaFilter(auditoria, seguridad);
        EntradaMaliciosaFilter filtroEntrada = new EntradaMaliciosaFilter(seguridad, mapper);

        http
                .csrf(AbstractHttpConfigurer::disable) // API con token Bearer, sin cookies de sesión
                .cors(c -> c.configurationSource(fuenteCors(props)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        .frameOptions(f -> f.deny())
                        .contentTypeOptions(c -> { }))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/otp/verificar",
                                "/api/auth/recuperar", "/api/auth/restablecer").permitAll()
                        // El WebSocket se autentica en el handshake con ?token=
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> JsonErrores.escribir(mapper, req, res, 401,
                                "NO_AUTENTICADO", "Tu sesión no es válida o expiró. Inicia sesión de nuevo."))
                        .accessDeniedHandler((req, res, ex) -> JsonErrores.escribir(mapper, req, res, 403,
                                "ACCESO_DENEGADO", "No tienes permiso para realizar esta acción.")))
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(filtroAuditoria, JwtAuthFilter.class)
                .addFilterAfter(filtroEntrada, AuditoriaFilter.class);
        return http.build();
    }

    private CorsConfigurationSource fuenteCors(VlearningProperties props) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(props.cors().origenes());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        c.setExposedHeaders(List.of("Content-Disposition"));
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", c);
        return fuente;
    }
}
