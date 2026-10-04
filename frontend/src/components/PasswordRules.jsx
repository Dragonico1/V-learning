import { Check, Circle } from "lucide-react";
import { REGLAS_PASSWORD as REGLAS } from "../utils/password.js";

/** Lista de requisitos con texto «Cumple» / «Falta»; no depende solo del color. */
export function PasswordRules({ valor }) {
  return (
    <ul className="mt-2 grid gap-1 text-sm" aria-label="Requisitos de la contraseña">
      {REGLAS.map(([texto, f]) => {
        const ok = f(valor);
        return (
          <li key={texto} className={`flex items-center gap-2 ${ok ? "font-semibold text-ok-fg" : "text-body"}`}>
            {ok ? <Check size={16} aria-hidden="true" /> : <Circle size={16} aria-hidden="true" />}
            <span>{texto}</span><span className="sr-only">{ok ? ": cumple" : ": falta"}</span>
          </li>
        );
      })}
    </ul>
  );
}
