package com.elearning.platform.util;

import com.elearning.platform.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaPasswordTest {

    @Test
    void aceptaUnaContrasenaFuerte() {
        assertDoesNotThrow(() -> PoliticaPassword.validar("Vlearning#2026", "ana@tdea.edu.co"));
    }

    @Test
    void rechazaCortaSinSimboloOSinNumero() {
        assertThrows(ApiException.class, () -> PoliticaPassword.validar("Ab#1", "ana@tdea.edu.co"));
        assertThrows(ApiException.class, () -> PoliticaPassword.validar("Vlearning2026", "ana@tdea.edu.co"));
        assertThrows(ApiException.class, () -> PoliticaPassword.validar("Vlearning#abc", "ana@tdea.edu.co"));
        assertThrows(ApiException.class, () -> PoliticaPassword.validar(null, "ana@tdea.edu.co"));
    }

    @Test
    void rechazaContenerLaPrimeraParteDelCorreo() {
        assertThrows(ApiException.class, () -> PoliticaPassword.validar("Camila.rojas#2026", "camila.rojas@tdea.edu.co"));
    }

    @Test
    void laContrasenaTemporalGeneradaCumpleLaPolitica() {
        for (int i = 0; i < 50; i++) {
            String t = PoliticaPassword.generarTemporal();
            assertDoesNotThrow(() -> PoliticaPassword.validar(t, "ana@tdea.edu.co"));
        }
    }
}
