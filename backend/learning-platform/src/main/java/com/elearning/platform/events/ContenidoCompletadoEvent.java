package com.elearning.platform.events;

/** Se publica la primera vez que un estudiante completa un contenido (gamificación, RF-009). */
public record ContenidoCompletadoEvent(Long estudianteId, Long contenidoId, Long cursoId) {}
