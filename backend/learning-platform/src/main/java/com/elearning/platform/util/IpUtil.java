package com.elearning.platform.util;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtil {

    private IpUtil() {}

    /** Dirección remota de la conexión (máx. 45 caracteres, como la columna ip_origen). */
    public static String de(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (ip == null) return null;
        return ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
