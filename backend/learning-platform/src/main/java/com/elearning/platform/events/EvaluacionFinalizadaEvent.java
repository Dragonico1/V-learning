package com.elearning.platform.events;

import java.math.BigDecimal;

/** porcentaje y mejorPrevio van de 0 a 100; gamificación solo suma la mejora (RF-009). */
public record EvaluacionFinalizadaEvent(Long estudianteId, Long evaluacionId, Long cursoId,
                                        BigDecimal porcentaje, BigDecimal mejorPrevio) {}
