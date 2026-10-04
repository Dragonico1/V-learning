package com.elearning.platform.events;

/** Se publica cuando el avance del curso llega al 100 %. */
public record CursoFinalizadoEvent(Long estudianteId, Long cursoId) {}
