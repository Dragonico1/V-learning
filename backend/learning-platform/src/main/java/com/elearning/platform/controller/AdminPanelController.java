package com.elearning.platform.controller;

import com.elearning.platform.dto.AdminDtos.*;
import com.elearning.platform.enums.ResultadoAuditoria;
import com.elearning.platform.services.AdminPanelService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Panel del administrador: resumen y consulta de auditoría. */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class AdminPanelController {

    private final AdminPanelService panel;

    @GetMapping("/dashboard")
    public AdminDashboard dashboard() {
        return panel.dashboard();
    }

    @GetMapping("/auditoria")
    public PaginaAuditoria auditoria(@RequestParam(required = false) Long usuarioId,
                                     @RequestParam(required = false) String accion,
                                     @RequestParam(required = false) ResultadoAuditoria resultado,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                     @RequestParam(defaultValue = "0") int pagina,
                                     @RequestParam(defaultValue = "25") int tamano) {
        return panel.auditoria(usuarioId, accion, resultado, desde, hasta, pagina, tamano);
    }
}
