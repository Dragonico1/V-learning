import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { TIPO_EVAL_TEXTO, fmtPct } from "../../utils/format.js";
import { Badge, Banner, Button, Card, Cargando, EstadoError, PageHeader, Vacio } from "../../components/ui/index.jsx";

async function cargarTodas() {
  const mios = (await api.cursos.mios());
  const salida = [];
  for (const cur of mios) {
    const det = await api.cursos.detalle(cur.id);
    for (const m of det.modulos) {
      if (!m.evaluaciones.length) continue;
      const evs = await api.evaluaciones.delModulo(m.id);
      salida.push({ curso: det.titulo, modulo: m.titulo, evaluaciones: evs });
    }
  }
  return salida;
}

/** RF-007: lista de evaluaciones de mis cursos. */
export default function Evaluaciones() {
  const nav = useNavigate();
  const { avisar } = useNotif();
  const { datos, cargando, error, recargar } = useAsync(cargarTodas, []);
  const [iniciando, setIniciando] = useState(null);

  if (cargando && !datos) return <Cargando texto="Cargando evaluaciones…" />;
  if (error && !datos) return <EstadoError mensaje={error} onReintentar={recargar} />;

  const iniciar = async (e) => {
    setIniciando(e.id);
    try {
      // El servidor reanuda el intento en curso si existe, así que siempre se llama a iniciar.
      const intento = await api.evaluaciones.iniciar(e.id);
      try { sessionStorage.setItem(`vl.eval.${intento.id}`, String(e.id)); } catch { /* sin almacenamiento */ }
      nav(`/intentos/${intento.id}`, { state: { intento } });
    } catch (err) { avisar(mensajeError(err), "error"); } finally { setIniciando(null); }
  };

  return (
    <>
      <PageHeader titulo="Evaluaciones" subtitulo="Presenta las evaluaciones de tus cursos. Tus respuestas se guardan solas." />
      {datos.length === 0 ? <Vacio titulo="Aún no hay evaluaciones disponibles">Aparecerán aquí cuando tus cursos las incluyan.</Vacio> : datos.map((g, i) => (
        <section key={i} className="mb-6" aria-labelledby={`g-${i}`}>
          <h2 id={`g-${i}`} className="mb-2 text-xl">{g.curso} · {g.modulo}</h2>
          <ul className="grid gap-3">
            {g.evaluaciones.map((e) => (
              <Card as="li" key={e.id} className="list-none">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <h3 className="text-lg">{e.titulo}</h3>
                    {e.descripcion && <p>{e.descripcion}</p>}
                    <p className="mt-1 flex flex-wrap items-center gap-2 text-sm">
                      <Badge tono="primario">{TIPO_EVAL_TEXTO[e.tipo]}</Badge>
                      {e.tiempoLimiteMinutos ? <span>{e.tiempoLimiteMinutos} min</span> : <span>Sin límite de tiempo</span>}
                      <span>{e.intentos} intento(s)</span>
                      {e.mejorPorcentaje != null && <span>Mejor resultado: {fmtPct(e.mejorPorcentaje)}</span>}
                    </p>
                  </div>
                  <Button cargando={iniciando === e.id} disabled={!e.habilitada || !e.calificable} motivo={!e.habilitada ? e.motivoNoHabilitada : !e.calificable ? e.aviso : undefined}
                    onClick={() => iniciar(e)}>{e.intentoEnProgresoId ? "Reanudar" : e.intentos > 0 ? "Intentar de nuevo" : "Comenzar"}<span className="sr-only"> {e.titulo}</span></Button>
                </div>
                {!e.habilitada && e.motivoNoHabilitada && <Banner tono="info" className="mt-3">{e.motivoNoHabilitada}</Banner>}
                {!e.calificable && e.aviso && <Banner tono="warn" className="mt-3">{e.aviso}</Banner>}
              </Card>
            ))}
          </ul>
        </section>
      ))}
    </>
  );
}
