package com.elearning.platform.controller;

import com.elearning.platform.dto.TutorDtos.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.TutorService;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF-024 Tutor de IA (solo estudiantes). */
@RestController
@RequestMapping("/tutor")
@PreAuthorize("hasRole('ESTUDIANTE')")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutor;

    @PostMapping("/consultas")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaTutorDto consultar(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody ConsultaRequest req,
                                       HttpServletRequest http) {
        return tutor.consultar(u, req.pregunta(), req.contenidoId(), IpUtil.de(http));
    }

    @PostMapping("/respuestas/{id}/util")
    public RespuestaTutorDto util(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                  @Valid @RequestBody UtilRequest req) {
        return tutor.marcarUtil(u.id(), id, req.util());
    }

    @GetMapping("/historial")
    public List<RespuestaTutorDto> historial(@AuthenticationPrincipal UsuarioPrincipal u,
                                             @RequestParam(defaultValue = "30") int limite) {
        return tutor.historial(u.id(), limite);
    }
}
