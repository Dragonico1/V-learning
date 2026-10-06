import { api } from "../../api/client.js";
import { useAsync } from "../../utils/useAsync.js";
import TareaEstudianteCard from "../../components/TareaEstudianteCard.jsx";

/** Tareas calificables de un módulo dentro del detalle del curso (estudiante). */
export default function TareasDelModulo({ moduloId }) {
  const { datos, recargar } = useAsync(() => api.tareas.delModulo(moduloId), [moduloId]);
  if (!datos || datos.length === 0) return null;
  return (
    <div className="mt-4 border-t border-line pt-4">
      <h3 className="mb-2 text-lg">Tareas</h3>
      <ul className="grid gap-3">
        {datos.map((t) => <TareaEstudianteCard key={t.id} tarea={t} onCambio={recargar} />)}
      </ul>
    </div>
  );
}
