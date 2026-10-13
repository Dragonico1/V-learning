import { Bot } from "lucide-react";
import { useTutor } from "../../context/TutorContext.jsx";
import { cx } from "../../utils/cx.js";

export function TutorFlotante({ conPanel = false }) {
  const { abierto, alternar } = useTutor();
  if (abierto) return null;
  return (
    <button
      type="button"
      onClick={alternar}
      aria-label="Abrir Tutor IA"
      aria-expanded="false"
      title="Tutor IA (Alt+T)"
      className={cx(
        "fixed bottom-6 right-6 z-30 flex cursor-pointer h-14 w-14 items-center justify-center rounded-full bg-primary-600 text-white shadow-lg transition-colors hover:bg-primary-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-600",
        conPanel && "xl:right-[324px]",
      )}
    >
      <Bot size={26} strokeWidth={1.9} aria-hidden="true" />
    </button>
  );
}
