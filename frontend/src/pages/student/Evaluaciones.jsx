import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { useTexto } from "../../utils/useTexto.js";
import { TIPO_EVAL_TEXTO, fmtFecha, fmtPct } from "../../utils/format.js";
import {
  Badge,
  Banner,
  Button,
  Card,
  Cargando,
  EstadoError,
  PageHeader,
  Vacio,
} from "../../components/ui/index.jsx";
import TareaEstudianteCard from "../../components/TareaEstudianteCard.jsx";
import VolverACursos from "../../components/VolverACursos.jsx";

async function cargarTodas() {
  const mios = await api.cursos.mios();
  const salida = [];
  for (const cur of mios) {
    const det = await api.cursos.detalle(cur.id);
    for (const m of det.modulos) {
      const [evs, tareas] = await Promise.all([
        m.evaluaciones.length
          ? api.evaluaciones.delModulo(m.id)
          : Promise.resolve([]),
        api.tareas.delModulo(m.id),
      ]);
      if (!evs.length && !tareas.length) continue;
      salida.push({
        cursoId: det.id,
        curso: det.titulo,
        moduloId: m.id,
        modulo: m.titulo,
        evaluaciones: evs,
        tareas,
      });
    }
  }
  return salida;
}

/** RF-007: lista de evaluaciones de mis cursos. */
export default function Evaluaciones() {
  const t = useTexto();
  const nav = useNavigate();
  const { avisar } = useNotif();
  const { datos, cargando, error, recargar } = useAsync(cargarTodas, []);
  const [iniciando, setIniciando] = useState(null);

  if (cargando && !datos) return <Cargando texto={t("Cargando evaluaciones…")} />;
  if (error && !datos)
    return <EstadoError mensaje={error} onReintentar={recargar} />;

  const iniciar = async (e) => {
    setIniciando(e.id);
    try {
      // El servidor reanuda el intento en curso si existe, así que siempre se llama a iniciar.
      const intento = await api.evaluaciones.iniciar(e.id);
      try {
        sessionStorage.setItem(`vl.eval.${intento.id}`, String(e.id));
      } catch {
        /* sin almacenamiento */
      }
      nav(`/intentos/${intento.id}`, { state: { intento } });
    } catch (err) {
      avisar(mensajeError(err), "error");
    } finally {
      setIniciando(null);
    }
  };

  return (
    <>
      <VolverACursos />
      <PageHeader
        titulo={t("Evaluaciones y tareas")}
        subtitulo={t("Presenta las evaluaciones y entrega las tareas de tus cursos. Tus respuestas se guardan solas.")}
      />
      {datos.length === 0 ? (
        <Vacio titulo={t("Aún no hay evaluaciones ni tareas")}>
          {t("Aparecerán aquí cuando tus cursos las incluyan.")}
        </Vacio>
      ) : (
        datos.map((g, i) => (
          <section key={i} className="mb-6" aria-labelledby={`g-${i}`}>
            <h2 id={`g-${i}`} className="mb-2 text-xl">
              {g.curso} · {g.modulo}
            </h2>
            <ul className="grid gap-3">
              {g.evaluaciones.map((e) => (
                <Card as="li" key={e.id} className="list-none">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <h3 className="text-lg">{e.titulo}</h3>
                      {e.descripcion && <p>{e.descripcion}</p>}
                      <p className="mt-1 flex flex-wrap items-center gap-2 text-sm">
                        <Badge tono="primario">{t(TIPO_EVAL_TEXTO[e.tipo])}</Badge>
                        {e.tiempoLimiteMinutos ? (
                          <span>{e.tiempoLimiteMinutos} min</span>
                        ) : (
                          <span>{t("Sin límite de tiempo")}</span>
                        )}
                        {e.fechaLimite &&
                          (e.vencida ? (
                            <Badge tono="err">{t("Plazo vencido")}</Badge>
                          ) : (
                            <span>{t("Fecha límite", "Último día")}: {fmtFecha(e.fechaLimite)}</span>
                          ))}
                        <span>{e.intentos} {e.intentos === 1 ? t("intento", "vez") : t("intentos", "veces")}</span>
                        {e.mejorPorcentaje != null && (
                          <span>
                            {t("Mejor resultado", "Tu mejor nota")}: {fmtPct(e.mejorPorcentaje)}
                          </span>
                        )}
                      </p>
                    </div>
                    <div className="flex flex-wrap items-start gap-2">
                      <Button
                        variante="secondary"
                        to={`/cursos/${g.cursoId}#m-${g.moduloId}`}
                      >
                        {t("Ir al módulo")}
                        <span className="sr-only"> de {e.titulo}</span>
                      </Button>
                      <Button
                        cargando={iniciando === e.id}
                        disabled={!e.habilitada || !e.calificable}
                        onClick={() => iniciar(e)}
                      >
                        {e.intentoEnProgresoId
                          ? t("Reanudar")
                          : e.intentos > 0
                            ? t("Intentar de nuevo")
                            : t("Comenzar")}
                        <span className="sr-only"> {e.titulo}</span>
                      </Button>
                    </div>
                  </div>
                  {!e.habilitada && e.motivoNoHabilitada && (
                    <Banner tono="info" className="mt-3">
                      {e.motivoNoHabilitada}
                    </Banner>
                  )}
                  {!e.calificable && e.aviso && (
                    <Banner tono="warn" className="mt-3">
                      {e.aviso}
                    </Banner>
                  )}
                </Card>
              ))}
              {g.tareas.map((tarea) => (
                <TareaEstudianteCard
                  key={`t${tarea.id}`}
                  tarea={tarea}
                  onCambio={recargar}
                  acciones={
                    <Button
                      variante="secondary"
                      to={`/cursos/${g.cursoId}#m-${g.moduloId}`}
                    >
                      {t("Ir al módulo")}
                      <span className="sr-only"> de {tarea.titulo}</span>
                    </Button>
                  }
                />
              ))}
            </ul>
          </section>
        ))
      )}
    </>
  );
}
