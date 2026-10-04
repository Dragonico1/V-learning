package com.elearning.platform.controller;

import com.elearning.platform.dto.AccesibilidadDtos.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.AccesibilidadService;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF-014 Perfil de accesibilidad y RF-016 Visualización de la interfaz (estudiante). */
@RestController
@RequestMapping("/accesibilidad")
@PreAuthorize("hasRole('ESTUDIANTE')")
@RequiredArgsConstructor
public class AccesibilidadController {

    private final AccesibilidadService accesibilidad;

    @GetMapping("/perfil")
    public PerfilAccesibilidadRespuesta perfil(@AuthenticationPrincipal UsuarioPrincipal u) {
        return accesibilidad.obtener(u.id());
    }

    @PutMapping("/perfil")
    public PerfilAccesibilidadRespuesta guardarPerfil(@AuthenticationPrincipal UsuarioPrincipal u,
                                                      @Valid @RequestBody PerfilAccesibilidadRequest req,
                                                      HttpServletRequest http) {
        return accesibilidad.guardarCategorias(u.id(), req.categorias(), IpUtil.de(http));
    }

    @PostMapping("/previsualizar")
    public PrevisualizacionRespuesta previsualizar(@Valid @RequestBody PerfilAccesibilidadRequest req) {
        return accesibilidad.previsualizar(req.categorias());
    }

    @GetMapping("/configuracion")
    public ConfiguracionDto configuracion(@AuthenticationPrincipal UsuarioPrincipal u) {
        return accesibilidad.configuracionDe(u.id());
    }

    @PutMapping("/configuracion")
    public ConfiguracionRespuesta guardarConfiguracion(@AuthenticationPrincipal UsuarioPrincipal u,
                                                       @Valid @RequestBody ConfiguracionDto dto,
                                                       HttpServletRequest http) {
        return accesibilidad.guardarConfiguracion(u.id(), dto, IpUtil.de(http));
    }

    @PostMapping("/restablecer")
    public ConfiguracionDto restablecer(@AuthenticationPrincipal UsuarioPrincipal u, HttpServletRequest http) {
        return accesibilidad.restablecer(u.id(), IpUtil.de(http));
    }

    @GetMapping("/temas")
    public List<TemaRespuesta> temas() {
        return accesibilidad.temas();
    }
}
