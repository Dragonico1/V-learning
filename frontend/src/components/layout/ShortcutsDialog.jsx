import { Dialogo } from "../ui/index.jsx";

const ATAJOS = [
  ["?", "Mostrar esta lista de atajos"],
  ["Ctrl + K  /  ⌘ + K", "Ir al buscador"],
  ["Alt + T", "Abrir o cerrar el Tutor IA (estudiantes)"],
  ["Tab / Mayús + Tab", "Moverse entre elementos: menú, barra superior, contenido y Tutor"],
  ["Esc", "Cerrar paneles y diálogos"],
  ["Espacio (en la lección)", "Reproducir o pausar el video"],
  ["C (en la lección)", "Activar o desactivar los subtítulos"],
  ["← / → (en la lección)", "Retroceder o avanzar 10 segundos"],
];

export function ShortcutsDialog({ abierto, onCerrar }) {
  return (
    <Dialogo abierto={abierto} onCerrar={onCerrar} titulo="Atajos de teclado">
      <p className="mb-3 text-sm text-muted">Los atajos de la lección solo funcionan cuando el reproductor tiene el foco, para no chocar con tu lector de pantalla.</p>
      <dl className="divide-y divide-line">
        {ATAJOS.map(([tecla, texto]) => (
          <div key={tecla} className="flex items-center justify-between gap-4 py-2.5">
            <dt><kbd className="rounded-md border border-line bg-subtle px-2 py-1 font-mono text-sm font-bold text-strong">{tecla}</kbd></dt>
            <dd className="text-right">{texto}</dd>
          </div>
        ))}
      </dl>
    </Dialogo>
  );
}
