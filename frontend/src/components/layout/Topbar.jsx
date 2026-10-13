import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ChevronDown,
  LogOut,
  Menu,
  Search,
  Settings,
  Star,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext.jsx";
import { api } from "../../api/client.js";
import { fmtNumero, ROL_TEXTO } from "../../utils/format.js";
import { NotificationBell } from "./NotificationBell.jsx";

export function ChipPuntos() {
  const [puntos, setPuntos] = useState(null);
  useEffect(() => {
    let activo = true;
    const cargar = () =>
      api.logros
        .ver()
        .then((l) => {
          if (activo) setPuntos(l.puntos);
        })
        .catch(() => {});
    cargar();
    window.addEventListener("vl:puntos", cargar);
    return () => {
      activo = false;
      window.removeEventListener("vl:puntos", cargar);
    };
  }, []);
  if (puntos === null) return null;
  return (
    <Link
      to="/logros"
      className="flex min-h-11 items-center gap-1.5 rounded-full bg-primary-100 px-3.5 font-bold text-strong hover:bg-primary-200"
      aria-label={`${fmtNumero(puntos)} puntos. Ver mis logros`}
    >
      <Star
        size={16}
        aria-hidden="true"
        className="fill-primary-600 text-primary-600"
      />
      {fmtNumero(puntos)} pts
    </Link>
  );
}

export function Topbar({ onMenu, buscadorRef, cuentaEnPanel = false }) {
  const { usuario, cerrarSesion } = useAuth();
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const [menu, setMenu] = useState(false);
  const caja = useRef(null);

  useEffect(() => {
    if (!menu) return undefined;
    const fuera = (e) => {
      if (caja.current && !caja.current.contains(e.target)) setMenu(false);
    };
    const tecla = (e) => {
      if (e.key === "Escape") setMenu(false);
    };
    document.addEventListener("mousedown", fuera);
    document.addEventListener("keydown", tecla);
    return () => {
      document.removeEventListener("mousedown", fuera);
      document.removeEventListener("keydown", tecla);
    };
  }, [menu]);

  const buscar = (e) => {
    e.preventDefault();
    navigate(`/cursos${q.trim() ? `?q=${encodeURIComponent(q.trim())}` : ""}`);
  };

  return (
    <header className="sticky top-0 z-30 flex min-h-[70px] items-center gap-3 bg-surface px-4 md:px-6">
      <button
        type="button"
        onClick={onMenu}
        aria-label="Abrir menú"
        className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-subtle md:hidden"
      >
        <Menu size={22} aria-hidden="true" />
      </button>
      <form onSubmit={buscar} role="search" className="w-full max-w-2xl flex-1">
        <label htmlFor="buscador" className="sr-only">
          Buscar cursos
        </label>
        <div className="relative">
          <Search
            size={18}
            aria-hidden="true"
            className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
          />
          <input
            id="buscador"
            ref={buscadorRef}
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Buscar cursos"
            className="vl-input min-h-11 w-full rounded-xl bg-subtle pl-10 pr-16 text-strong placeholder:text-muted"
          />
          <kbd
            aria-hidden="true"
            className="absolute right-3 top-1/2 -translate-y-1/2 rounded bg-surface px-1.5 text-xs font-bold text-muted max-lg:hidden"
          >
            Ctrl K
          </kbd>
        </div>
      </form>
      <div
        className={`ml-auto flex items-center gap-2 md:gap-3${cuentaEnPanel ? " xl:hidden" : ""}`}
      >
        {usuario.rol === "ESTUDIANTE" && <ChipPuntos />}
        <NotificationBell />
        <div ref={caja} className="relative">
          <button
            type="button"
            onClick={() => setMenu((m) => !m)}
            aria-expanded={menu}
            aria-haspopup="menu"
            className="flex min-h-11 items-center gap-2 rounded-xl bg-subtle px-3 font-bold text-strong hover:bg-primary-100"
          >
            <span className="max-w-32 truncate max-sm:sr-only">
              {usuario.nombre}
            </span>
            <ChevronDown size={16} aria-hidden="true" />
          </button>
          {menu && (
            <div
              role="menu"
              className="vl-in absolute right-0 z-50 mt-2 w-60 rounded-card bg-surface p-2 shadow-[0_8px_30px_rgba(15,23,42,0.18)]"
            >
              <p className="px-3 py-2 text-sm">
                <span className="block font-bold text-strong">
                  {usuario.nombre}
                </span>
                <span className="block text-muted">
                  {ROL_TEXTO[usuario.rol]} · {usuario.correo}
                </span>
              </p>
              <Link
                role="menuitem"
                to="/configuracion"
                onClick={() => setMenu(false)}
                className="flex min-h-11 items-center gap-2 rounded-lg px-3 font-bold text-strong hover:bg-subtle"
              >
                <Settings size={18} aria-hidden="true" />
                Configuración
              </Link>
              <button
                role="menuitem"
                type="button"
                onClick={async () => {
                  await cerrarSesion();
                  navigate("/login", { replace: true });
                }}
                className="flex min-h-11 w-full items-center gap-2 rounded-lg px-3 text-left font-bold text-strong hover:bg-subtle"
              >
                <LogOut size={18} aria-hidden="true" />
                Cerrar sesión
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
