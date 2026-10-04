package com.elearning.platform.services;

import com.elearning.platform.config.OllamaProperties;
import com.elearning.platform.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Cliente mínimo de Ollama (/api/chat sin streaming). Solo texto. */
@Slf4j
@Component
public class OllamaClient {

    private final RestClient http;
    private final OllamaProperties props;

    public OllamaClient(OllamaProperties props) {
        this.props = props;
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(props.timeoutSegundos()));
        this.http = RestClient.builder().baseUrl(props.url()).requestFactory(fabrica).build();
    }

    public boolean disponible() {
        try {
            http.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Devuelve el texto de la respuesta o lanza 503 con un mensaje claro si Ollama no responde. */
    public String chat(String sistema, String pregunta) {
        Map<String, Object> cuerpo = Map.of(
                "model", props.model(),
                "stream", false,
                "messages", List.of(Map.of("role", "system", "content", sistema), Map.of("role", "user", "content", pregunta)),
                "options", Map.of("temperature", props.temperatura(), "num_predict", props.numPredict()));
        try {
            JsonNode r = http.post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON).body(cuerpo)
                    .retrieve().body(JsonNode.class);
            return r == null ? "" : r.path("message").path("content").asText("");
        } catch (RuntimeException e) {
            log.warn("Ollama no respondió: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TUTOR_NO_DISPONIBLE",
                    "El tutor no está disponible en este momento. Intenta de nuevo en unos minutos o escribe a tu instructor.");
        }
    }
}
