package com.elearning.platform.services;

import com.elearning.platform.dto.ChatDtos.MensajeDto;
import com.elearning.platform.events.MensajeChatEvent;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.security.UsuarioPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canal en tiempo real por curso. Los mensajes llegan por REST o por el propio socket
 * ({"contenido":"..."}); ambos se guardan primero y se difunden después de confirmar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chat;
    private final ObjectMapper mapper;
    private final Map<Long, Set<WebSocketSession>> salas = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long cursoId = (Long) session.getAttributes().get(ChatHandshakeInterceptor.ATTR_CURSO);
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(session, 5000, 64 * 1024);
        session.getAttributes().put("segura", segura);
        salas.computeIfAbsent(cursoId, k -> ConcurrentHashMap.newKeySet()).add(segura);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        UsuarioPrincipal u = (UsuarioPrincipal) session.getAttributes().get(ChatHandshakeInterceptor.ATTR_USUARIO);
        Long cursoId = (Long) session.getAttributes().get(ChatHandshakeInterceptor.ATTR_CURSO);
        try {
            JsonNode json = mapper.readTree(message.getPayload());
            chat.enviar(u, cursoId, json.path("contenido").asText(""), null);
        } catch (ApiException e) {
            enviarError(session, e.getCodigo(), e.getMessage());
        } catch (IOException e) {
            enviarError(session, "SOLICITUD_INVALIDA", "No pudimos entender el mensaje.");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long cursoId = (Long) session.getAttributes().get(ChatHandshakeInterceptor.ATTR_CURSO);
        Object segura = session.getAttributes().get("segura");
        Set<WebSocketSession> sala = salas.get(cursoId);
        if (sala != null && segura != null) sala.remove((WebSocketSession) segura);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alEnviarMensaje(MensajeChatEvent evento) {
        Set<WebSocketSession> sala = salas.get(evento.cursoId());
        if (sala == null || sala.isEmpty()) return;
        String json;
        try {
            json = mapper.writeValueAsString(Map.of("tipo", "MENSAJE", "mensaje", evento.mensaje()));
        } catch (IOException e) {
            log.error("No se pudo serializar el mensaje del chat", e);
            return;
        }
        for (WebSocketSession s : sala) {
            try {
                if (s.isOpen()) s.sendMessage(new TextMessage(json));
            } catch (IOException | RuntimeException e) {
                log.debug("No se pudo entregar un mensaje a una conexión: {}", e.getMessage());
            }
        }
    }

    private void enviarError(WebSocketSession session, String codigo, String mensaje) throws IOException {
        Object segura = session.getAttributes().get("segura");
        WebSocketSession destino = segura != null ? (WebSocketSession) segura : session;
        destino.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of("tipo", "ERROR", "codigo", codigo, "mensaje", mensaje))));
    }
}
