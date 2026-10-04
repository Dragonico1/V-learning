package com.elearning.platform.controller;

import com.elearning.platform.dto.DashboardDtos.DashboardRespuesta;
import com.elearning.platform.dto.GamificacionDtos.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.DashboardService;
import com.elearning.platform.services.GamificacionService;
import com.elearning.platform.services.RankingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** RF-006 dashboard y RF-009 logros, privacidad y ranking (estudiante). */
@RestController
@PreAuthorize("hasRole('ESTUDIANTE')")
@RequiredArgsConstructor
public class LogrosController {

    private final DashboardService dashboard;
    private final GamificacionService gamificacion;
    private final RankingService ranking;

    @GetMapping("/progreso")
    public DashboardRespuesta progreso(@AuthenticationPrincipal UsuarioPrincipal u) {
        return dashboard.dashboard(u.id());
    }

    @GetMapping("/logros")
    public LogrosRespuesta logros(@AuthenticationPrincipal UsuarioPrincipal u) {
        return gamificacion.logros(u.id());
    }

    @PutMapping("/logros/privacidad")
    public LogrosRespuesta privacidad(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody PrivacidadRequest req) {
        return gamificacion.cambiarPrivacidad(u.id(), req.mostrarEnRanking());
    }

    @GetMapping("/cursos/{id}/ranking")
    public RankingRespuesta ranking(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return ranking.obtener(id, u.id());
    }
}
