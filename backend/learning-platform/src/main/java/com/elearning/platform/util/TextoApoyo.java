package com.elearning.platform.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Apoyo cognitivo (RF-022) sin IA: divide el texto en unidades pequeñas, extrae pasos, genera un
 * resumen extractivo con control de calidad, lee el glosario y propone conceptos clave.
 *
 * Formato del cuerpo de una lección: texto con títulos «## Título», listas numeradas o con guiones
 * y, opcionalmente, una sección «## Glosario» con líneas «Término: definición».
 */
public final class TextoApoyo {

    public record Unidad(int numero, String titulo, String texto) {}

    public record Resumen(String texto, boolean aceptable) {}

    public record Termino(String termino, String definicion) {}

    private static final int MAX_PALABRAS_UNIDAD = 60;
    private static final Pattern IMAGEN = Pattern.compile("!\\[([^\\]]*)]\\(([^)]*)\\)");
    private static final Pattern TITULO = Pattern.compile("^\\s*#{1,6}\\s*(.+?)\\s*$");
    private static final Pattern GLOSARIO = Pattern.compile("^\\s*#{0,6}\\s*glosario\\s*:?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASO = Pattern.compile("^\\s*(?:\\d+[.)]|[-*•])\\s+(.+?)\\s*$");
    private static final Pattern TERMINO = Pattern.compile("^\\s*[-*•]?\\s*([^:]{2,60}):\\s*(.{3,})$");
    private static final Pattern FIN_ORACION = Pattern.compile("(?<=[.!?…])\\s+");

    private static final Set<String> PALABRAS_VACIAS = Set.copyOf(Arrays.asList((
            "para,como,pero,porque,cuando,donde,entre,sobre,desde,hasta,hacia,esta,este,estos,estas,ese,esa,esos,esas,"
                    + "aqui,alli,ahora,antes,despues,tambien,ademas,aunque,siempre,nunca,mas,menos,muy,cada,todo,todos,"
                    + "toda,todas,otro,otra,otros,otras,mismo,misma,tiene,tienen,tener,puede,pueden,poder,hace,hacen,"
                    + "hacer,sera,seran,son,fue,fueron,ser,estar,esta,estan,esto,eso,sus,les,los,las,del,una,uno,unos,"
                    + "unas,con,sin,por,que,nos,ellos,ellas,usted,ustedes,cual,cuales,quien,quienes,donde,asi,solo,"
                    + "segun,durante,mediante,otras,algunos,algunas,algun,alguna,tanto,tanta,parte,forma,manera").split(",")));

    private TextoApoyo() {}

    /** Texto plano: sin imágenes ni marcas de Markdown y sin la sección de glosario. */
    public static String limpiar(String cuerpo) {
        if (cuerpo == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String linea : cuerpo.split("\\R")) {
            if (GLOSARIO.matcher(linea).matches()) break;
            String l = IMAGEN.matcher(linea).replaceAll("");
            Matcher t = TITULO.matcher(l);
            if (t.matches()) l = t.group(1) + ".";
            l = l.replace("**", "").replace("__", "").trim();
            if (!l.isEmpty()) sb.append(l).append('\n');
        }
        return sb.toString().trim();
    }

    /** Divide el contenido en unidades de máximo ~60 palabras, respetando los títulos. */
    public static List<Unidad> dividir(String cuerpo) {
        List<Unidad> unidades = new ArrayList<>();
        if (cuerpo == null || cuerpo.isBlank()) return unidades;
        String tituloActual = null;
        StringBuilder seccion = new StringBuilder();
        List<String[]> secciones = new ArrayList<>();
        for (String linea : cuerpo.split("\\R")) {
            if (GLOSARIO.matcher(linea).matches()) break;
            Matcher t = TITULO.matcher(linea);
            if (t.matches()) {
                if (seccion.toString().isBlank() == false) secciones.add(new String[]{tituloActual, seccion.toString()});
                tituloActual = t.group(1).replace("**", "");
                seccion = new StringBuilder();
            } else {
                String l = IMAGEN.matcher(linea).replaceAll("").replace("**", "").replace("__", "").trim();
                if (!l.isEmpty()) seccion.append(l).append(' ');
            }
        }
        if (!seccion.toString().isBlank()) secciones.add(new String[]{tituloActual, seccion.toString()});

        int numero = 1;
        for (String[] s : secciones) {
            StringBuilder actual = new StringBuilder();
            int palabras = 0;
            for (String oracion : FIN_ORACION.split(s[1].trim())) {
                int n = contarPalabras(oracion);
                if (palabras > 0 && palabras + n > MAX_PALABRAS_UNIDAD) {
                    unidades.add(new Unidad(numero++, s[0] != null ? s[0] : "Parte " + numero, actual.toString().trim()));
                    actual = new StringBuilder();
                    palabras = 0;
                }
                actual.append(oracion).append(' ');
                palabras += n;
            }
            if (!actual.toString().isBlank()) {
                unidades.add(new Unidad(numero++, s[0] != null ? s[0] : "Parte " + numero, actual.toString().trim()));
            }
        }
        return unidades;
    }

