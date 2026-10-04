package com.elearning.platform.controller;

import com.elearning.platform.dto.NotificacionDtos.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.NotificacionGestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** RF-011 Notificaciones. */
@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionGestionService servicio;

    @GetMapping
    public ListaNotificaciones listar(@AuthenticationPrincipal UsuarioPrincipal u,
                                      @RequestParam(defaultValue = "30") int limite) {
        return servicio.listar(u.id(), limite);
    }

    @PatchMapping("/{id}/leida")
    public NotificacionDto leida(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id) {
        return servicio.marcarLeida(u.id(), id);
    }

    @PostMapping("/{id}/posponer")
    public NotificacionDto posponer(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                    @Valid @RequestBody PosponerRequest req) {
        return servicio.posponer(u.id(), id, req.opcion());
    }

    @GetMapping("/preferencias")
    public PreferenciasDto preferencias(@AuthenticationPrincipal UsuarioPrincipal u) {
        return servicio.preferencias(u.id());
    }

    @PutMapping("/preferencias")
    public PreferenciasDto guardar(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody PreferenciasRequest req) {
        return servicio.guardarPreferencias(u.id(), req);
    }
}
