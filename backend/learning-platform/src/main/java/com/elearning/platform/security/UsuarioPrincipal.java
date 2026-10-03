package com.elearning.platform.security;

import com.elearning.platform.enums.RolUsuario;

/** Usuario autenticado en la petición actual. Se obtiene con @AuthenticationPrincipal. */
public record UsuarioPrincipal(Long id, String correo, String nombre, RolUsuario rol, Long sesionId) {}
