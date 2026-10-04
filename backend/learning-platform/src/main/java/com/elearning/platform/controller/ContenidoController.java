package com.elearning.platform.controller;

import com.elearning.platform.dto.ContenidoDtos.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.ContenidoService;
import com.elearning.platform.services.ProgresoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Lección, apoyo cognitivo y registro de avance. */
@RestController
@RequiredArgsConstructor
public class ContenidoController {

    private final ContenidoService contenidos;
    private final ProgresoService progreso;

    @GetMapping("/contenidos/{id}")
    @PreAuthorize("hasAnyRole('ESTUDIANTE','INSTRUCTOR','ADMINISTRADOR')")
    public ContenidoVista ver(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return contenidos.ver(u, id);
    }

    @GetMapping("/contenidos/{id}/apoyo-cognitivo")
    @PreAuthorize("hasAnyRole('ESTUDIANTE','INSTRUCTOR')")
    public ApoyoCognitivoRespuesta apoyo(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return contenidos.apoyoCognitivo(u, id);
    }

    @PostMapping("/contenidos/{id}/progreso")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ProgresoRespuesta registrar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                       @Valid @RequestBody ProgresoRequest req) {
        return progreso.registrar(u.id(), id, req);
    }
}
