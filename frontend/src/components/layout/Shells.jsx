import { Link } from "react-router-dom";
import { HelpCircle, LogOut } from "lucide-react";
import { cx } from "../../utils/cx.js";

/** Plantilla A: autenticación (fondo claro con círculos decorativos y tarjeta centrada). */
export function AuthShell({ children, ancho = "max-w-[650px]", ayuda = true }) {
  return (
    <div className="relative min-h-screen overflow-hidden bg-page">
      <span aria-hidden="true" className="pointer-events-none absolute -left-24 -top-24 h-72 w-72 rounded-full bg-primary-200/70" />
      <span aria-hidden="true" className="pointer-events-none absolute -bottom-32 -right-20 h-96 w-96 rounded-full bg-primary-200/70" />
      <header className="relative z-10 flex items-center justify-between px-6 py-5 md:px-10">
        <Link to="/login" className="flex items-center gap-3" aria-label="V-Learning, ir al inicio de sesión">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-600 text-xl font-extrabold text-white" aria-hidden="true">V</span>
          <span className="text-lg font-extrabold text-strong">V-Learning</span>
        </Link>
        {ayuda && (
          <a href="mailto:soporte@tdea.edu.co?subject=Ayuda%20de%20acceso%20V-Learning" className="flex min-h-11 items-center gap-2 rounded-lg px-3 font-bold text-primary-600 underline">
            <HelpCircle size={18} aria-hidden="true" />Ayuda de acceso
          </a>
        )}
      </header>
      <main id="contenido" tabIndex={-1} className="relative z-10 flex justify-center px-4 pb-16 pt-4 outline-none">
        <div className={cx("w-full rounded-screen border border-line bg-surface p-6 shadow-md md:p-10", ancho)}>{children}</div>
      </main>
    </div>
  );
}

/** Plantilla B: onboarding por pasos (cabecera blanca con chip de pasos y «Salir y continuar después»). */
export function OnboardingShell({ titulo, paso, total = 2, onSalir, children }) {
  return (
    <div className="min-h-screen bg-page">
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-line bg-surface px-4 py-3 md:px-8">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-600 text-xl font-extrabold text-white" aria-hidden="true">V</span>
          <span className="font-extrabold text-strong max-sm:sr-only">V-Learning</span>
        </div>
        <p className="font-bold text-strong">{titulo}</p>
        <div className="flex items-center gap-3">
          <span className="rounded-full border border-primary-300 bg-primary-100 px-3 py-1 text-sm font-bold text-primary-700">Paso {paso} de {total}</span>
          <button type="button" onClick={onSalir} className="flex min-h-11 items-center gap-1.5 rounded-lg px-3 font-bold text-primary-600 underline">
            <LogOut size={16} aria-hidden="true" />Salir y continuar después
          </button>
        </div>
      </header>
      <main id="contenido" tabIndex={-1} className="mx-auto max-w-[1340px] px-4 py-8 outline-none md:px-8">{children}</main>
    </div>
  );
}
