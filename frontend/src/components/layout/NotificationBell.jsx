import { useEffect, useId, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Bell } from "lucide-react";
import { useNotif } from "../../context/NotifContext.jsx";
import { fmtFecha } from "../../utils/format.js";

/** Campana con punto rojo y centro de notificaciones (RF-011). */
export function NotificationBell({ redonda = false }) {
  const { lista, noLeidas, marcarTodasLeidas } = useNotif();
  const [abierta, setAbierta] = useState(false);
  const caja = useRef(null);
  const idPanel = useId();

  const alternar = () => {
    const abrir = !abierta;
    setAbierta(abrir);
    if (abrir && noLeidas > 0) marcarTodasLeidas();
  };

  useEffect(() => {
    if (!abierta) return undefined;
    const fuera = (e) => {
      if (caja.current && !caja.current.contains(e.target)) setAbierta(false);
    };
    const tecla = (e) => {
      if (e.key === "Escape") setAbierta(false);
    };
    document.addEventListener("mousedown", fuera);
    document.addEventListener("keydown", tecla);
    return () => {
      document.removeEventListener("mousedown", fuera);
      document.removeEventListener("keydown", tecla);
    };
  }, [abierta]);

  return (
    <div ref={caja} className="relative">
      <button
        type="button"
        onClick={alternar}
        aria-expanded={abierta}
        aria-controls={idPanel}
        aria-label={
          noLeidas > 0
            ? `Notificaciones: ${noLeidas} sin leer`
            : "Notificaciones"
        }
        className={`relative flex items-center justify-center ${redonda ? "h-10 w-10 rounded-full bg-primary-100 text-primary-700! hover:bg-primary-200 2xl:h-11 2xl:w-11" : "h-11 w-11 rounded-xl bg-subtle text-strong hover:bg-primary-100"}`}
      >
        <Bell size={20} aria-hidden="true" />
        {noLeidas > 0 && (
          <span
            aria-hidden="true"
            className="absolute right-2 top-2 h-2.5 w-2.5 rounded-full border-2 border-white bg-dot"
          />
        )}
      </button>
      {abierta && (
        <div
          id={idPanel}
          role="region"
          aria-label="Centro de notificaciones"
          className="vl-in absolute right-0 z-50 mt-2 w-[min(26rem,calc(100vw-2rem))] rounded-card border border-line bg-surface p-4 shadow-lg"
        >
          <div className="mb-2 flex items-center justify-between gap-2">
            <h2 className="text-lg">Notificaciones</h2>
            <Link
              to="/configuracion"
              onClick={() => setAbierta(false)}
              className="text-sm font-bold text-primary-600 underline"
            >
              Preferencias
            </Link>
          </div>
          {lista.length === 0 ? (
            <p className="py-6 text-center text-body">
              No tienes notificaciones por ahora.
            </p>
          ) : (
            <ul className="max-h-96 divide-y divide-line overflow-y-auto">
              {lista.map((n) => (
                <li key={n.id} className="py-3">
                  <div className="flex items-start gap-2">
                    {!n.leida && (
                      <span
                        className="mt-2 inline-block h-2 w-2 shrink-0 rounded-full bg-dot"
                        aria-hidden="true"
                      />
                    )}
                    <div className="min-w-0 flex-1">
                      <p className="font-bold text-strong">
                        {n.titulo}
                        {!n.leida && (
                          <span className="sr-only"> (sin leer)</span>
                        )}
                      </p>
                      <p className="text-sm">{n.mensaje}</p>
                      <p className="mt-0.5 text-xs text-muted">
                        {fmtFecha(n.fechaCreacion)}
                      </p>
                    </div>
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
