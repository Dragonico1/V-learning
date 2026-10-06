package com.elearning.platform.controller;

import com.elearning.platform.dto.AuthDtos.MensajeRespuesta;
import com.elearning.platform.dto.ChatDtos.MensajeDto;
import com.elearning.platform.dto.ChatDtos.MensajeReportado;
import com.elearning.platform.dto.ChatDtos.MensajeRequest;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.ChatService;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF-010 Comunidad del curso (REST). El tiempo real va por /ws/cursos/{id}. */
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chat;

    @GetMapping("/cursos/{id}/mensajes")
    public List<MensajeDto> historial(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                      @RequestParam(defaultValue = "50") int limite) {
        return chat.historial(u, id, limite);
    }

    @PostMapping("/cursos/{id}/mensajes")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeDto enviar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                             @Valid @RequestBody MensajeRequest req, HttpServletRequest http) {
        return chat.enviar(u, id, req.contenido(), IpUtil.de(http));
    }

    @PostMapping("/mensajes/{id}/reportar")
    public MensajeRespuesta reportar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                     HttpServletRequest http) {
        chat.reportar(u, id, IpUtil.de(http));
        return new MensajeRespuesta("Gracias. Avisamos al instructor del curso para que revise el mensaje.");
    }

    @PostMapping("/mensajes/{id}/ocultar")
    public MensajeRespuesta ocultar(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                    HttpServletRequest http) {
        chat.ocultar(u, id, IpUtil.de(http));
        return new MensajeRespuesta("El mensaje se eliminó.");
    }

    @PostMapping("/mensajes/{id}/mantener")
    public MensajeRespuesta mantener(@AuthenticationPrincipal UsuarioPrincipal u, @PathVariable Long id,
                                     HttpServletRequest http) {
        chat.mantener(u, id, IpUtil.de(http));
        return new MensajeRespuesta("Listo: el mensaje se mantiene en la conversación.");
    }

    /** Sección «Mensajes reportados» del instructor: pendientes en todos sus cursos. */
    @GetMapping("/moderacion/reportes")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public List<MensajeReportado> reportados(@AuthenticationPrincipal UsuarioPrincipal u) {
        return chat.reportados(u);
    }
}
