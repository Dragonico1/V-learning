package com.elearning.platform.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContrasteWcagTest {

    @Test
    void negroSobreBlancoEsElMaximo() {
        assertEquals(21.0, ContrasteWcag.ratio("#000000", "#FFFFFF"), 0.01);
    }

    @Test
    void esSimetrico() {
        assertEquals(ContrasteWcag.ratio("#5B21B6", "#FFFFFF"), ContrasteWcag.ratio("#FFFFFF", "#5B21B6"), 1e-9);
    }

    @Test
    void colorIgualDaUno() {
        assertEquals(1.0, ContrasteWcag.ratio("#777777", "#777777"), 1e-9);
    }

    @Test
    void elMoradoPrimarioCumpleAASobreBlanco() {
        assertTrue(ContrasteWcag.ratio("#5B21B6", "#FFFFFF") >= ContrasteWcag.MINIMO_AA);
    }

    @Test
    void amarilloSobreBlancoNoCumpleAA() {
        assertTrue(ContrasteWcag.ratio("#FFFF00", "#FFFFFF") < ContrasteWcag.MINIMO_AA);
    }
}
