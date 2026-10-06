import { Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";

/** Enlace de regreso a la sección de cursos (pantallas del menú lateral). */
export default function VolverACursos() {
  return (
    <p className="mb-3">
      <Link to="/cursos" className="inline-flex min-h-11 items-center gap-1.5 font-bold text-primary-600 underline">
        <ArrowLeft size={16} aria-hidden="true" />Volver a cursos
      </Link>
    </p>
  );
}
