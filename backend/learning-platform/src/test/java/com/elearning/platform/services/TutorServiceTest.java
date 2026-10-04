package com.elearning.platform.services;

import com.elearning.platform.enums.TipoRespuestaTutor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TutorServiceTest {

    @Test
    void clasificaLaPreguntaPorSuIntencion() {
        assertEquals(TipoRespuestaTutor.EJEMPLO, TutorService.clasificar("Dame un ejemplo de variable"));
        assertEquals(TipoRespuestaTutor.RESUMEN, TutorService.clasificar("Resúmeme la lección"));
        assertEquals(TipoRespuestaTutor.ACTIVIDAD, TutorService.clasificar("Quiero un ejercicio para practicar"));
        assertEquals(TipoRespuestaTutor.EXPLICACION, TutorService.clasificar("¿Qué es un ciclo?"));
    }

    @Test
    void quitaMarkdownParaLectoresDePantalla() {
        String limpio = TutorService.limpiar("## Título\n- **Uno**\n- `dos`\n\n\n\nFin");
        assertFalse(limpio.contains("#"));
        assertFalse(limpio.contains("*"));
        assertFalse(limpio.contains("`"));
        assertTrue(limpio.contains("Uno"));
        assertTrue(limpio.contains("dos"));
        assertFalse(limpio.contains("\n\n\n"));
    }

    @Test
    void textoNuloDevuelveVacio() {
        assertEquals("", TutorService.limpiar(null));
    }
}
