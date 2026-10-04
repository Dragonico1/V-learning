package com.elearning.platform.util;

import java.util.Map;

/** Eventos que otorgan puntos y su valor por defecto (editable por reglas del administrador). */
public final class EventosGamificacion {

    public static final String CONTENIDO_COMPLETADO = "CONTENIDO_COMPLETADO";
    public static final String EVALUACION_COMPLETADA = "EVALUACION_COMPLETADA";
    public static final String TEST_VARK = "TEST_VARK";
    public static final String CURSO_FINALIZADO = "CURSO_FINALIZADO";

    public static final int PUNTOS_POR_NIVEL = 500;

    public static final Map<String, Integer> POR_DEFECTO = Map.of(
            CONTENIDO_COMPLETADO, 50,
            EVALUACION_COMPLETADA, 100,
            TEST_VARK, 30,
            CURSO_FINALIZADO, 0);

    private EventosGamificacion() {}

    public static int nivelPara(int puntos) {
        return 1 + Math.max(0, puntos) / PUNTOS_POR_NIVEL;
    }
}
