package com.elearning.platform.controller;

import com.elearning.platform.dto.InformeDtos.*;
import com.elearning.platform.enums.FormatoInforme;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.InformeService;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** RF-012 Informes (admin / instructor) y RF-006 exportación del progreso del estudiante. */
@RestController
@RequiredArgsConstructor
public class InformeController {

    private final InformeService informes;

    @PostMapping("/informes/vista")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public VistaInforme vista(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody InformeRequest req) {
        return informes.vista(u, req);
    }

    @PostMapping("/informes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<byte[]> generar(@AuthenticationPrincipal UsuarioPrincipal u, @Valid @RequestBody InformeRequest req,
                                          HttpServletRequest http) {
        return descarga(informes.generar(u, req, IpUtil.de(http)));
    }

    @GetMapping("/informes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public List<InformeHistorial> historial(@AuthenticationPrincipal UsuarioPrincipal u) {
        return informes.historial(u.id());
    }

    @GetMapping("/progreso/exportar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<byte[]> exportar(@AuthenticationPrincipal UsuarioPrincipal u, @RequestParam FormatoInforme formato,
                                           HttpServletRequest http) {
        return descarga(informes.exportarProgresoPropio(u, formato, IpUtil.de(http)));
    }

    private static ResponseEntity<byte[]> descarga(ArchivoGenerado a) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(a.nombre(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(a.tipoContenido()))
                .body(a.contenido());
    }
}
