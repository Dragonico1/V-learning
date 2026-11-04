package com.elearning.platform.config;

import com.elearning.platform.enums.FormatoContenido;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursosDemoSeederTest {

    @Test
    void creaCincoCursosConModulosContenidosYEvaluaciones() {
        var cursos = CursosDemoSeeder.cursosDemo();

        assertEquals(5, cursos.size());
        for (var curso : cursos) {
            assertFalse(curso.titulo().isBlank());
            assertEquals(3, curso.modulos().size(), "Cada curso debe tener 3 módulos");

            long quizzes = curso.modulos().stream().filter(m -> m.evaluacion() != null).count();
            assertEquals(1, quizzes, "Cada curso debe tener un quiz");

            for (var modulo : curso.modulos()) {
                assertFalse(modulo.contenidos().isEmpty(), "Cada módulo debe tener contenidos");
                if (modulo.evaluacion() != null) {
                    assertEquals(3, modulo.evaluacion().preguntas().size());
                }
                for (var contenido : modulo.contenidos()) {
                    assertFalse(contenido.titulo().isBlank());
                    assertTrue(contenido.minutos() > 0);
                    if (contenido.formato() == FormatoContenido.VIDEO) {
                        assertNotNull(contenido.url());
                        assertNotNull(contenido.subtituloUrl(), "El video debe tener subtítulos");
                        assertNotNull(contenido.transcripcion(), "El video debe tener transcripción");
                    }
                    if (contenido.formato() == FormatoContenido.PODCAST) {
                        assertNotNull(contenido.transcripcion(), "El podcast debe tener transcripción");
                    }
                    if (contenido.formato() == FormatoContenido.LECTURA) {
                        assertNotNull(contenido.cuerpo(), "La lectura debe tener texto");
                    }
                }
            }
        }
    }
}
