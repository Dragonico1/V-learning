package com.elearning.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Configuración propia de V-Learning (prefijo vlearning.*). */
@ConfigurationProperties(prefix = "vlearning")
public record VlearningProperties(
        String dominioInstitucional,
        Dev dev,
        Mail mail,
        Jwt jwt,
        Sesion sesion,
        Otp otp,
        Recuperacion recuperacion,
        Bloqueo bloqueo,
        Chat chat,
        Notificaciones notificaciones,
        Cors cors) {

    public record Dev(boolean exponerSecretos, boolean sembrarDatos) {}

    public record Mail(String remitente) {}

    public record Jwt(String secret, int expiracionHoras) {}

    public record Sesion(int inactividadMinutos) {}

    public record Otp(int minutosValidez) {}

    public record Recuperacion(int minutosValidez, String urlBase) {}

    public record Bloqueo(int intentosMaximos, int ventanaMinutos, int bloqueoMinutos) {}

    public record Chat(int longitudMaxima, List<String> palabrasProhibidas) {}

    public record Notificaciones(int diasInactividad, String cron) {}

    public record Cors(List<String> origenes) {}
}
