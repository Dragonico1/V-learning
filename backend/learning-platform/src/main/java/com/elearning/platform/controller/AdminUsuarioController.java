package com.elearning.platform.controller;

import com.elearning.platform.dto.UsuarioDtos.*;
import com.elearning.platform.enums.EstadoUsuario;
import com.elearning.platform.enums.RolUsuario;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.UsuarioService;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** RF-001 Registrar cuenta: gestión de usuarios (solo administrador). */
@RestController
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final UsuarioService usuarios;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CredencialesRespuesta crear(@AuthenticationPrincipal UsuarioPrincipal admin,
                                       @Valid @RequestBody UsuarioCrearRequest req, HttpServletRequest http) {
        return usuarios.crear(req, admin.id(), IpUtil.de(http));
    }

    @GetMapping
    public Pagina<UsuarioRespuesta> listar(@RequestParam(required = false) RolUsuario rol,
                                           @RequestParam(required = false) EstadoUsuario estado,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int pagina,
                                           @RequestParam(defaultValue = "20") int tamano) {
        return usuarios.listar(rol, estado, q, pagina, tamano);
    }

    @PatchMapping("/{id}/estado")
    public UsuarioRespuesta cambiarEstado(@AuthenticationPrincipal UsuarioPrincipal admin, @PathVariable Long id,
                                          @Valid @RequestBody EstadoRequest req, HttpServletRequest http) {
        return usuarios.cambiarEstado(id, req.estado(), admin.id(), IpUtil.de(http));
    }

    @PostMapping("/{id}/reenviar-credenciales")
    public CredencialesRespuesta reenviar(@AuthenticationPrincipal UsuarioPrincipal admin, @PathVariable Long id,
                                          HttpServletRequest http) {
        return usuarios.reenviarCredenciales(id, admin.id(), IpUtil.de(http));
    }
}
