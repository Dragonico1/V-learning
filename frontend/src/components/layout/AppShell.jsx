import { useEffect, useRef, useState } from "react";
import { Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext.jsx";
import { useTutor } from "../../context/TutorContext.jsx";
import { Sidebar } from "./Sidebar.jsx";
import { Topbar } from "./Topbar.jsx";
import { TutorPanel } from "./TutorPanel.jsx";
import { ShortcutsDialog } from "./ShortcutsDialog.jsx";
import { cx } from "../../utils/cx.js";

/** Plantillas C y D: menú lateral + barra superior + contenido (+ panel del Tutor para estudiantes). */
export function AppShell() {
  const { usuario } = useAuth();
  const { abierto: tutorAbierto, alternar } = useTutor();
  const [menu, setMenu] = useState(false);
  const [atajos, setAtajos] = useState(false);
  const buscador = useRef(null);
  const principal = useRef(null);
  const { pathname } = useLocation();
  const esEstudiante = usuario.rol === "ESTUDIANTE";

  // Al cambiar de pantalla, el foco vuelve al contenido y se cierra el cajón móvil.
  useEffect(() => {
    setMenu(false);
    principal.current?.focus({ preventScroll: true });
    window.scrollTo(0, 0);
  }, [pathname]);

  useEffect(() => {
    const tecla = (e) => {
      const el = e.target;
      const escribiendo = el instanceof HTMLElement && (el.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(el.tagName));
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") { e.preventDefault(); buscador.current?.focus(); return; }
      if (e.altKey && e.key.toLowerCase() === "t" && esEstudiante) { e.preventDefault(); alternar(); return; }
      if (e.key === "?" && !escribiendo) { e.preventDefault(); setAtajos(true); }
    };
    document.addEventListener("keydown", tecla);
    return () => document.removeEventListener("keydown", tecla);
  }, [alternar, esEstudiante]);

  return (
    <div className="min-h-screen">
      <a href="#contenido" className="sr-only-focusable fixed left-3 top-3 z-[70] rounded-lg bg-primary-600 px-4 py-2 font-bold text-white">Saltar al contenido principal</a>
      <Sidebar abierto={menu} onCerrar={() => setMenu(false)} />
      <div className="md:pl-[72px] xl:pl-[246px]">
        <Topbar onMenu={() => setMenu(true)} buscadorRef={buscador} />
        <div className="flex">
          <main id="contenido" ref={principal} tabIndex={-1} className="min-w-0 flex-1 px-4 py-6 outline-none md:px-8 md:py-8">
            <div className="mx-auto max-w-[1180px]"><Outlet /></div>
          </main>
          {esEstudiante && tutorAbierto && (
            <div className={cx("fixed inset-0 z-[55] bg-white xl:sticky xl:inset-auto xl:top-[70px] xl:z-20 xl:h-[calc(100vh-70px)] xl:w-[420px] xl:shrink-0 xl:border-l xl:border-line",
              "max-xl:md:left-auto max-xl:md:w-[420px] max-xl:md:border-l max-xl:md:border-line max-xl:md:shadow-xl")}>
              <TutorPanel />
            </div>
          )}
        </div>
      </div>
      <ShortcutsDialog abierto={atajos} onCerrar={() => setAtajos(false)} />
    </div>
  );
}
