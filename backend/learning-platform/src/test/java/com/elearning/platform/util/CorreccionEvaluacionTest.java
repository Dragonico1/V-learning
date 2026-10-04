package com.elearning.platform.util;

import com.elearning.platform.enums.Clasificacion;
import com.elearning.platform.enums.TipoPregunta;
import com.elearning.platform.util.CorreccionEvaluacion.OpcionDato;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorreccionEvaluacionTest {

    private static final List<OpcionDato> OPCIONES = List.of(
            new OpcionDato(1L, "Uno", true), new OpcionDato(2L, "Dos", false),
            new OpcionDato(3L, "Tres", true), new OpcionDato(4L, "Cuatro", false));

    @Test
    void seleccionUnicaCorrectaDaPuntajeCompleto() {
        var r = CorreccionEvaluacion.corregir(TipoPregunta.SELECCION_UNICA, new BigDecimal("5"), OPCIONES, "1");
        assertEquals(new BigDecimal("5"), r.obtenido());
        assertTrue(r.completa());
    }

    @Test
    void seleccionUnicaIncorrectaOVaciaDaCero() {
        assertEquals(0, CorreccionEvaluacion.corregir(TipoPregunta.SELECCION_UNICA, new BigDecimal("5"), OPCIONES, "2").obtenido().signum());
        assertFalse(CorreccionEvaluacion.corregir(TipoPregunta.SELECCION_UNICA, new BigDecimal("5"), OPCIONES, "  ").completa());
        assertFalse(CorreccionEvaluacion.corregir(TipoPregunta.SELECCION_UNICA, new BigDecimal("5"), OPCIONES, null).completa());
    }

    @Test
    void seleccionMultipleEsProporcionalYDescuentaErrores() {
        var parcial = CorreccionEvaluacion.corregir(TipoPregunta.SELECCION_MULTIPLE, new BigDecimal("10"), OPCIONES, "1");
        assertEquals(new BigDecimal("5.00"), parcial.obtenido());
        assertFalse(parcial.completa());

        var completa = CorreccionEvaluacion.corregir(TipoPregunta.SELECCION_MULTIPLE, new BigDecimal("10"), OPCIONES, "1,3");
        assertEquals(new BigDecimal("10.00"), completa.obtenido());
        assertTrue(completa.completa());

        var conError = CorreccionEvaluacion.corregir(TipoPregunta.SELECCION_MULTIPLE, new BigDecimal("10"), OPCIONES, "1,3,2");
        assertEquals(new BigDecimal("5.00"), conError.obtenido());
        assertFalse(conError.completa());

        var soloErrores = CorreccionEvaluacion.corregir(TipoPregunta.SELECCION_MULTIPLE, new BigDecimal("10"), OPCIONES, "2,4");
        assertEquals(0, soloErrores.obtenido().signum());
    }

    @Test
    void preguntaAbiertaIgnoraTildesMayusculasYPuntuacion() {
        List<OpcionDato> aceptadas = List.of(new OpcionDato(9L, "Variable", true));
        assertTrue(CorreccionEvaluacion.corregir(TipoPregunta.ABIERTA, BigDecimal.ONE, aceptadas, "  ¡VARIABLE! ").completa());
        assertFalse(CorreccionEvaluacion.corregir(TipoPregunta.ABIERTA, BigDecimal.ONE, aceptadas, "constante").completa());
    }

    @Test
    void clasificaPorUmbrales() {
        assertEquals(Clasificacion.EXCELENTE, CorreccionEvaluacion.clasificar(new BigDecimal("85")));
        assertEquals(Clasificacion.ACEPTABLE, CorreccionEvaluacion.clasificar(new BigDecimal("84.99")));
        assertEquals(Clasificacion.ACEPTABLE, CorreccionEvaluacion.clasificar(new BigDecimal("60")));
        assertEquals(Clasificacion.INSUFICIENTE, CorreccionEvaluacion.clasificar(new BigDecimal("59.99")));
    }

    @Test
    void porcentajeSeCalculaConDosDecimalesYNoDivideEntreCero() {
        assertEquals(new BigDecimal("33.33"), CorreccionEvaluacion.porcentaje(BigDecimal.ONE, new BigDecimal("3")));
        assertEquals(0, CorreccionEvaluacion.porcentaje(BigDecimal.ONE, BigDecimal.ZERO).signum());
    }

    @Test
    void normalizarQuitaTildesSignosYEspacios() {
        assertEquals("que tal", CorreccionEvaluacion.normalizar("¿Qué   Tal?"));
    }
}
