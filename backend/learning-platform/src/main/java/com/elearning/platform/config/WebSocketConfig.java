package com.elearning.platform.config;

import com.elearning.platform.services.ChatWebSocketHandler;
import com.elearning.platform.services.ChatHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** WebSocket de la comunidad: /ws/cursos/{id}?token=JWT (RF-010). */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler handler;
    private final ChatHandshakeInterceptor interceptor;
    private final VlearningProperties props;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/cursos/*")
                .addInterceptors(interceptor)
                .setAllowedOrigins(props.cors().origenes().toArray(new String[0]));
    }
}
