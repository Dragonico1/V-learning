import { useEffect, useId, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Bell, Check, Clock } from "lucide-react";
import { useNotif } from "../../context/NotifContext.jsx";
import { fmtFecha } from "../../utils/format.js";
import { mensajeError } from "../../api/client.js";

const OPCIONES = [["1_DIA", "1 día"], ["3_DIAS", "3 días"], ["1_SEMANA", "1 semana"]];

/** Campana con punto rojo y centro de notificaciones (RF-011). */
export function NotificationBell() {
  const { lista, noLeidas, marcarLeida, posponer, avisar } = useNotif();
  const [abierta, setAbierta] = useState(false);
  const caja = useRef(null);
  const idPanel = useId();

  useEffect(() => {
    if (!abierta) return undefined;
    const fuera = (e) => { if (caja.current && !caja.current.contains(e.target)) setAbierta(false); };
    const tecla = (e) => { if (e.key === "Escape") setAbierta(false); };
    document.addEventListener("mousedown", fuera);
    document.addEventListener("keydown", tecla);
    return () => { document.removeEventListener("mousedown", fuera); document.removeEventListener("keydown", tecla); };
  }, [abierta]);

  const accion = async (fn, ok) => {
    try { await fn(); if (ok) avisar(ok, "ok"); } catch (e) { avisar(mensajeError(e), "error"); }
  };

  return (
    <div ref={caja} className="relative">
      <button type="button" onClick={() => setAbierta((a) => !a)} aria-expanded={abierta} aria-controls={idPanel}
        aria-label={noLeidas > 0 ? `Notificaciones: ${noLeidas} sin leer` : "Notificaciones"}
        className="relative flex h-11 w-11 items-center justify-center rounded-xl border border-line bg-surface text-strong hover:bg-subtle">
        <Bell size={20} aria-hidden="true" />
        {noLeidas > 0 && <span aria-hidden="true" className="absolute right-2 top-2 h-2.5 w-2.5 rounded-full border-2 border-white bg-dot" />}
      </button>
      {abierta && (
        <div id={idPanel} role="region" aria-label="Centro de notificaciones"
          className="vl-in absolute right-0 z-50 mt-2 w-[min(26rem,calc(100vw-2rem))] rounded-card border border-line bg-surface p-4 shadow-lg">
          <div className="mb-2 flex items-center justify-between gap-2">
            <h2 className="text-lg">Notificaciones</h2>
            <Link to="/configuracion" onClick={() => setAbierta(false)} className="text-sm font-bold text-primary-600 underline">Preferencias</Link>
          </div>
          {lista.length === 0 ? (
            <p className="py-6 text-center text-body">No tienes notificaciones por ahora.</p>
          ) : (
            <ul className="max-h-96 divide-y divide-line overflow-y-auto">
              {lista.map((n) => (
                <li key={n.id} className="py-3">
                  <div className="flex items-start gap-2">
                    {!n.leida && <span className="mt-2 inline-block h-2 w-2 shrink-0 rounded-full bg-dot" aria-hidden="true" />}
                    <div className="min-w-0 flex-1">
                      <p className="font-bold text-strong">{n.titulo}{!n.leida && <span className="sr-only"> (sin leer)</span>}</p>
                      <p className="text-sm">{n.mensaje}</p>
                      <p className="mt-0.5 text-xs text-muted">{fmtFecha(n.fechaCreacion)}</p>
                    </div>
                  </div>
                  <div className="mt-2 flex flex-wrap items-center gap-2 pl-4">
                    {!n.leida && (
                      <button type="button" onClick={() => accion(() => marcarLeida(n.id))}
                        className="flex min-h-9 items-center gap-1 rounded-lg border border-line px-2.5 text-sm font-bold text-strong hover:bg-subtle">
                        <Check size={15} aria-hidden="true" />Marcar como leída
                      </button>
                    )}
                    <label className="flex items-center gap-1 text-sm font-bold text-strong">
                      <Clock size={15} aria-hidden="true" /><span className="sr-only">Posponer</span>
                      <select aria-label={`Posponer «${n.titulo}»`} defaultValue=""
                        onChange={(e) => { const v = e.target.value; e.target.value = ""; if (v) accion(() => posponer(n.id, v), "Recordatorio pospuesto."); }}
                        className="min-h-9 rounded-lg border border-line bg-surface px-2 text-sm">
                        <option value="">Posponer…</option>
                        {OPCIONES.map(([v, t]) => <option key={v} value={v}>{t}</option>)}
                      </select>
                    </label>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
