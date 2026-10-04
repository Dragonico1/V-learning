import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ChevronDown, LogOut, Menu, Search, Settings, Star } from "lucide-react";
import { useAuth } from "../../context/AuthContext.jsx";
import { api } from "../../api/client.js";
import { fmtNumero, ROL_TEXTO } from "../../utils/format.js";
import { NotificationBell } from "./NotificationBell.jsx";
import { TITULOS } from "./menus.js";

function Migas() {
  const { pathname } = useLocation();
  const segs = pathname.split("/").filter(Boolean).filter((s) => Number.isNaN(Number(s)));
  const etiquetas = segs.map((s) => TITULOS[s] ?? s);
  return (
    <nav aria-label="Ruta de navegación" className="min-w-0 max-md:hidden">
      <ol className="flex items-center gap-1.5 truncate text-sm">
        <li className="text-muted">V-Learning</li>
        {etiquetas.map((t, i) => (
          <li key={`${t}-${i}`} className="flex items-center gap-1.5">
            <span aria-hidden="true" className="text-muted">/</span>
            <span aria-current={i === etiquetas.length - 1 ? "page" : undefined}
              className={i === etiquetas.length - 1 ? "font-bold text-strong" : "text-muted"}>{t}</span>
          </li>
        ))}
      </ol>
    </nav>
  );
}

function ChipPuntos() {
  const [puntos, setPuntos] = useState(null);
  useEffect(() => {
    let activo = true;
    const cargar = () => api.logros.ver().then((l) => { if (activo) setPuntos(l.puntos); }).catch(() => {});
    cargar();
    window.addEventListener("vl:puntos", cargar);
    return () => { activo = false; window.removeEventListener("vl:puntos", cargar); };
  }, []);
  if (puntos === null) return null;
  return (
    <Link to="/logros" className="flex min-h-11 items-center gap-1.5 rounded-full border-[1.5px] border-primary-300 bg-surface px-3.5 font-bold text-strong hover:bg-primary-100"
      aria-label={`${fmtNumero(puntos)} puntos. Ver mis logros`}>
      <Star size={16} aria-hidden="true" className="fill-primary-600 text-primary-600" />{fmtNumero(puntos)} pts
    </Link>
  );
}

export function Topbar({ onMenu, buscadorRef }) {
  const { usuario, cerrarSesion } = useAuth();
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const [menu, setMenu] = useState(false);
  const caja = useRef(null);

  useEffect(() => {
    if (!menu) return undefined;
    const fuera = (e) => { if (caja.current && !caja.current.contains(e.target)) setMenu(false); };
    const tecla = (e) => { if (e.key === "Escape") setMenu(false); };
    document.addEventListener("mousedown", fuera);
    document.addEventListener("keydown", tecla);
    return () => { document.removeEventListener("mousedown", fuera); document.removeEventListener("keydown", tecla); };
  }, [menu]);

  const buscar = (e) => {
    e.preventDefault();
    navigate(`/cursos${q.trim() ? `?q=${encodeURIComponent(q.trim())}` : ""}`);
  };

  return (
    <header className="sticky top-0 z-30 flex min-h-[70px] items-center gap-3 border-b border-line bg-surface px-4 md:px-6">
      <button type="button" onClick={onMenu} aria-label="Abrir menú"
        className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-line md:hidden"><Menu size={22} aria-hidden="true" /></button>
      <Migas />
      <form onSubmit={buscar} role="search" className="mx-auto w-full max-w-md flex-1">
        <label htmlFor="buscador" className="sr-only">Buscar cursos</label>
        <div className="relative">
          <Search size={18} aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
          <input id="buscador" ref={buscadorRef} value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar cursos"
            className="vl-input min-h-11 w-full rounded-xl border border-line bg-subtle pl-10 pr-16 text-strong placeholder:text-muted" />
          <kbd aria-hidden="true" className="absolute right-3 top-1/2 -translate-y-1/2 rounded border border-line bg-surface px-1.5 text-xs font-bold text-muted max-lg:hidden">Ctrl K</kbd>
        </div>
      </form>
      <div className="ml-auto flex items-center gap-2 md:gap-3">
        {usuario.rol === "ESTUDIANTE" && <ChipPuntos />}
        <NotificationBell />
        <div ref={caja} className="relative">
          <button type="button" onClick={() => setMenu((m) => !m)} aria-expanded={menu} aria-haspopup="menu"
            className="flex min-h-11 items-center gap-2 rounded-xl border border-line bg-surface px-3 font-bold text-strong hover:bg-subtle">
            <span className="max-w-32 truncate max-sm:sr-only">{usuario.nombre}</span>
            <ChevronDown size={16} aria-hidden="true" />
          </button>
          {menu && (
            <div role="menu" className="vl-in absolute right-0 z-50 mt-2 w-60 rounded-card border border-line bg-surface p-2 shadow-lg">
              <p className="px-3 py-2 text-sm"><span className="block font-bold text-strong">{usuario.nombre}</span>
                <span className="block text-muted">{ROL_TEXTO[usuario.rol]} · {usuario.correo}</span></p>
              <Link role="menuitem" to="/configuracion" onClick={() => setMenu(false)} className="flex min-h-11 items-center gap-2 rounded-lg px-3 font-bold text-strong hover:bg-subtle">
                <Settings size={18} aria-hidden="true" />Configuración</Link>
              <button role="menuitem" type="button" onClick={async () => { await cerrarSesion(); navigate("/login", { replace: true }); }}
                className="flex min-h-11 w-full items-center gap-2 rounded-lg px-3 text-left font-bold text-strong hover:bg-subtle">
                <LogOut size={18} aria-hidden="true" />Cerrar sesión</button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
