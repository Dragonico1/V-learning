import { useEffect, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { LogOut, Menu, X } from "lucide-react";
import { useAuth } from "../../context/AuthContext.jsx";
import { ITEM_CONFIG, MENU } from "./menus.js";
import logoCompleto from "../../assets/LogoVLearning.svg";
import logoSolo from "../../assets/LogoVLearning-solo.svg";
import { cx } from "../../utils/cx.js";

/**
 * Menú lateral fijo. En escritorio y tableta queda recogido (72 px, solo
 * íconos) y se despliega (246 px) con el botón de abajo, flotando sobre el
 * contenido. En móvil es un cajón que siempre muestra los textos.
 */
export function Sidebar({ abierto, onCerrar }) {
  const { usuario, cerrarSesion } = useAuth();
  const navigate = useNavigate();
  const [expandido, setExpandido] = useState(false);
  const items = MENU[usuario.rol] ?? [];

  // Esc recoge el menú desplegado.
  useEffect(() => {
    if (!expandido) return undefined;
    const alTeclear = (e) => {
      if (e.key === "Escape") setExpandido(false);
    };
    document.addEventListener("keydown", alTeclear);
    return () => document.removeEventListener("keydown", alTeclear);
  }, [expandido]);

  const salir = async () => {
    await cerrarSesion();
    navigate("/login", { replace: true });
  };

  const alElegir = () => {
    setExpandido(false);
    onCerrar();
  };

  const clase = (activo) =>
    cx(
      "vl-nav-item flex min-h-11 items-center gap-3 whitespace-nowrap rounded-xl px-3 text-[0.9375rem] transition-colors",
      !expandido && "md:justify-center",
      activo
        ? "bg-primary-600 font-bold text-white"
        : "text-white/90 hover:bg-white/10",
    );

  // Texto visible siempre en móvil; en escritorio solo con el menú desplegado.
  const claseTexto = cx(!expandido && "md:sr-only");

  const item = (it) => {
    const Icono = it.i;
    return (
      <li key={it.a}>
        <NavLink
          to={it.a}
          onClick={alElegir}
          title={it.t}
          aria-label={it.t}
          className={({ isActive }) => clase(isActive)}
        >
          <Icono size={20} strokeWidth={1.9} aria-hidden="true" />
          <span className={claseTexto}>{it.t}</span>
        </NavLink>
      </li>
    );
  };

  const textoAlternar = expandido ? "Contraer menú" : "Expandir menú";

  return (
    <>
      {abierto && (
        <div
          className="fixed inset-0 z-40 bg-black/50 md:hidden"
          onClick={onCerrar}
          aria-hidden="true"
        />
      )}
      {expandido && (
        <div
          className="fixed inset-0 z-40 hidden bg-black/30 md:block"
          onClick={() => setExpandido(false)}
          aria-hidden="true"
        />
      )}
      <aside
        id="menu-lateral"
        aria-label="Menú principal"
        className={cx(
          "on-dark fixed inset-y-0 left-0 z-50 flex w-[246px] flex-col bg-sidebar text-white transition-[transform,width] md:translate-x-0",
          expandido ? "md:w-[246px] md:shadow-2xl" : "md:w-[72px]",
          abierto ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <div
          className={cx(
            "flex items-center gap-3 px-6 pb-4 pt-5 md:px-3",
            !expandido && "md:flex-col md:gap-2",
          )}
        >
          {/* Hamburguesa: abre y recoge el menú en escritorio y tableta. */}
          <button
            type="button"
            onClick={() => setExpandido(!expandido)}
            aria-expanded={expandido}
            aria-controls="menu-lateral"
            aria-label={textoAlternar}
            title={textoAlternar}
            className="hidden size-11 shrink-0 items-center justify-center rounded-xl text-white transition-colors hover:bg-white/10 md:flex [html[data-targets=large]_&]:w-14"
          >
            <Menu size={22} strokeWidth={1.9} aria-hidden="true" />
          </button>
          <div className={cx("min-w-0 text-white", !expandido && "md:hidden")}>
            <img src={logoCompleto} alt="V-Learning" className="h-7 w-auto" />
          </div>
          <div className={cx("hidden text-white", !expandido && "md:block")}>
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
                <span className={claseTexto}>Cerrar sesión</span>
              </button>
            </li>
          </ul>
        </div>
      </aside>
    </>
  );
}
