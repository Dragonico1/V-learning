package com.elearning.platform.controller;

import com.elearning.platform.dto.CursoDtos.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.CursoService;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF-005: catálogo, mis cursos, detalle ordenado por método e inscripción (estudiante). */
@RestController
@PreAuthorize("hasRole('ESTUDIANTE')")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService cursos;

    @GetMapping("/cursos")
    public List<CursoResumen> catalogo(@AuthenticationPrincipal UsuarioPrincipal u) {
        return cursos.catalogo(u.id());
    }

    @GetMapping("/mis-cursos")
    public List<CursoResumen> misCursos(@AuthenticationPrincipal UsuarioPrincipal u) {
        return cursos.misCursos(u.id());
    }

    @GetMapping("/cursos/{id}")
    public CursoDetalle detalle(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return cursos.detalle(id, u.id());
    }

    @PostMapping("/cursos/{id}/inscribirme")
    public CursoResumen inscribirme(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                    HttpServletRequest http) {
        return cursos.inscribirme(id, u.id(), IpUtil.de(http));
    }
}
