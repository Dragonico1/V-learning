package com.elearning.platform.util;

/** Relación de contraste WCAG 2.1 (AA exige 4.5:1 en texto normal, AAA 7:1). */
public final class ContrasteWcag {

    public static final double MINIMO_AA = 4.5;
    public static final double MINIMO_AAA = 7.0;

    private ContrasteWcag() {}

    public static double ratio(String colorA, String colorB) {
        double la = luminancia(colorA);
        double lb = luminancia(colorB);
        double claro = Math.max(la, lb);
        double oscuro = Math.min(la, lb);
        return (claro + 0.05) / (oscuro + 0.05);
    }

    private static double luminancia(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        double r = canal(Integer.parseInt(h.substring(0, 2), 16));
        double g = canal(Integer.parseInt(h.substring(2, 4), 16));
        double b = canal(Integer.parseInt(h.substring(4, 6), 16));
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double canal(int valor) {
        double c = valor / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
