package com.elearning.platform.services;

import com.elearning.platform.entity.Contenido;
import com.elearning.platform.entity.RecursoAccesible;
import com.elearning.platform.enums.FormatoContenido;
import com.elearning.platform.enums.TipoRecursoAccesible;
import com.elearning.platform.util.TextoApoyo;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Reglas de publicación conforme (RF-019): un video sin subtítulos y transcripción, o un podcast
 * sin transcripción, es «no conforme» y no puede publicarse. Las advertencias no bloquean.
 */
public final class ConformidadAccesibilidad {

    private ConformidadAccesibilidad() {}

    public static List<String> faltantes(Contenido c, List<RecursoAccesible> recursos) {
        Set<TipoRecursoAccesible> tipos = disponibles(recursos);
        List<String> faltan = new ArrayList<>();
        if (c.getFormato() == FormatoContenido.VIDEO || c.getFormato() == FormatoContenido.PODCAST) {
            if (esVacio(c.getUrlRecurso())) {
                faltan.add("Falta el enlace del " + (c.getFormato() == FormatoContenido.VIDEO ? "video." : "podcast."));
            }
        }
        if (c.getFormato() == FormatoContenido.VIDEO) {
            if (!tipos.contains(TipoRecursoAccesible.SUBTITULO)) faltan.add("Falta cargar los subtítulos del video.");
            if (!tipos.contains(TipoRecursoAccesible.TRANSCRIPCION)) faltan.add("Falta la transcripción completa del video.");
        }
        if (c.getFormato() == FormatoContenido.PODCAST && !tipos.contains(TipoRecursoAccesible.TRANSCRIPCION)) {
            faltan.add("Falta la transcripción completa del podcast.");
        }
        if (c.getFormato() == FormatoContenido.LECTURA && esVacio(c.getCuerpo())) {
            faltan.add("Falta el texto de la lectura.");
        }
        return faltan;
    }

    public static List<String> advertencias(Contenido c) {
        List<String> avisos = new ArrayList<>();
        if (c.getFormato() != FormatoContenido.LECTURA && esVacio(c.getCuerpo())) {
            avisos.add("Agrega un texto estructurado como alternativa para quienes no puedan usar este formato.");
        }
        int sinAlt = TextoApoyo.imagenesSinTextoAlternativo(c.getCuerpo());
        if (sinAlt > 0) {
            avisos.add("Hay " + sinAlt + " imagen(es) sin texto alternativo. Escribe la descripción entre corchetes: ![descripción](enlace).");
        }
        return avisos;
    }

    public static Set<TipoRecursoAccesible> disponibles(List<RecursoAccesible> recursos) {
        Set<TipoRecursoAccesible> tipos = EnumSet.noneOf(TipoRecursoAccesible.class);
        for (RecursoAccesible r : recursos) if (r.isDisponible()) tipos.add(r.getTipo());
        return tipos;
    }

    private static boolean esVacio(String s) {
        return s == null || s.isBlank();
    }
}
