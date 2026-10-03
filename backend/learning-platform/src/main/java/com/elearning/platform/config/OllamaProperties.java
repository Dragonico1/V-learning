package com.elearning.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tutor de IA con Ollama local (ollama.*). Solo texto. */
@ConfigurationProperties(prefix = "ollama")
public record OllamaProperties(
        String url,
        String model,
        int timeoutSegundos,
        int numPredict,
        double temperatura) {}
