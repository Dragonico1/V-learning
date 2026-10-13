import { NavLink, useNavigate } from "react-router-dom";
import { LogOut, X } from "lucide-react";
import { useAuth } from "../../context/AuthContext.jsx";
import { ITEM_CONFIG, MENU } from "./menus.js";
import logoCompleto from "../../assets/LogoVLearning.svg";
import logoSolo from "../../assets/LogoVLearning-solo.svg";
import { cx } from "../../utils/cx.js";

/** Menú lateral fijo: 246 px en escritorio, solo íconos en tableta y cajón en móvil. */
export function Sidebar({ abierto, onCerrar }) {
  const { usuario, cerrarSesion } = useAuth();
  const navigate = useNavigate();
  const items = MENU[usuario.rol] ?? [];

  const salir = async () => {
    await cerrarSesion();
    navigate("/login", { replace: true });
  };

  const clase = (activo) =>
    cx(
      "vl-nav-item flex min-h-11 items-center gap-3 rounded-xl px-3 text-[0.9375rem] transition-colors max-xl:justify-center",
      activo
        ? "bg-primary-600 font-bold text-white"
        : "text-white/90 hover:bg-white/10",
    );

  const item = (it) => {
    const Icono = it.i;
    return (
      <li key={it.a}>
        <NavLink
          to={it.a}
          onClick={onCerrar}
          title={it.t}
          aria-label={it.t}
          className={({ isActive }) => clase(isActive)}
        >
          <Icono size={20} strokeWidth={1.9} aria-hidden="true" />
          <span className="max-xl:sr-only">{it.t}</span>
        </NavLink>
      </li>
    );
  };

  return (
    <>
      {abierto && (
        <div
          className="fixed inset-0 z-40 bg-black/50 md:hidden"
          onClick={onCerrar}
          aria-hidden="true"
        />
      )}
      <aside
        aria-label="Menú principal"
        className={cx(
          "on-dark fixed inset-y-0 left-0 z-50 flex w-[246px] flex-col bg-sidebar text-white transition-transform md:w-[72px] md:translate-x-0 xl:w-[246px]",
          abierto ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <div className="flex items-center gap-3 px-6 pb-4 pt-5 max-xl:md:justify-center max-xl:md:px-2">
          <div className="min-w-0 text-white max-xl:md:hidden">
            <img src={logoCompleto} alt="V-Learning" className="h-7 w-auto" />
          </div>
          <div className="hidden text-white max-xl:md:block">
            <img src={logoSolo} alt="V-Learning" className="h-7 w-auto" />
          </div>
          <button
            type="button"
            onClick={onCerrar}
            aria-label="Cerrar menú"
            className="ml-auto flex h-11 w-11 items-center justify-center rounded-lg md:hidden"
          >
            <X size={22} aria-hidden="true" />
          </button>
        </div>

        <nav aria-label="Secciones" className="flex-1 overflow-y-auto px-3">
          <ul className="flex flex-col gap-1">{items.map(item)}</ul>
        </nav>

        <div className="border-t border-white/10 p-3">
          <ul className="flex flex-col gap-1">
            {item(ITEM_CONFIG)}
            <li>
              <button
                type="button"
                onClick={salir}
                title="Cerrar sesión"
                className={cx(clase(false), "w-full text-left")}
              >
                <LogOut size={20} strokeWidth={1.9} aria-hidden="true" />
                <span className="max-xl:sr-only">Cerrar sesión</span>
              </button>
            </li>
          </ul>
        </div>
      </aside>
    </>
  );
}
