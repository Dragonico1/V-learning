import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { UserPlus } from "lucide-react";
import { api } from "../../api/client.js";
import { useAsync } from "../../utils/useAsync.js";
import { ESTADO_CURSO_TEXTO } from "../../utils/format.js";
import { Badge, Banner, Button, Card, Cargando, Dialogo, EstadoError, PageHeader, Vacio } from "../../components/ui/index.jsx";
import InscribirEstudiantes from "../../components/InscribirEstudiantes.jsx";
import { tonoEstadoCurso } from "../instructor/constantes.js";

export default function AdminCursos() {
  const [params] = useSearchParams();
  const q = (params.get("q") ?? "").trim().toLowerCase();
  const { datos, cargando, error, recargar } = useAsync(() => api.admin.cursos(), []);
  const [curso, setCurso] = useState(null);

  if (cargando && !datos) return <Cargando texto="Cargando cursos…" />;
  if (error && !datos) return <EstadoError mensaje={error} onReintentar={recargar} />;
  const lista = datos.filter((c) => !q || `${c.titulo} ${c.instructor ?? ""}`.toLowerCase().includes(q));

  return (
    <>
      <PageHeader eyebrow="Administración" titulo="Cursos" subtitulo="Consulta todos los cursos e inscribe estudiantes en los publicados." />
      {q && <p className="mb-3" role="status">Filtrando por «{params.get("q")}»: {lista.length} resultado(s).</p>}
      {lista.length === 0 ? <Vacio titulo="No hay cursos para mostrar" /> : (
        <ul className="grid gap-4 md:grid-cols-2">
          {lista.map((c) => (
            <Card as="li" key={c.id} className="list-none">
              <div className="mb-2 flex flex-wrap items-center gap-2"><Badge tono={tonoEstadoCurso(c.estado)}>{ESTADO_CURSO_TEXTO[c.estado]}</Badge></div>
              <h2 className="text-xl">{c.titulo}</h2>
              <p className="text-sm">Instructor: {c.instructor ?? "—"}</p>
              {c.descripcion && <p className="mt-2 line-clamp-3">{c.descripcion}</p>}
              <div className="mt-4 flex flex-wrap gap-2">
                <Button icono={UserPlus} disabled={c.estado !== "PUBLICADO"} motivo={c.estado !== "PUBLICADO" ? "Solo se inscribe en cursos publicados." : undefined}
                  onClick={() => setCurso(c)}>Inscribir estudiantes<span className="sr-only"> en {c.titulo}</span></Button>
                <Button variante="secondary" to={`/comunidad/${c.id}`}>Comunidad<span className="sr-only"> de {c.titulo}</span></Button>
              </div>
            </Card>
          ))}
        </ul>
      )}
      <Dialogo abierto={Boolean(curso)} onCerrar={() => setCurso(null)} titulo={`Inscribir en: ${curso?.titulo ?? ""}`}>
        {curso && (<>
          <Banner tono="info" className="mb-4">Pega los correos institucionales de los estudiantes.</Banner>
          <InscribirEstudiantes inscribir={(correos) => api.admin.inscribir(curso.id, correos)} />
        </>)}
      </Dialogo>
    </>
  );
}
