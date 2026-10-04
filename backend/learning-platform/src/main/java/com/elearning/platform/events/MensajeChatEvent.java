package com.elearning.platform.events;

import com.elearning.platform.dto.ChatDtos.MensajeDto;

/** Se difunde por WebSocket cuando el mensaje ya quedó guardado. */
public record MensajeChatEvent(Long cursoId, MensajeDto mensaje) {}