    /** Pasos de listas numeradas o con guiones; si no hay, un paso por unidad. */
    public static List<String> pasos(String cuerpo, List<Unidad> unidades) {
        List<String> pasos = new ArrayList<>();
        if (cuerpo != null) {
            for (String linea : cuerpo.split("\\R")) {
                if (GLOSARIO.matcher(linea).matches()) break;
                Matcher m = PASO.matcher(linea);
                if (m.matches()) pasos.add(m.group(1).replace("**", ""));
            }
        }
        if (pasos.isEmpty()) {
            for (Unidad u : unidades) pasos.add("Lee «" + u.titulo() + "» y piensa en una idea principal.");
        }
        return pasos;
    }

    /** Resumen extractivo. aceptable=false si el texto es muy corto o el resumen no reduce lo suficiente. */
    public static Resumen resumir(String cuerpo) {
        String texto = limpiar(cuerpo);
        List<String> oraciones = new ArrayList<>(Arrays.asList(FIN_ORACION.split(texto.replace('\n', ' '))));
        oraciones.removeIf(o -> o.isBlank());
        int palabrasTotal = contarPalabras(texto);
        if (oraciones.size() < 4 || palabrasTotal < 60) {
            return new Resumen(texto, false);
        }
        Map<String, Integer> frecuencia = frecuencias(texto);
        int k = Math.max(2, Math.min(4, oraciones.size() / 3));
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < oraciones.size(); i++) indices.add(i);
        indices.sort(Comparator.comparingDouble((Integer i) -> -puntaje(oraciones.get(i), frecuencia)));
        List<Integer> elegidos = new ArrayList<>(indices.subList(0, k));
        elegidos.sort(Integer::compare);
        StringBuilder sb = new StringBuilder();
        for (int i : elegidos) sb.append(oraciones.get(i).trim()).append(' ');
        String resumen = sb.toString().trim();
        boolean aceptable = contarPalabras(resumen) <= palabrasTotal * 0.7;
        return aceptable ? new Resumen(resumen, true) : new Resumen(texto, false);
    }

    /** Líneas «Término: definición» bajo el título «Glosario». */
    public static List<Termino> glosario(String cuerpo) {
        List<Termino> lista = new ArrayList<>();
        if (cuerpo == null) return lista;
        boolean dentro = false;
        for (String linea : cuerpo.split("\\R")) {
            if (GLOSARIO.matcher(linea).matches()) {
                dentro = true;
                continue;
            }
            if (!dentro) continue;
            if (TITULO.matcher(linea).matches()) break;
            Matcher m = TERMINO.matcher(linea);
            if (m.matches()) lista.add(new Termino(m.group(1).replace("**", "").trim(), m.group(2).trim()));
        }
        return lista;
    }

    /** Palabras más frecuentes (sin palabras vacías) para el repaso de conceptos. */
    public static List<String> conceptosClave(String cuerpo, int cantidad) {
        Map<String, Integer> frecuencia = frecuencias(limpiar(cuerpo));
        return frecuencia.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(cantidad).map(Map.Entry::getKey).toList();
    }

    /** Alt vacío en imágenes de Markdown: ![](url). Devuelve la cantidad. */
    public static int imagenesSinTextoAlternativo(String cuerpo) {
        if (cuerpo == null) return 0;
        Matcher m = IMAGEN.matcher(cuerpo);
        int n = 0;
        while (m.find()) if (m.group(1).isBlank()) n++;
        return n;
    }

    /** Sustituye el alt vacío por una descripción genérica temporal. */
    public static String conAltGenerico(String cuerpo, String descripcionGenerica) {
        if (cuerpo == null) return null;
        Matcher m = IMAGEN.matcher(cuerpo);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String alt = m.group(1).isBlank() ? descripcionGenerica : m.group(1);
            m.appendReplacement(sb, Matcher.quoteReplacement("![" + alt + "](" + m.group(2) + ")"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ------------------------------------------------------------------

    private static int contarPalabras(String s) {
        String t = s.trim();
        return t.isEmpty() ? 0 : t.split("\\s+").length;
    }

    private static Map<String, Integer> frecuencias(String texto) {
        Map<String, Integer> f = new HashMap<>();
        for (String p : normalizar(texto).split("[^\\p{L}]+")) {
            if (p.length() >= 5 && !PALABRAS_VACIAS.contains(p)) f.merge(p, 1, Integer::sum);
        }
        return new LinkedHashMap<>(f);
    }

    private static double puntaje(String oracion, Map<String, Integer> frecuencia) {
        String[] palabras = normalizar(oracion).split("[^\\p{L}]+");
        double suma = 0;
        for (String p : palabras) suma += frecuencia.getOrDefault(p, 0);
        return suma / Math.sqrt(Math.max(1, palabras.length));
    }

    private static String normalizar(String s) {
        String n = java.text.Normalizer.normalize(s.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}+", "");
    }
}
