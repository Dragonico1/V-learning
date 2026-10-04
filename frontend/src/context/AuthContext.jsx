import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api, token } from "../api/client.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(Boolean(token.get()));
  const [avisoSesion, setAvisoSesion] = useState(null);

  const refrescar = useCallback(async () => {
    try {
      const u = await api.auth.me();
      setUsuario(u);
      return u;
    } catch {
      token.clear();
      setUsuario(null);
      return null;
    }
  }, []);

  useEffect(() => {
    if (!token.get()) return undefined;
    let activo = true;
    api.auth.me()
      .then((u) => { if (activo) setUsuario(u); })
      .catch(() => { token.clear(); })
      .finally(() => { if (activo) setCargando(false); });
    return () => { activo = false; };
  }, []);

  useEffect(() => {
    const alExpirar = (e) => {
      setUsuario(null);
      setAvisoSesion(e.detail || "Tu sesión terminó por inactividad. Inicia sesión de nuevo para continuar.");
    };
    window.addEventListener("vl:sesion-expirada", alExpirar);
    return () => window.removeEventListener("vl:sesion-expirada", alExpirar);
  }, []);

  const verificarOtp = useCallback(async (correo, codigo) => {
    const r = await api.auth.verificarOtp(correo, codigo);
    token.set(r.token);
    setAvisoSesion(null);
    setUsuario(r.usuario);
    return r.usuario;
  }, []);

  const cerrarSesion = useCallback(async () => {
    try { await api.auth.logout(); } catch { /* la sesión puede haber vencido */ }
    token.clear();
    setUsuario(null);
  }, []);

  const valor = useMemo(() => ({
    usuario, cargando, avisoSesion, setAvisoSesion, refrescar, verificarOtp, cerrarSesion, setUsuario,
  }), [usuario, cargando, avisoSesion, refrescar, verificarOtp, cerrarSesion]);

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth debe usarse dentro de AuthProvider");
  return ctx;
}
