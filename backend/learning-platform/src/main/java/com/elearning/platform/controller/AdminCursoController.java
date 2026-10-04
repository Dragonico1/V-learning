package com.elearning.platform.controller;

import com.elearning.platform.dto.CursoDtos.CursoResumen;
import com.elearning.platform.dto.CursoDtos.InscribirRequest;
import com.elearning.platform.dto.CursoDtos.InscripcionResultado;
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

/** El administrador inscribe estudiantes en cualquier curso publicado. */
@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class AdminCursoController {

    private final CursoService cursos;

    @GetMapping("/admin/cursos")
    public List<CursoResumen> todos() {
        return cursos.todos();
    }

    @PostMapping("/admin/cursos/{id}/inscripciones")
    public InscripcionResultado inscribir(@AuthenticationPrincipal UsuarioPrincipal admin, @PathVariable Long id,
                                          @Valid @RequestBody InscribirRequest req, HttpServletRequest http) {
        return cursos.inscribirEstudiantes(id, req.correos(), admin.id(), true, IpUtil.de(http));
    }
}
