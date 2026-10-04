package com.elearning.platform.util;

import java.util.List;

/** Contenido neutro de un informe: se renderiza igual a PDF o Excel. */
public record TablaInforme(String titulo, List<String> contexto, List<String> columnas, List<List<String>> filas,
                           List<String> notas) {

    public boolean sinDatos() {
        return filas == null || filas.isEmpty();
    }
}
