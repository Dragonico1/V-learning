package com.elearning.platform.controller;

import com.elearning.platform.dto.CursoDtos.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.CursoService;
import com.elearning.platform.services.InstructorCursoService;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestión de cursos, módulos, contenidos, recursos accesibles y publicación (instructor). */
@RestController
@PreAuthorize("hasRole('INSTRUCTOR')")
@RequiredArgsConstructor
public class InstructorCursoController {

    private final InstructorCursoService servicio;
    private final CursoService cursos;

    @GetMapping("/instructor/cursos")
    public List<CursoResumen> listar(@AuthenticationPrincipal UsuarioPrincipal u) {
        return servicio.listar(u.id());
    }

    @PostMapping("/instructor/cursos")
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResumen crear(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody CursoRequest req,
                              HttpServletRequest http) {
        return servicio.crear(u.id(), req, IpUtil.de(http));
    }

    @GetMapping("/instructor/cursos/{id}")
    public CursoDetalle detalle(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return servicio.detalle(id, u.id());
    }

    @PutMapping("/instructor/cursos/{id}")
    public CursoResumen actualizar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                   @Valid @RequestBody CursoRequest req, HttpServletRequest http) {
        return servicio.actualizar(id, u.id(), req, IpUtil.de(http));
    }

    @PostMapping("/instructor/cursos/{id}/publicar")
    public CursoResumen publicarCurso(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                      HttpServletRequest http) {
        return servicio.publicarCurso(id, u.id(), IpUtil.de(http));
    }

    @PostMapping("/instructor/cursos/{id}/archivar")
    public CursoResumen archivarCurso(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                      HttpServletRequest http) {
        return servicio.archivarCurso(id, u.id(), IpUtil.de(http));
    }

    @PostMapping("/instructor/cursos/{id}/inscripciones")
    public InscripcionResultado inscribir(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                          @Valid @RequestBody InscribirRequest req, HttpServletRequest http) {
        return cursos.inscribirEstudiantes(id, req.correos(), u.id(), false, IpUtil.de(http));
    }

    @PostMapping("/instructor/cursos/{id}/modulos")
    @ResponseStatus(HttpStatus.CREATED)
    public ModuloItem crearModulo(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                  @Valid @RequestBody ModuloRequest req, HttpServletRequest http) {
        return servicio.crearModulo(id, u.id(), req, IpUtil.de(http));
    }

    @PutMapping("/modulos/{id}")
    public ModuloItem actualizarModulo(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                       @Valid @RequestBody ModuloRequest req, HttpServletRequest http) {
        return servicio.actualizarModulo(id, u.id(), req, IpUtil.de(http));
    }

    @PostMapping("/modulos/{id}/contenidos")
    @ResponseStatus(HttpStatus.CREATED)
    public ContenidoItem crearContenido(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                        @Valid @RequestBody ContenidoRequest req, HttpServletRequest http) {
        return servicio.crearContenido(id, u.id(), req, IpUtil.de(http));
    }

    @PutMapping("/contenidos/{id}")
    public ContenidoItem actualizarContenido(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                             @Valid @RequestBody ContenidoRequest req, HttpServletRequest http) {
        return servicio.actualizarContenido(id, u.id(), req, IpUtil.de(http));
    }

    @GetMapping("/contenidos/{id}/recursos")
    public List<RecursoRespuesta> recursos(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return servicio.listarRecursos(id, u.id());
    }

    @PostMapping("/contenidos/{id}/recursos")
    @ResponseStatus(HttpStatus.CREATED)
    public RecursoRespuesta agregarRecurso(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                           @Valid @RequestBody RecursoRequest req, HttpServletRequest http) {
        return servicio.agregarRecurso(id, u.id(), req, IpUtil.de(http));
    }

    @DeleteMapping("/recursos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarRecurso(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                HttpServletRequest http) {
        servicio.eliminarRecurso(id, u.id(), IpUtil.de(http));
    }

    @GetMapping("/contenidos/{id}/conformidad")
    public ConformidadRespuesta conformidad(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return servicio.conformidad(id, u.id());
    }

    @PostMapping("/contenidos/{id}/publicar")
    public PublicacionRespuesta publicar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                         HttpServletRequest http) {
        return servicio.publicarContenido(id, u.id(), IpUtil.de(http));
    }

    @PostMapping("/contenidos/{id}/despublicar")
    public PublicacionRespuesta despublicar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                            HttpServletRequest http) {
        return servicio.despublicarContenido(id, u.id(), IpUtil.de(http));
    }
}
