package com.elearning.platform.controller;

import com.elearning.platform.dto.VarkDtos.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.VarkService;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** RF-004 Identificar estilo de aprendizaje (estudiante). */
@RestController
@RequestMapping("/vark")
@PreAuthorize("hasRole('ESTUDIANTE')")
@RequiredArgsConstructor
public class VarkController {

    private final VarkService vark;

    @GetMapping("/preguntas")
    public CuestionarioRespuesta preguntas() {
        return vark.cuestionario();
    }

    @GetMapping("/parcial")
    public ParcialRespuesta parcial(@AuthenticationPrincipal UsuarioPrincipal u) {
        return vark.parcial(u.id());
    }

    @PutMapping("/respuestas/{numero}")
    public ParcialRespuesta guardar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable int numero,
                                    @Valid @RequestBody RespuestaParcialRequest req) {
        return vark.guardarRespuesta(u.id(), numero, req.opcion());
    }

    @PostMapping("/enviar")
    public ResultadoVarkRespuesta enviar(@AuthenticationPrincipal UsuarioPrincipal u, HttpServletRequest http) {
        return vark.enviar(u.id(), IpUtil.de(http));
    }

    @GetMapping("/metodos")
    public MetodosRespuesta metodos(@AuthenticationPrincipal UsuarioPrincipal u) {
        return vark.metodos(u.id());
    }

    @PutMapping("/metodos")
    public MetodosRespuesta cambiarMetodos(@AuthenticationPrincipal UsuarioPrincipal u,
                                           @Valid @RequestBody MetodosRequest req, HttpServletRequest http) {
        return vark.cambiarMetodos(u.id(), req.principal(), req.secundario(), IpUtil.de(http));
    }
}
