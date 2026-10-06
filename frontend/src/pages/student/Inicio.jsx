import { Link } from "react-router-dom";
import { ArrowRight, Clock, Lightbulb } from "lucide-react";
import { api } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { FORMATO_TEXTO, fmtDuracion, fmtNumero } from "../../utils/format.js";
import { Badge, Banner, Button, Card, Cargando, EstadoError, PageHeader, ProgressBar, Vacio } from "../../components/ui/index.jsx";

export default function EstudianteInicio() {
  const { usuario } = useAuth();
  const { datos: d, cargando, error, recargar } = useAsync(() => api.progreso.dashboard(), []);
  if (cargando && !d) return <Cargando texto="Preparando tu inicio…" />;
  if (error && !d) return <EstadoError mensaje={error} onReintentar={recargar} />;
  const nombre = (usuario.nombre ?? "").split(" ")[0];
  const enCurso = d.cursos.filter((c) => c.estado === "ACTIVA");

  return (
    <>
      <PageHeader titulo={`Qué bueno verte, ${nombre}`} subtitulo="Continúa donde lo dejaste." />
      {d.sinActividad && <Banner tono="info" className="mb-5">{d.mensaje || "Aún no tienes actividad. Elige un curso y empieza tu primera lección."}</Banner>}
      <div className="grid gap-6 xl:grid-cols-[2fr_1fr]">
        <section aria-labelledby="seguir">
          <h2 id="seguir" className="mb-3 text-xl">Continúa aprendiendo</h2>
          {enCurso.length === 0 ? (
            <Vacio titulo="No estás inscrito en ningún curso"><Button to="/cursos" icono={ArrowRight}>Explorar cursos</Button></Vacio>
          ) : (
            <ul className="grid gap-4">
              {enCurso.map((c) => (
                <Card as="li" key={c.cursoId} className="list-none">
                  <h3 className="mb-1 text-lg">{c.titulo}</h3>
                  <p className="mb-2 text-sm">{c.modulosCompletos} de {c.modulosTotales} módulos completos · {fmtDuracion(c.tiempoConsumidoSegundos)}</p>
                  <ProgressBar valor={Number(c.porcentaje)} etiqueta={`Avance en ${c.titulo}`} className="mb-3" />
                  <Button to={`/cursos/${c.cursoId}`} icono={ArrowRight}>Continuar<span className="sr-only"> {c.titulo}</span></Button>
                </Card>
              ))}
            </ul>
          )}
          {d.sugerencias.length > 0 && (
            <>
              <h2 className="mb-3 mt-8 flex items-center gap-2 text-xl"><Lightbulb size={20} aria-hidden="true" />Para reforzar</h2>
              <ul className="grid gap-3">
                {d.sugerencias.map((s) => (
                  <li key={s.contenidoId} className="rounded-opt border border-line bg-surface p-3">
                    <Link className="font-bold text-primary-600 underline" to={`/contenidos/${s.contenidoId}`}>{s.titulo}</Link>
                    <p className="text-sm"><Badge tono="primario">{FORMATO_TEXTO[s.formato]}</Badge> {s.curso} · {s.motivo}</p>
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
        <aside className="grid h-fit gap-4">
          <Card as="section" aria-labelledby="pend">
            <h2 id="pend" className="mb-2 flex items-center gap-2 text-xl"><Clock size={20} aria-hidden="true" />Pendientes</h2>
            {d.pendientes.length === 0 ? <p>No tienes pendientes. ¡Buen trabajo!</p> : (
              <ul className="grid gap-2">
                {d.pendientes.map((p) => (
                  <li key={`${p.tipo}-${p.id}`}>
                    <Link className="font-bold text-primary-600 underline" to={p.tipo === "EVALUACION" ? "/evaluaciones" : p.tipo === "TAREA" ? `/cursos/${p.cursoId}#m-${p.moduloId}` : `/contenidos/${p.id}`}>{p.titulo}</Link>
                    <span className="block text-sm">{p.tipo === "EVALUACION" ? "Evaluación" : p.tipo === "TAREA" ? "Tarea" : "Contenido"} · {p.curso}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>
          <Card as="section" aria-labelledby="res">
            <h2 id="res" className="mb-2 text-xl">Tu resumen</h2>
            <dl className="grid grid-cols-2 gap-3 text-sm">
              <div><dt>Contenidos completados</dt><dd className="text-2xl font-extrabold text-strong">{fmtNumero(d.totales.contenidosCompletados)}</dd></div>
              <div><dt>Evaluaciones y tareas</dt><dd className="text-2xl font-extrabold text-strong">{fmtNumero((d.totales.evaluacionesRealizadas ?? 0) + (d.totales.tareasCalificadas ?? 0))}</dd></div>
            </dl>
            <p className="mt-3"><Link className="font-bold text-primary-600 underline" to="/progreso">Ver mi progreso completo</Link></p>
          </Card>
        </aside>
      </div>
    </>
  );
}
