import { Link } from "react-router-dom";
import { BookOpen, FileText, MessageSquare, Pencil, Plus } from "lucide-react";
import { api } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { ESTADO_CURSO_TEXTO } from "../../utils/format.js";
import { Badge, Button, Card, Cargando, EstadoError, PageHeader, Vacio } from "../../components/ui/index.jsx";
import { tonoEstadoCurso } from "./constantes.js";

const ESTADOS = ["PUBLICADO", "BORRADOR", "ARCHIVADO"];

/** Panel del instructor: resumen de sus cursos y accesos rápidos. */
export default function Inicio() {
  const { usuario } = useAuth();
  const { datos: cursos, cargando, error, recargar } = useAsync(() => api.instructor.cursos(), []);

  const primerNombre = usuario?.nombre?.split(" ")[0] ?? "";
  const conteo = (estado) => (cursos ?? []).filter((c) => c.estado === estado).length;

  return (
    <>
      <PageHeader eyebrow="Instructor" titulo={primerNombre ? `Hola, ${primerNombre}` : "Hola"}
        subtitulo="Aquí ves el estado de tus cursos y puedes seguir creando contenido accesible." />

      {cargando && !cursos && <Cargando texto="Cargando tus cursos…" />}
      {error && <EstadoError mensaje={error} onReintentar={recargar} />}

      {cursos && (
        <>
          <section aria-labelledby="h-resumen">
            <h2 id="h-resumen" className="mb-3 text-xl">Tus cursos por estado</h2>
            <ul className="grid gap-4 sm:grid-cols-3">
              {ESTADOS.map((e) => (
                <li key={e}>
                  <Card className="h-full">
                    <p className="text-4xl font-extrabold tabular-nums text-strong">{conteo(e)}</p>
                    <p className="mt-1 font-bold text-body">{ESTADO_CURSO_TEXTO[e]}{conteo(e) === 1 ? "" : "s"}</p>
                  </Card>
                </li>
              ))}
            </ul>
          </section>

          <section aria-labelledby="h-accesos" className="mt-8">
            <h2 id="h-accesos" className="mb-3 text-xl">Accesos rápidos</h2>
            <div className="flex flex-wrap gap-3">
              <Button to="/cursos" icono={Plus}>Crear curso</Button>
              <Button to="/informes" variante="secondary" icono={FileText}>Informes</Button>
              <Button to="/comunidad" variante="secondary" icono={MessageSquare}>Comunidad</Button>
            </div>
          </section>

          <section aria-labelledby="h-lista" className="mt-8">
            <h2 id="h-lista" className="mb-3 text-xl">Mis cursos</h2>
            {cursos.length === 0 ? (
              <Vacio icono={BookOpen} titulo="Todavía no tienes cursos">
                <p>Crea tu primer curso desde «Crear curso». Empieza como borrador y lo publicas cuando esté listo.</p>
              </Vacio>
            ) : (
              <ul className="grid gap-3">
                {cursos.map((c) => (
                  <li key={c.id}>
                    <Card className="flex flex-wrap items-center justify-between gap-3 !p-4">
                      <div className="min-w-0">
                        <p className="font-bold text-strong">{c.titulo}</p>
                        <Badge tono={tonoEstadoCurso(c.estado)} className="mt-1">{ESTADO_CURSO_TEXTO[c.estado] ?? c.estado}</Badge>
                      </div>
                      <Link to={`/cursos/${c.id}/editar`} className="inline-flex min-h-11 items-center gap-2 rounded-ctl border-[1.5px] border-primary-600 px-4 font-bold text-primary-600 hover:bg-primary-100">
                        <Pencil size={18} aria-hidden="true" /><span>Editar<span className="sr-only"> el curso {c.titulo}</span></span>
                      </Link>
                    </Card>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </>
  );
}
