import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { CheckCircle2, CircleAlert, Info, X } from "lucide-react";
import { api } from "../api/client.js";
import { useAuth } from "./AuthContext.jsx";
import { reproducirAviso, sonidoActivado } from "../utils/sonido.js";

const NotifContext = createContext(null);
const ICONOS = { ok: CheckCircle2, error: CircleAlert, info: Info };
const ESTILOS = {
  ok: "bg-ok-bg text-ok-fg border-ok-fg",
  error: "bg-err-bg text-err-fg border-err-fg",
  info: "bg-primary-100 text-primary-700 border-primary-600",
};

/**
 * Notificaciones (RF-011) y alertas visuales (RF-020): cada aviso es un banner emergente visible más un
 * anuncio en una región aria-live. Al llegar una notificación del servidor suena un tono corto, que cada
 * persona puede apagar en Configuración (nunca es la única señal).
 */
export function NotifProvider({ children }) {
  const { usuario } = useAuth();
  const [lista, setLista] = useState([]);
  const [noLeidas, setNoLeidas] = useState(0);
  const [toasts, setToasts] = useState([]);
  const vistas = useRef(null);
  const contador = useRef(0);

  const avisar = useCallback((mensaje, tipo = "info") => {
    const id = ++contador.current;
    setToasts((t) => [...t, { id, mensaje, tipo }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), tipo === "error" ? 9000 : 6000);
  }, []);

  const cerrarToast = (id) => setToasts((t) => t.filter((x) => x.id !== id));

  const recargar = useCallback(async () => {
    if (!usuario || usuario.primerAcceso) return;
    try {
      const r = await api.notificaciones.listar(30);
      setLista(r.notificaciones);
      setNoLeidas(r.noLeidas);
      if (vistas.current === null) {
        vistas.current = new Set(r.notificaciones.map((n) => n.id));
      } else {
        const nuevas = r.notificaciones.filter((n) => !n.leida && !vistas.current.has(n.id));
        nuevas.forEach((n) => {
          vistas.current.add(n.id);
          avisar(`${n.titulo}: ${n.mensaje}`, "info");
        });
        if (nuevas.length > 0 && sonidoActivado()) reproducirAviso();
      }
    } catch { /* el sondeo reintenta en 30 s */ }
  }, [usuario, avisar]);

  useEffect(() => {
    vistas.current = null;
    if (!usuario || usuario.primerAcceso) return undefined;
    const primera = setTimeout(recargar, 0);
    const intervalo = setInterval(recargar, 30000);
    return () => { clearTimeout(primera); clearInterval(intervalo); };
  }, [usuario, recargar]);

  const marcarLeida = useCallback(async (id) => {
    await api.notificaciones.leida(id);
    await recargar();
  }, [recargar]);

  const posponer = useCallback(async (id, opcion) => {
    await api.notificaciones.posponer(id, opcion);
    await recargar();
  }, [recargar]);

  const valor = useMemo(() => ({ lista, noLeidas, avisar, recargar, marcarLeida, posponer }),
    [lista, noLeidas, avisar, recargar, marcarLeida, posponer]);

  return (
    <NotifContext.Provider value={valor}>
      {children}
      <div className="fixed right-4 top-4 z-[60] flex w-[min(24rem,calc(100vw-2rem))] flex-col gap-2" aria-live="polite" aria-atomic="false" role="status">
        {toasts.map((t) => {
          const Icono = ICONOS[t.tipo] ?? Info;
          return (
            <div key={t.id} role={t.tipo === "error" ? "alert" : undefined}
              className={`vl-in flex items-start gap-3 rounded-xl border-2 p-3 text-sm font-semibold shadow-md ${ESTILOS[t.tipo] ?? ESTILOS.info}`}>
              <Icono size={20} aria-hidden="true" className="mt-0.5 shrink-0" />
              <p className="flex-1">{t.mensaje}</p>
              <button type="button" onClick={() => cerrarToast(t.id)} aria-label="Cerrar aviso" className="min-h-6 min-w-6 rounded">
                <X size={18} aria-hidden="true" />
              </button>
            </div>
          );
        })}
      </div>
    </NotifContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useNotif() {
  const ctx = useContext(NotifContext);
  if (!ctx) throw new Error("useNotif debe usarse dentro de NotifProvider");
  return ctx;
}
