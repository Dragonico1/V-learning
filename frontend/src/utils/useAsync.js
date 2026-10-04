import { useCallback, useEffect, useRef, useState } from "react";
import { mensajeError } from "../api/client.js";

/**
 * Carga datos al montar y cuando cambian las dependencias.
 * Devuelve { datos, cargando, error, recargar, setDatos }.
 */
export function useAsync(funcion, dependencias = []) {
  const [estado, setEstado] = useState({ datos: null, cargando: true, error: null });
  const version = useRef(0);
  const fn = useRef(funcion);
  fn.current = funcion;

  const ejecutar = useCallback(async () => {
    const mia = ++version.current;
    setEstado((e) => ({ ...e, cargando: true, error: null }));
    try {
      const datos = await fn.current();
      if (mia === version.current) setEstado({ datos, cargando: false, error: null });
    } catch (e) {
      if (mia === version.current) setEstado((p) => ({ ...p, cargando: false, error: mensajeError(e) }));
    }
  }, []);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { ejecutar(); }, dependencias);

  const setDatos = useCallback((d) => setEstado((e) => ({ ...e, datos: typeof d === "function" ? d(e.datos) : d })), []);
  return { ...estado, recargar: ejecutar, setDatos };
}
