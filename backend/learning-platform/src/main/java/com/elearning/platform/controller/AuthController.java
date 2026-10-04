package com.elearning.platform.controller;

import com.elearning.platform.dto.AuthDtos.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.services.AuthService;
import com.elearning.platform.services.RecuperacionService;
import com.elearning.platform.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** RF-002 Autenticar, RF-003 Recuperar contraseña y datos de la sesión actual. */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;
    private final RecuperacionService recuperacion;

    @PostMapping("/login")
    public LoginRespuesta login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return auth.login(req, IpUtil.de(http));
    }

    @PostMapping("/otp/verificar")
    public SesionRespuesta verificarOtp(@Valid @RequestBody OtpRequest req, HttpServletRequest http) {
        return auth.verificarOtp(req, IpUtil.de(http));
    }

    @PostMapping("/recuperar")
    public RecuperarRespuesta recuperar(@Valid @RequestBody RecuperarRequest req, HttpServletRequest http) {
        return recuperacion.solicitar(req, IpUtil.de(http));
    }

    @PostMapping("/restablecer")
    public MensajeRespuesta restablecer(@Valid @RequestBody RestablecerRequest req, HttpServletRequest http) {
        return recuperacion.restablecer(req, IpUtil.de(http));
    }

    @GetMapping("/me")
    public PerfilActual me(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return auth.perfilActual(principal);
    }

    @PostMapping("/primer-acceso")
    public PerfilActual primerAcceso(@AuthenticationPrincipal UsuarioPrincipal principal,
                                     @Valid @RequestBody PrimerAccesoRequest req, HttpServletRequest http) {
        return auth.primerAcceso(principal, req, IpUtil.de(http));
    }

    @PostMapping("/logout")
    public MensajeRespuesta logout(@AuthenticationPrincipal UsuarioPrincipal principal, HttpServletRequest http) {
        auth.cerrarSesion(principal, IpUtil.de(http));
        return new MensajeRespuesta("Cerraste sesión.");
    }
}
