import { useState } from "react";
import { Award } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { fmtDia } from "../../utils/format.js";
import { Banner, Card, Cargando, EstadoError, PageHeader, ProgressBar, SelectField, Toggle, Vacio } from "../../components/ui/index.jsx";

function Ranking({ cursos }) {
  const [cursoId, setCursoId] = useState("");
  const r = useAsync(() => (cursoId ? api.logros.ranking(cursoId) : Promise.resolve(null)), [cursoId]);
  return (
    <Card as="section" aria-labelledby="rk" className="mt-6">
      <h2 id="rk" className="mb-3 text-xl">Clasificación del curso</h2>
      <SelectField label="Curso" className="mb-4 max-w-md" value={cursoId} onChange={(e) => setCursoId(e.target.value)}>
        <option value="">Elige un curso…</option>
        {cursos.map((c) => <option key={c.id} value={c.id}>{c.titulo}</option>)}
      </SelectField>
      {cursoId && r.cargando && !r.datos && <Cargando />}
      {r.error && <EstadoError mensaje={r.error} onReintentar={r.recargar} />}
      {r.datos && (
        <>
          {r.datos.modoPrivado && <Banner tono="info" className="mb-3">Elegiste no aparecer en la clasificación. Solo tú ves tu posición{r.datos.miPosicion ? `: ${r.datos.miPosicion}.` : "."}</Banner>}
          {r.datos.posiciones.length === 0 ? <p>Todavía no hay posiciones.</p> : (
            <table className="w-full max-w-xl text-left">
              <caption className="sr-only">Clasificación por puntos</caption>
              <thead className="bg-subtle text-sm"><tr><th scope="col" className="p-3">Posición</th><th scope="col" className="p-3">Nombre</th><th scope="col" className="p-3">Puntos</th></tr></thead>
              <tbody>{r.datos.posiciones.map((p) => (
                <tr key={p.posicion} className={`border-t border-line ${p.esYo ? "bg-primary-100 font-bold" : ""}`}><td className="p-3">{p.posicion}</td><td className="p-3">{p.nombre}{p.esYo ? " (tú)" : ""}</td><td className="p-3">{p.puntos}</td></tr>
              ))}</tbody>
            </table>
          )}
        </>
      )}
    </Card>
  );
}

/** RF-009: puntos, nivel, insignias y clasificación. */
export default function Logros() {
  const { avisar } = useNotif();
  const { datos: l, cargando, error, recargar, setDatos } = useAsync(() => api.logros.ver(), []);
  const cursos = useAsync(() => api.cursos.mios(), []);
  if (cargando && !l) return <Cargando texto="Cargando tus logros…" />;
  if (error && !l) return <EstadoError mensaje={error} onReintentar={recargar} />;

  const privacidad = async (v) => {
    try { await api.logros.privacidad(v); setDatos((x) => ({ ...x, mostrarEnRanking: v })); avisar(v ? "Apareces en la clasificación." : "Ya no apareces en la clasificación.", "ok"); }
    catch (e) { avisar(mensajeError(e), "error"); }
  };

  return (
    <>
      <PageHeader titulo="Logros" subtitulo="Gana puntos al avanzar y desbloquea insignias." />
      <Card className="mb-6 max-w-2xl">
        <p className="text-sm">Nivel</p>
        <p className="text-4xl font-extrabold text-strong">{l.nivel}</p>
        <p className="mb-2">{l.puntos} puntos · siguiente nivel en {l.puntosSiguienteNivel} puntos</p>
        <ProgressBar valor={l.progresoNivelPorcentaje} etiqueta="Avance al siguiente nivel" />
      </Card>
      <section aria-labelledby="ins" className="mb-6">
        <h2 id="ins" className="mb-3 text-xl">Insignias obtenidas</h2>
        {l.insignias.length === 0 ? <Vacio titulo="Aún no tienes insignias">Completa contenidos y evaluaciones para ganarlas.</Vacio> : (
          <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {l.insignias.map((i) => (
              <Card as="li" key={i.id} className="list-none"><Award size={28} aria-hidden="true" className="mb-2 text-primary-600" /><p className="font-bold text-strong">{i.nombre}</p>
                {i.descripcion && <p className="text-sm">{i.descripcion}</p>}<p className="mt-1 text-sm">Obtenida el {fmtDia(i.fechaObtencion)}</p></Card>
            ))}
          </ul>
        )}
      </section>
      {l.pendientes.length > 0 && (
        <section aria-labelledby="pen">
          <h2 id="pen" className="mb-3 text-xl">Próximas insignias</h2>
          <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {l.pendientes.map((i) => (
              <Card as="li" key={i.id} className="list-none"><p className="font-bold text-strong">{i.nombre}</p>{i.descripcion && <p className="text-sm">{i.descripcion}</p>}
                <p className="mt-1 text-sm">Te faltan <strong>{i.faltan} puntos</strong> (necesitas {i.puntosRequeridos}).</p></Card>
            ))}
          </ul>
        </section>
      )}
      <Card className="mt-6 max-w-2xl">
        <Toggle label="Aparecer en la clasificación" descripcion="Si lo desactivas, otros estudiantes no verán tu nombre." checked={l.mostrarEnRanking} onChange={privacidad} />
      </Card>
      <Ranking cursos={cursos.datos ?? []} />
    </>
  );
}
