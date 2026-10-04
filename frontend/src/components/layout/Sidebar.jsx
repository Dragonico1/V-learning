import { NavLink, useNavigate } from "react-router-dom";
import { LogOut, X } from "lucide-react";
import { useAuth } from "../../context/AuthContext.jsx";
import { useTutor } from "../../context/TutorContext.jsx";
import { LogoV } from "./Logo.jsx";
import { ITEM_CONFIG, MENU } from "./menus.js";
import { cx } from "../../utils/cx.js";

function Etiqueta() {
  return <span className="ml-auto rounded-full bg-white/20 px-2 py-0.5 text-[0.6875rem] font-extrabold uppercase tracking-[0.04em] max-xl:hidden">Actual</span>;
}

/** Menú lateral fijo: 246 px en escritorio, solo íconos en tableta y cajón en móvil. */
export function Sidebar({ abierto, onCerrar }) {
  const { usuario, cerrarSesion } = useAuth();
  const { abierto: tutorAbierto, alternar } = useTutor();
  const navigate = useNavigate();
  const items = MENU[usuario.rol] ?? [];

  const salir = async () => {
    await cerrarSesion();
    navigate("/login", { replace: true });
  };

  const clase = (activo) => cx(
    "vl-nav-item flex min-h-11 items-center gap-3 rounded-xl px-3 text-[0.9375rem] transition-colors max-xl:justify-center",
    activo ? "bg-primary-600 font-bold text-white" : "text-white/90 hover:bg-white/10",
  );

  const item = (it) => {
    const Icono = it.i;
    if (it.tutor) {
      return (
        <li key="tutor">
          <button type="button" onClick={() => { alternar(); onCerrar?.(); }} aria-pressed={tutorAbierto} title="Tutor IA"
            className={cx(clase(tutorAbierto), "w-full text-left")}>
            <Icono size={20} strokeWidth={1.9} aria-hidden="true" />
            <span className="max-xl:sr-only">{it.t}</span>
            {tutorAbierto && <Etiqueta />}
          </button>
        </li>
      );
    }
    return (
      <li key={it.a}>
        <NavLink to={it.a} onClick={onCerrar} title={it.t} aria-label={it.t} className={({ isActive }) => clase(isActive)}>
          {({ isActive }) => (
            <>
              <Icono size={20} strokeWidth={1.9} aria-hidden="true" />
              <span className="max-xl:sr-only">{it.t}</span>
              {isActive && <Etiqueta />}
            </>
          )}
        </NavLink>
      </li>
    );
  };

  return (
    <>
      {abierto && <div className="fixed inset-0 z-40 bg-black/50 md:hidden" onClick={onCerrar} aria-hidden="true" />}
      <aside aria-label="Menú principal"
        className={cx("on-dark fixed inset-y-0 left-0 z-50 flex w-[246px] flex-col bg-sidebar text-white transition-transform md:w-[72px] md:translate-x-0 xl:w-[246px]",
          abierto ? "translate-x-0" : "-translate-x-full")}>
        <div className="flex items-center gap-3 px-4 pb-4 pt-5 max-xl:md:justify-center max-xl:md:px-2">
          <LogoV />
          <div className="min-w-0 max-xl:md:hidden">
            <p className="text-lg font-extrabold leading-tight text-white">V-Learning</p>
            <p className="text-xs text-primary-300">Aprender a tu manera</p>
          </div>
          <button type="button" onClick={onCerrar} aria-label="Cerrar menú" className="ml-auto flex h-11 w-11 items-center justify-center rounded-lg md:hidden">
            <X size={22} aria-hidden="true" />
          </button>
        </div>

        <nav aria-label="Secciones" className="flex-1 overflow-y-auto px-3">
          <ul className="flex flex-col gap-1">{items.map(item)}</ul>
          <ul className="mt-6 flex flex-col gap-1">{item(ITEM_CONFIG)}</ul>
        </nav>

        <div className="border-t border-white/10 p-3">
          <button type="button" onClick={salir} title="Cerrar sesión"
            className="flex min-h-12 w-full items-center gap-3 rounded-xl bg-sidebar-surface px-3 font-bold text-white hover:bg-white/10 max-xl:md:justify-center">
            <LogOut size={20} strokeWidth={1.9} aria-hidden="true" />
            <span className="md:max-xl:sr-only">Cerrar sesión</span>
          </button>
        </div>
      </aside>
    </>
  );
}
