package com.elearning.platform.controller;

import com.elearning.platform.dto.GamificacionDtos.*;
import com.elearning.platform.repository.AdministradorRepository;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.AuditoriaService;
import com.elearning.platform.services.GamificacionService;
import com.elearning.platform.enums.ResultadoAuditoria;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF-009: el administrador edita reglas de puntos e insignias. */
@RestController
@RequestMapping("/admin/gamificacion")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class AdminGamificacionController {

    private final GamificacionService gamificacion;
    private final AdministradorRepository administradores;
    private final AuditoriaService auditoria;

    @GetMapping("/reglas")
    public List<ReglaRespuesta> reglas() {
        return gamificacion.listarReglas();
    }

    @PostMapping("/reglas")
    @ResponseStatus(HttpStatus.CREATED)
    public ReglaRespuesta crearRegla(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody ReglaRequest req,
                                     HttpServletRequest http) {
        ReglaRespuesta r = gamificacion.guardarRegla(null, req, administradores.getReferenceById(u.id()));
        auditoria.registrar(u.id(), "REGLA_GAMIFICACION_CREADA", "reglas/" + r.id(), ResultadoAuditoria.PERMITIDO, IpUtil.de(http));
        return r;
    }

    @PutMapping("/reglas/{id}")
    public ReglaRespuesta actualizarRegla(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                          @Valid @RequestBody ReglaRequest req, HttpServletRequest http) {
        ReglaRespuesta r = gamificacion.guardarRegla(id, req, administradores.getReferenceById(u.id()));
        auditoria.registrar(u.id(), "REGLA_GAMIFICACION_ACTUALIZADA", "reglas/" + id, ResultadoAuditoria.PERMITIDO, IpUtil.de(http));
        return r;
    }

    @GetMapping("/insignias")
    public List<InsigniaRespuesta> insignias() {
        return gamificacion.listarInsignias();
    }

    @PostMapping("/insignias")
    @ResponseStatus(HttpStatus.CREATED)
    public InsigniaRespuesta crearInsignia(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody InsigniaRequest req,
                                           HttpServletRequest http) {
        InsigniaRespuesta r = gamificacion.guardarInsignia(null, req);
        auditoria.registrar(u.id(), "INSIGNIA_CREADA", "insignias/" + r.id(), ResultadoAuditoria.PERMITIDO, IpUtil.de(http));
        return r;
    }

    @PutMapping("/insignias/{id}")
    public InsigniaRespuesta actualizarInsignia(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                                @Valid @RequestBody InsigniaRequest req, HttpServletRequest http) {
        InsigniaRespuesta r = gamificacion.guardarInsignia(id, req);
        auditoria.registrar(u.id(), "INSIGNIA_ACTUALIZADA", "insignias/" + id, ResultadoAuditoria.PERMITIDO, IpUtil.de(http));
        return r;
    }
}
