package com.elearning.platform.controller;

import com.elearning.platform.dto.EvaluacionDtos.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.EvaluacionService;
import com.elearning.platform.services.InstructorEvaluacionService;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF-007 / RF-008: evaluaciones para el estudiante y autoría para el instructor. */
@RestController
@RequiredArgsConstructor
public class EvaluacionController {

    private final EvaluacionService estudiante;
    private final InstructorEvaluacionService instructor;

    // ----- Estudiante -----
    @GetMapping("/modulos/{id}/evaluaciones")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public List<EvaluacionEstudiante> listar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return estudiante.listarPorModulo(u.id(), id);
    }

    @PostMapping("/evaluaciones/{id}/intentos")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public IntentoVista iniciar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id, HttpServletRequest http) {
        return estudiante.iniciarIntento(u.id(), id, IpUtil.de(http));
    }

    @GetMapping("/evaluaciones/{id}/historial")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public List<IntentoHistorial> historial(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return estudiante.historial(u.id(), id);
    }

    @PutMapping("/intentos/{id}/respuestas/{preguntaId}")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public GuardadoRespuesta guardar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                     @PathVariable Long preguntaId, @Valid @RequestBody RespuestaRequest req) {
        return estudiante.guardarRespuesta(u.id(), id, preguntaId, req.valor());
    }

    @PostMapping("/intentos/{id}/finalizar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResultadoIntento finalizar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id, HttpServletRequest http) {
        return estudiante.finalizar(u.id(), id, IpUtil.de(http));
    }

    @GetMapping("/intentos/{id}/resultado")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResultadoIntento resultado(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return estudiante.resultado(u.id(), id);
    }

    // ----- Instructor -----
    @PostMapping("/modulos/{id}/evaluaciones")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluacionCreada crear(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                  @Valid @RequestBody EvaluacionRequest req, HttpServletRequest http) {
        return instructor.crear(id, u.id(), req, IpUtil.de(http));
    }

    @PutMapping("/evaluaciones/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public EvaluacionCreada actualizar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                       @Valid @RequestBody EvaluacionRequest req, HttpServletRequest http) {
        return instructor.actualizar(id, u.id(), req, IpUtil.de(http));
    }

    @DeleteMapping("/evaluaciones/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id, HttpServletRequest http) {
        instructor.eliminar(id, u.id(), IpUtil.de(http));
    }

    @PostMapping("/evaluaciones/{id}/preguntas")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public PreguntaCreada agregarPregunta(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                          @Valid @RequestBody PreguntaRequest req, HttpServletRequest http) {
        return instructor.agregarPregunta(id, u.id(), req, IpUtil.de(http));
    }
}
