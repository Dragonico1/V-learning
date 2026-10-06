package com.elearning.platform.controller;

import com.elearning.platform.dto.TareaDtos.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.TareaService;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Tareas o actividades calificables: autoría y calificación (instructor) y entrega (estudiante). */
@RestController
@RequiredArgsConstructor
public class TareaController {

    private final TareaService servicio;

    // ----- Instructor -----
    @GetMapping("/instructor/modulos/{id}/tareas")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public List<TareaInstructor> listarInstructor(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return servicio.listarInstructor(id, u.id());
    }

    @PostMapping("/modulos/{id}/tareas")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public TareaInstructor crear(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                 @Valid @RequestBody TareaRequest req, HttpServletRequest http) {
        return servicio.crear(id, u.id(), req, IpUtil.de(http));
    }

    @PutMapping("/tareas/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public TareaInstructor actualizar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                      @Valid @RequestBody TareaRequest req, HttpServletRequest http) {
        return servicio.actualizar(id, u.id(), req, IpUtil.de(http));
    }

    @DeleteMapping("/tareas/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id, HttpServletRequest http) {
        servicio.eliminar(id, u.id(), IpUtil.de(http));
    }

    @GetMapping("/tareas/{id}/entregas")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public List<EntregaVista> entregas(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return servicio.entregasDe(id, u.id());
    }

    @PutMapping("/entregas/{id}/calificacion")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public EntregaVista calificar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                  @Valid @RequestBody CalificacionRequest req, HttpServletRequest http) {
        return servicio.calificar(id, u.id(), req, IpUtil.de(http));
    }

    // ----- Estudiante -----
    @GetMapping("/modulos/{id}/tareas")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public List<TareaEstudiante> listarEstudiante(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return servicio.listarEstudiante(u.id(), id);
    }

    @PutMapping("/tareas/{id}/entrega")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public TareaEstudiante entregar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                    @Valid @RequestBody EntregaRequest req, HttpServletRequest http) {
        return servicio.entregar(u.id(), id, req, IpUtil.de(http));
    }
}
