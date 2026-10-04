package com.elearning.platform.util;

import com.elearning.platform.enums.EstiloVark;
import com.elearning.platform.enums.FormatoContenido;
import com.elearning.platform.enums.TipoEvaluacion;

import java.util.List;

/** Qué formato de contenido y qué tipo de evaluación encajan con cada método VARK (RF-005, RF-007). */
public final class AfinidadVark {

    private AfinidadVark() {}

    public static FormatoContenido formatoDe(EstiloVark estilo) {
        return switch (estilo) {
            case VISUAL -> FormatoContenido.VIDEO;
            case AUDITIVO -> FormatoContenido.PODCAST;
            case LECTURA_ESCRITURA -> FormatoContenido.LECTURA;
            case KINESTESICO -> FormatoContenido.SIMULACION;
        };
    }

    /** 0 = formato del método principal, 1 = del secundario, 2 = el resto. Sin método: 2 para todos. */
    public static int rangoContenido(FormatoContenido formato, EstiloVark principal, EstiloVark secundario) {
        if (principal != null && formato == formatoDe(principal)) return 0;
        if (secundario != null && formato == formatoDe(secundario)) return 1;
        return 2;
    }

    public static List<TipoEvaluacion> preferenciaEvaluacion(EstiloVark estilo) {
        return switch (estilo) {
            case VISUAL -> List.of(TipoEvaluacion.SIMULACION, TipoEvaluacion.QUIZ);
            case AUDITIVO -> List.of(TipoEvaluacion.QUIZ);
            case LECTURA_ESCRITURA -> List.of(TipoEvaluacion.QUIZ, TipoEvaluacion.PRACTICA);
            case KINESTESICO -> List.of(TipoEvaluacion.PRACTICA, TipoEvaluacion.SIMULACION);
        };
    }

    public static int rangoEvaluacion(TipoEvaluacion tipo, EstiloVark principal) {
        if (principal == null) return 0;
        List<TipoEvaluacion> pref = preferenciaEvaluacion(principal);
        int i = pref.indexOf(tipo);
        return i >= 0 ? i : pref.size();
    }
}
