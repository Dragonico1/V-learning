package com.elearning.platform.services;

import com.elearning.platform.dto.AccesibilidadDtos.ConfiguracionDto;
import com.elearning.platform.enums.CategoriaAccesibilidad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReglasAccesibilidadTest {

    @Test
    void lasCategoriasMantienenLaTipografiaBase() {
        assertEquals(ReglasAccesibilidad.FUENTE_BASE,
                ReglasAccesibilidad.porCategorias(EnumSet.of(CategoriaAccesibilidad.VISUAL)).tipografia());
        assertEquals(ReglasAccesibilidad.FUENTE_BASE,
                ReglasAccesibilidad.porCategorias(EnumSet.of(CategoriaAccesibilidad.COGNITIVA)).tipografia());
    }

    @Test
    void laSugerenciaRespetaLaFuenteElegidaManualmente() {
        ConfiguracionDto config = new ConfiguracionDto(false, 16, "Lexend", new BigDecimal("1.50"),
                false, false, false, false, false, 0);

        assertEquals("Lexend",
                ReglasAccesibilidad.sugerir(config, EnumSet.of(CategoriaAccesibilidad.VISUAL)).tipografia());
    }

    @Test
    void laSugerenciaExplicitaProponeUnaFuenteDeLectura() {
        ConfiguracionDto config = ReglasAccesibilidad.base();

        assertEquals(ReglasAccesibilidad.FUENTE_ATKINSON,
                ReglasAccesibilidad.sugerir(config, EnumSet.of(CategoriaAccesibilidad.VISUAL)).tipografia());
        assertEquals(ReglasAccesibilidad.FUENTE_LEXEND,
                ReglasAccesibilidad.sugerir(config, EnumSet.of(CategoriaAccesibilidad.COGNITIVA)).tipografia());
    }
}
