package com.elearning.platform.services;

import com.elearning.platform.dto.AccesibilidadDtos.ConfiguracionDto;
import com.elearning.platform.enums.CategoriaAccesibilidad;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Qué ajustes activa cada categoría (RF-014) y qué avisos se dan sobre una configuración (RF-016).
 * El tiempo adicional es un porcentaje extra sobre el tiempo límite de las evaluaciones.
 */
public final class ReglasAccesibilidad {

    public static final String FUENTE_ATKINSON = "Atkinson Hyperlegible Next";
    public static final String FUENTE_LEXEND = "Lexend";
    public static final List<String> TIPOGRAFIAS = List.of(FUENTE_ATKINSON, FUENTE_LEXEND, "Arial", "Verdana", "Sistema");

    private ReglasAccesibilidad() {}

    public static ConfiguracionDto base() {
        return new ConfiguracionDto(false, 16, "Sistema", new BigDecimal("1.50"),
                false, false, false, false, false, 0);
    }

    /** Configuración automática: combina (máximos y OR) los ajustes de cada categoría elegida. */
    public static ConfiguracionDto porCategorias(Set<CategoriaAccesibilidad> categorias) {
        boolean contraste = false, teclado = false, lector = false, subtitulos = false, transcripcion = false, voz = false;
        int fuente = 16, extra = 0;
        BigDecimal espaciado = new BigDecimal("1.50");
        String tipografia = "Sistema";

        if (categorias.contains(CategoriaAccesibilidad.VISUAL)) {
            contraste = true; lector = true; voz = true; teclado = true;
            fuente = Math.max(fuente, 20);
            espaciado = espaciado.max(new BigDecimal("1.60"));
            tipografia = FUENTE_ATKINSON;
        }
        if (categorias.contains(CategoriaAccesibilidad.AUDITIVA)) {
            subtitulos = true; transcripcion = true;
        }
        if (categorias.contains(CategoriaAccesibilidad.MOTORA)) {
            teclado = true;
            extra = Math.max(extra, 50);
        }
        if (categorias.contains(CategoriaAccesibilidad.COGNITIVA)) {
            fuente = Math.max(fuente, 18);
            espaciado = espaciado.max(new BigDecimal("1.80"));
            if (tipografia.equals("Sistema")) tipografia = FUENTE_LEXEND;
            extra = Math.max(extra, 25);
        }
        return new ConfiguracionDto(contraste, fuente, tipografia, espaciado, teclado, lector,
                subtitulos, transcripcion, voz, extra);
    }

    /** Avisos de legibilidad (no bloquean el guardado). */
    public static List<String> advertencias(ConfiguracionDto c, Set<CategoriaAccesibilidad> categorias) {
        List<String> avisos = new ArrayList<>();
        if (c.tamanoFuente() < 14) {
            avisos.add("El texto es muy pequeño. Se recomienda 16 px o más.");
        }
        if (c.espaciadoLinea().compareTo(new BigDecimal("1.35")) < 0 && c.tamanoFuente() >= 24) {
            avisos.add("Con texto grande, un interlineado menor a 1.35 dificulta la lectura.");
        } else if (c.espaciadoLinea().compareTo(new BigDecimal("1.20")) < 0) {
            avisos.add("El interlineado es muy ajustado. Se recomienda 1.4 o más.");
        }
        if (categorias.contains(CategoriaAccesibilidad.VISUAL) && !c.altoContraste()) {
            avisos.add("Indicaste necesidades visuales: activa el alto contraste para leer con más facilidad.");
        }
        if (categorias.contains(CategoriaAccesibilidad.AUDITIVA) && !c.subtitulos()) {
            avisos.add("Indicaste necesidades auditivas: activa los subtítulos para los videos.");
        }
        if (categorias.contains(CategoriaAccesibilidad.MOTORA) && !c.navegacionTeclado()) {
            avisos.add("Indicaste necesidades motoras: activa la navegación por teclado.");
        }
        boolean necesitaLegible = categorias.contains(CategoriaAccesibilidad.VISUAL)
                || categorias.contains(CategoriaAccesibilidad.COGNITIVA);
        if (necesitaLegible && c.tipografia() != null
                && !c.tipografia().equals(FUENTE_ATKINSON) && !c.tipografia().equals(FUENTE_LEXEND)) {
            avisos.add("Las tipografías Atkinson Hyperlegible Next y Lexend están diseñadas para facilitar la lectura.");
        }
        return avisos;
    }

    /** Combinación accesible sugerida: nunca baja los ajustes elegidos, solo corrige los insuficientes. */
    public static ConfiguracionDto sugerir(ConfiguracionDto c, Set<CategoriaAccesibilidad> categorias) {
        ConfiguracionDto auto = porCategorias(categorias);
        boolean necesitaLegible = categorias.contains(CategoriaAccesibilidad.VISUAL)
                || categorias.contains(CategoriaAccesibilidad.COGNITIVA);
        return new ConfiguracionDto(
                c.altoContraste() || auto.altoContraste(),
                Math.max(Math.max(c.tamanoFuente(), auto.tamanoFuente()), 16),
                necesitaLegible ? auto.tipografia() : c.tipografia(),
                c.espaciadoLinea().max(auto.espaciadoLinea()).max(new BigDecimal("1.50")),
                c.navegacionTeclado() || auto.navegacionTeclado(),
                c.lectorPantalla() || auto.lectorPantalla(),
                c.subtitulos() || auto.subtitulos(),
                c.transcripcion() || auto.transcripcion(),
                c.textoAVoz() || auto.textoAVoz(),
                Math.max(c.tiempoAdicional(), auto.tiempoAdicional()));
    }
}
