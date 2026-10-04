package com.elearning.platform.util;

import com.elearning.platform.exception.ApiException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reglas de robustez de contraseña (RF-003) y generador de contraseñas temporales (RF-001). */
public final class PoliticaPassword {

    public static final int MINIMO = 10;
    public static final int MAXIMO = 72; // límite de BCrypt

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final String MAYUS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUS = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITOS = "23456789";
    private static final String SIMBOLOS = "#$%&*+?";

    private PoliticaPassword() {}

    public static void validar(String password, String correo) {
        List<String> faltas = new ArrayList<>();
        if (password == null || password.length() < MINIMO) faltas.add("tener al menos " + MINIMO + " caracteres");
        if (password != null && password.length() > MAXIMO) faltas.add("tener máximo " + MAXIMO + " caracteres");
        if (password == null || !password.matches(".*[a-z].*")) faltas.add("incluir una letra minúscula");
        if (password == null || !password.matches(".*[A-Z].*")) faltas.add("incluir una letra mayúscula");
        if (password == null || !password.matches(".*\\d.*")) faltas.add("incluir un número");
        if (password == null || !password.matches(".*[^A-Za-z0-9].*")) faltas.add("incluir un símbolo (por ejemplo # o !)");
        if (password != null && correo != null && correo.contains("@")) {
            String local = correo.substring(0, correo.indexOf('@')).toLowerCase();
            if (local.length() >= 4 && password.toLowerCase().contains(local)) {
                faltas.add("no contener la primera parte de tu correo");
            }
        }
        if (!faltas.isEmpty()) {
            throw ApiException.solicitudInvalida("PASSWORD_DEBIL", "La contraseña debe " + String.join(", ", faltas) + ".");
        }
    }

    public static String generarTemporal() {
        List<Character> caracteres = new ArrayList<>();
        caracteres.add(tomar(MAYUS));
        caracteres.add(tomar(MINUS));
        caracteres.add(tomar(DIGITOS));
        caracteres.add(tomar(SIMBOLOS));
        String todos = MAYUS + MINUS + DIGITOS + SIMBOLOS;
        while (caracteres.size() < 12) caracteres.add(tomar(todos));
        Collections.shuffle(caracteres, ALEATORIO);
        StringBuilder sb = new StringBuilder();
        caracteres.forEach(sb::append);
        return sb.toString();
    }

    private static char tomar(String alfabeto) {
        return alfabeto.charAt(ALEATORIO.nextInt(alfabeto.length()));
    }
}
