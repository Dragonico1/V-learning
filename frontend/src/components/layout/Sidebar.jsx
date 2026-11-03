import { useEffect, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { LogOut, X, ChevronsLeft, ChevronsRight } from "lucide-react";

import { useAuth } from "../../context/AuthContext.jsx";
import { ITEM_CONFIG, MENU } from "./menus.js";
import logoCompleto from "../../assets/LogoVLearning.svg";
import logoSolo from "../../assets/LogoVLearning-solo.svg";
import { cx } from "../../utils/cx.js";

/**
 * Menú lateral fijo.
 * En escritorio y tableta queda recogido (72 px, solo íconos)
 * y se despliega (246 px) mediante el control de doble flecha.
 * En móvil funciona como un cajón que siempre muestra los textos.
 */
export function Sidebar({ abierto, onCerrar }) {
  const { usuario, cerrarSesion } = useAuth();
  const navigate = useNavigate();
  const [expandido, setExpandido] = useState(false);

  const items = MENU[usuario.rol] ?? [];

  // Esc cierra el menú desplegado.
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

  // Clase compartida para todos los elementos de navegación.
  const clase = (activo) =>
    cx(
      "vl-nav-item flex min-h-11 short:min-h-10 cursor-pointer items-center gap-3 whitespace-nowrap rounded-xl px-3 text-[0.9375rem] transition-colors",
      !expandido && "md:justify-center",
      activo
        ? "bg-primary-600 font-bold text-white"
        : "text-white/90 hover:bg-white/10",
    );

  // Texto visible siempre en móvil; en escritorio solo al expandir.
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
          <Icono
            size={20}
            strokeWidth={1.9}
            aria-hidden="true"
            className="shrink-0"
          />
          <span className={claseTexto}>{it.t}</span>
        </NavLink>
      </li>
    );
  };

  const textoAlternar = expandido ? "Contraer menú" : "Expandir menú";

  return (
    <>
      {/* Fondo oscuro para el menú móvil */}
      {abierto && (
        <div
          className="fixed inset-0 z-40 cursor-pointer bg-black/50 md:hidden"
          onClick={onCerrar}
          aria-hidden="true"
        />
      )}

      {/* Fondo al expandir el menú en escritorio */}
      {expandido && (
        <div
          className="fixed inset-0 z-40 hidden cursor-pointer bg-black/30 md:block"
          onClick={() => setExpandido(false)}
          aria-hidden="true"
        />
      )}

      <aside
        id="menu-lateral"
        aria-label="Menú principal"
        className={cx(
          "on-dark fixed inset-y-0 left-0 z-50 flex w-[246px] flex-col bg-sidebar text-white transition-[transform,width] duration-300 md:translate-x-0",
          expandido ? "md:w-[246px] md:shadow-2xl" : "md:w-[72px]",
          abierto ? "translate-x-0" : "-translate-x-full",
        )}
      >
        {/* Encabezado */}
        <div
          className={cx(
            "relative flex min-h-[76px] items-center gap-3 px-6 pb-4 pt-5 md:px-3",
            !expandido &&
              "md:min-h-[104px] short:md:min-h-[84px] md:flex-col md:justify-start md:gap-2",
          )}
        >
          {/* Logo completo */}
          <div className={cx("min-w-0 text-white", !expandido && "md:hidden")}>
            <img src={logoCompleto} alt="V-Learning" className="h-7 w-auto" />
          </div>

          {/* Logo reducido */}
          <div className={cx("hidden text-white", !expandido && "md:block")}>
            <img src={logoSolo} alt="V-Learning" className="h-7 w-auto" />
          </div>

          {/* Control de expansión */}
          <button
            type="button"
            onClick={() => setExpandido((prev) => !prev)}
            aria-expanded={expandido}
            aria-controls="menu-lateral"
            aria-label={textoAlternar}
            title={textoAlternar}
            className={cx(
              "hidden h-8 w-8 shrink-0 cursor-pointer items-center justify-center rounded-lg text-white/90 transition-colors hover:bg-white/10 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#2F6FED] md:flex",
              expandido ? "ml-auto" : "mx-auto",
            )}
          >
            {expandido ? (
              <ChevronsLeft size={20} strokeWidth={1.9} />
            ) : (
              <ChevronsRight size={20} strokeWidth={1.9} />
            )}
          </button>

          {/* Botón para cerrar en móvil */}
          <button
            type="button"
            onClick={onCerrar}
            aria-label="Cerrar menú"
            className="ml-auto flex h-11 w-11 cursor-pointer items-center justify-center rounded-lg transition-colors hover:bg-white/10 md:hidden"
          >
            <X size={22} aria-hidden="true" />
          </button>
        </div>

        {/* Navegación principal */}
        <nav
          aria-label="Secciones"
          className="vl-scroll-hint flex-1 overflow-y-auto px-3 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
        >
          <ul className="flex flex-col gap-1 short:gap-0.5">
            {items.map(item)}
          </ul>
        </nav>

        {/* Configuración y cierre de sesión */}
        <div className="border-t border-white/10 p-3 short:py-2">
          <ul className="flex flex-col gap-1">
            {item(ITEM_CONFIG)}

            <li>
              <button
                type="button"
                onClick={salir}
                title="Cerrar sesión"
                className={cx(clase(false), "w-full cursor-pointer text-left")}
              >
                <LogOut
                  size={20}
                  strokeWidth={1.9}
                  aria-hidden="true"
                  className="shrink-0"
                />
                <span className={claseTexto}>Cerrar sesión</span>
              </button>
            </li>
          </ul>
        </div>
      </aside>
    </>
  );
}
