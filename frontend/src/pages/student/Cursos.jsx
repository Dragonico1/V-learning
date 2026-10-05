import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { api, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { Badge, Button, Card, Cargando, EstadoError, PageHeader, ProgressBar, Vacio } from "../../components/ui/index.jsx";

export default function EstudianteCursos() {
  const [params] = useSearchParams();
  const q = (params.get("q") ?? "").trim().toLowerCase();
  const { avisar } = useNotif();
  const { datos, cargando, error, recargar } = useAsync(() => api.cursos.catalogo(), []);
  const [inscribiendo, setInscribiendo] = useState(null);

  if (cargando && !datos) return <Cargando texto="Cargando cursos…" />;
  if (error && !datos) return <EstadoError mensaje={error} onReintentar={recargar} />;
  const filtrar = (l) => l.filter((c) => !q || `${c.titulo} ${c.instructor ?? ""} ${c.descripcion ?? ""}`.toLowerCase().includes(q));
  const mios = filtrar(datos.filter((c) => c.inscrito));
  const otros = filtrar(datos.filter((c) => !c.inscrito));

  const inscribirme = async (c) => {
    setInscribiendo(c.id);
    try { await api.cursos.inscribirme(c.id); avisar(`Te inscribiste en «${c.titulo}».`, "ok"); recargar(); }
    catch (e) { avisar(mensajeError(e), "error"); } finally { setInscribiendo(null); }
  };

  return (
    <>
      <PageHeader titulo="Mis cursos" subtitulo="Tus cursos y el catálogo disponible." />
      {q && <p className="mb-3" role="status">Resultados para «{params.get("q")}»: {mios.length + otros.length}.</p>}
      <section aria-labelledby="m" className="mb-8">
        <h2 id="m" className="mb-3 text-xl">Inscrito</h2>
        {mios.length === 0 ? <Vacio titulo="Aún no estás inscrito en cursos" /> : (
          <ul className="grid gap-4 md:grid-cols-2">
            {mios.map((c) => (
              <Card as="li" key={c.id} className="list-none">
                <h3 className="text-lg">{c.titulo}</h3>
                <p className="text-sm">Instructor: {c.instructor ?? "—"}</p>
                <ProgressBar valor={Number(c.porcentaje ?? 0)} etiqueta={`Avance en ${c.titulo}`} className="my-3" />
                <Button to={`/cursos/${c.id}`}>Entrar<span className="sr-only"> a {c.titulo}</span></Button>
              </Card>
            ))}
          </ul>
        )}
      </section>
      <section aria-labelledby="c">
        <h2 id="c" className="mb-3 text-xl">Catálogo</h2>
        {otros.length === 0 ? <Vacio titulo="No hay más cursos disponibles por ahora" /> : (
          <ul className="grid gap-4 md:grid-cols-2">
            {otros.map((c) => (
              <Card as="li" key={c.id} className="list-none">
                <Badge tono="gris" className="mb-2">Disponible</Badge>
                <h3 className="text-lg">{c.titulo}</h3>
                <p className="text-sm">Instructor: {c.instructor ?? "—"}</p>
                {c.descripcion && <p className="my-2 line-clamp-3">{c.descripcion}</p>}
                <Button cargando={inscribiendo === c.id} onClick={() => inscribirme(c)}>Inscribirme<span className="sr-only"> en {c.titulo}</span></Button>
              </Card>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
