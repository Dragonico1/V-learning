package com.elearning.platform.services;

import com.elearning.platform.dto.GamificacionDtos.ActividadRespuesta;
import com.elearning.platform.entity.HistorialPuntos;
import com.elearning.platform.repository.HistorialPuntosRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GamificacionActividadTest {

    @Test
    void etiquetasDeDiaEnEspanol() {
        assertEquals("L", GamificacionService.etiquetaDia(LocalDate.of(2026, 10, 19)));
        assertEquals("X", GamificacionService.etiquetaDia(LocalDate.of(2026, 10, 21)));
        assertEquals("D", GamificacionService.etiquetaDia(LocalDate.of(2026, 10, 25)));
    }

    @Test
    void sumaPuntosPorDiaYRellenaDiasSinActividad() {
        HistorialPuntosRepository repo = mock(HistorialPuntosRepository.class);
        LocalDateTime hoy = LocalDate.now().atTime(10, 0);
        HistorialPuntos a = new HistorialPuntos(1L, 50, "a");
        a.setFecha(hoy);
        HistorialPuntos b = new HistorialPuntos(1L, 100, "b");
        b.setFecha(hoy);
        HistorialPuntos c = new HistorialPuntos(1L, 30, "c");
        c.setFecha(hoy.minusDays(3));
        when(repo.desde(eq(1L), any())).thenReturn(List.of(a, b, c));

        GamificacionService svc = new GamificacionService(null, null, null, null, null, null, repo);
        ActividadRespuesta r = svc.actividad(1L, 7);

        assertEquals(7, r.dias().size());
        assertTrue(r.dias().get(6).esHoy());
        assertEquals(150, r.puntosHoy());
        assertEquals(30, r.dias().get(3).puntos());
        assertEquals(0, r.dias().get(0).puntos());
        assertEquals(180, r.totalPuntos());
    }

    @Test
    void limitaElRangoDeDias() {
        HistorialPuntosRepository repo = mock(HistorialPuntosRepository.class);
        when(repo.desde(any(), any())).thenReturn(List.of());
        GamificacionService svc = new GamificacionService(null, null, null, null, null, null, repo);
        assertEquals(1, svc.actividad(1L, 0).dias().size());
        assertEquals(31, svc.actividad(1L, 500).dias().size());
    }
}
