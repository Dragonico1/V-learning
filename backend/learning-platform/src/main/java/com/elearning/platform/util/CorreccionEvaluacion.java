package com.elearning.platform.util;

import com.elearning.platform.enums.Clasificacion;
import com.elearning.platform.enums.TipoPregunta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Corrección automática de preguntas (RF-007) y clasificación del resultado (RF-008). Sin dependencias. */
public final class CorreccionEvaluacion {

    public static final BigDecimal UMBRAL_EXCELENTE = new BigDecimal("85");
    public static final BigDecimal UMBRAL_ACEPTABLE = new BigDecimal("60");

    public record OpcionDato(Long id, String texto, boolean correcta) {}

    /** obtenido: puntos de la pregunta; completa: respuesta totalmente correcta. */
    public record Resultado(BigDecimal obtenido, boolean completa) {}

    private CorreccionEvaluacion() {}

    /**
     * Selección única y verdadero/falso: el valor es el id de la opción.
     * Selección múltiple: ids separados por coma; puntaje proporcional (aciertos − errores) / correctas, mínimo 0.
     * Abierta: coincide (sin tildes ni mayúsculas) con alguna respuesta aceptada.
     */
    public static Resultado corregir(TipoPregunta tipo, BigDecimal puntaje, List<OpcionDato> opciones, String valor) {
        if (valor == null || valor.isBlank()) return new Resultado(BigDecimal.ZERO, false);
        switch (tipo) {
            case SELECCION_UNICA:
            case VERDADERO_FALSO: {
                Long id = parsear(valor.trim());
                boolean ok = id != null && opciones.stream().anyMatch(o -> o.id().equals(id) && o.correcta());
                return ok ? new Resultado(puntaje, true) : new Resultado(BigDecimal.ZERO, false);
            }
            case SELECCION_MULTIPLE: {
                Set<Long> elegidas = new HashSet<>();
                for (String parte : valor.split(",")) {
                    Long id = parsear(parte.trim());
                    if (id != null) elegidas.add(id);
                }
                Set<Long> correctas = new HashSet<>();
                for (OpcionDato o : opciones) if (o.correcta()) correctas.add(o.id());
                if (correctas.isEmpty()) return new Resultado(BigDecimal.ZERO, false);
                long aciertos = elegidas.stream().filter(correctas::contains).count();
                long errores = elegidas.size() - aciertos;
                double fraccion = Math.max(0.0, (double) (aciertos - errores) / correctas.size());
                BigDecimal obtenido = puntaje.multiply(BigDecimal.valueOf(fraccion)).setScale(2, RoundingMode.HALF_UP);
                return new Resultado(obtenido, aciertos == correctas.size() && errores == 0);
            }
            case ABIERTA: {
                String dada = normalizar(valor);
                boolean ok = opciones.stream().anyMatch(o -> o.correcta() && normalizar(o.texto()).equals(dada));
                return ok ? new Resultado(puntaje, true) : new Resultado(BigDecimal.ZERO, false);
            }
            default:
                return new Resultado(BigDecimal.ZERO, false);
        }
    }

    public static Clasificacion clasificar(BigDecimal porcentaje) {
        if (porcentaje.compareTo(UMBRAL_EXCELENTE) >= 0) return Clasificacion.EXCELENTE;
        if (porcentaje.compareTo(UMBRAL_ACEPTABLE) >= 0) return Clasificacion.ACEPTABLE;
        return Clasificacion.INSUFICIENTE;
    }

    /** porcentaje = puntaje / máximo × 100, con 2 decimales. */
    public static BigDecimal porcentaje(BigDecimal puntaje, BigDecimal maximo) {
        if (puntaje == null || maximo == null || maximo.signum() == 0) return BigDecimal.ZERO.setScale(2);
        return puntaje.multiply(new BigDecimal("100")).divide(maximo, 2, RoundingMode.HALF_UP);
    }

    public static String normalizar(String s) {
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.replaceAll("[\\p{Punct}¿¡]+", " ").trim().replaceAll("\\s+", " ");
    }

    private static Long parsear(String s) {
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
