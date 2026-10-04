import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { api, mensajeError } from "../api/client.js";
import { useAuth } from "./AuthContext.jsx";
import { PREFS_BASE, aplicarAlDocumento } from "../utils/prefs.js";

const CLAVE = "vl.prefs";
const PrefsContext = createContext(null);

function leerLocal() {
  try {
    const t = localStorage.getItem(CLAVE);
    return t ? { ...PREFS_BASE, ...JSON.parse(t) } : PREFS_BASE;
  } catch { return PREFS_BASE; }
}
function guardarLocal(cfg) {
  try { localStorage.setItem(CLAVE, JSON.stringify(cfg)); } catch { /* sin almacenamiento */ }
}

export function PrefsProvider({ children }) {
  const { usuario } = useAuth();
  const esEstudiante = usuario?.rol === "ESTUDIANTE";
  const [config, setConfig] = useState(leerLocal);
  const [categorias, setCategorias] = useState([]);
  const [guardadoEn, setGuardadoEn] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState(null);
  const [advertencias, setAdvertencias] = useState([]);
  const [sugerencia, setSugerencia] = useState(null);
  const temporizador = useRef(null);
  const configRef = useRef(config);

  // Al iniciar sesión, un estudiante trae su perfil guardado en el servidor.
  useEffect(() => {
    if (!esEstudiante || usuario?.primerAcceso) return undefined;
    let activo = true;
    api.accesibilidad.perfil()
      .then((p) => {
        if (!activo) return;
        setCategorias([...(p.categorias ?? [])]);
        if (p.configurado && p.configuracion) {
          const cfg = { ...PREFS_BASE, ...p.configuracion, espaciadoLinea: Number(p.configuracion.espaciadoLinea) };
          setConfig(cfg);
          guardarLocal(cfg);
        }
      })
      .catch(() => { /* se conservan las preferencias del dispositivo */ });
    return () => { activo = false; };
  }, [esEstudiante, usuario?.primerAcceso, usuario?.id]);

  useEffect(() => {
    configRef.current = config;
    aplicarAlDocumento(config, categorias);
  }, [config, categorias]);

  const guardarRemoto = useCallback((cfg) => {
    guardarLocal(cfg);
    if (!esEstudiante) {
      setGuardadoEn(new Date());
      return;
    }
    clearTimeout(temporizador.current);
    setGuardando(true);
    temporizador.current = setTimeout(async () => {
      try {
        const r = await api.accesibilidad.guardarConfiguracion({ ...cfg, espaciadoLinea: Number(cfg.espaciadoLinea) });
        setAdvertencias(r.advertencias ?? []);
        setSugerencia(r.sugerencia ?? null);
        setError(null);
        setGuardadoEn(new Date());
      } catch (e) {
        setError(mensajeError(e, "No pudimos guardar tus ajustes. Se aplican en este dispositivo."));
      } finally {
        setGuardando(false);
      }
    }, 600);
  }, [esEstudiante]);

  const actualizar = useCallback((parcial) => {
    const siguiente = { ...configRef.current, ...parcial };
    configRef.current = siguiente;
    setConfig(siguiente);
    guardarRemoto(siguiente);
  }, [guardarRemoto]);

  const reemplazar = useCallback((cfg, nuevasCategorias) => {
    const siguiente = { ...PREFS_BASE, ...cfg, espaciadoLinea: Number(cfg.espaciadoLinea) };
    setConfig(siguiente);
    guardarLocal(siguiente);
    if (nuevasCategorias) setCategorias([...nuevasCategorias]);
  }, []);

  const restablecer = useCallback(async () => {
    if (esEstudiante) {
      const cfg = await api.accesibilidad.restablecer();
      reemplazar(cfg);
    } else {
      reemplazar(PREFS_BASE);
    }
    setAdvertencias([]);
    setSugerencia(null);
    setGuardadoEn(new Date());
  }, [esEstudiante, reemplazar]);

  const valor = useMemo(() => ({
    config, categorias, setCategorias, actualizar, reemplazar, restablecer, guardadoEn, guardando, error,
    advertencias, sugerencia, setSugerencia, persistenteEnServidor: esEstudiante,
  }), [config, categorias, actualizar, reemplazar, restablecer, guardadoEn, guardando, error, advertencias, sugerencia, esEstudiante]);

  return <PrefsContext.Provider value={valor}>{children}</PrefsContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function usePrefs() {
  const ctx = useContext(PrefsContext);
  if (!ctx) throw new Error("usePrefs debe usarse dentro de PrefsProvider");
  return ctx;
}
