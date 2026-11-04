import { useState } from "react";
import { Download } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { useTexto } from "../../utils/useTexto.js";
import { CLASIFICACION_TEXTO, fmtDuracion, fmtNumero, fmtPct } from "../../utils/format.js";
import { Badge, Banner, Button, Card, Cargando, EstadoError, PageHeader, ProgressBar } from "../../components/ui/index.jsx";
import VolverACursos from "../../components/VolverACursos.jsx";

const TONO = { EXCELENTE: "ok", ACEPTABLE: "warn", INSUFICIENTE: "err" };

/** RF-006: dashboard de progreso. Cada gráfica tiene su tabla equivalente. */
export default function MiProgreso() {
  const t = useTexto();
  const { avisar } = useNotif();
  const { datos: d, cargando, error, recargar } = useAsync(() => api.progreso.dashboard(), []);
  const [bajando, setBajando] = useState(null);
  if (cargando && !d) return <Cargando texto={t("Cargando tu progreso…", "Cargando tu avance…")} />;
  if (error && !d) return <EstadoError mensaje={error} onReintentar={recargar} />;

  const bajar = async (f) => {
    setBajando(f);
    try { await api.progreso.exportar(f); avisar(t("Descarga lista.", "Tu archivo está listo."), "ok"); } catch (e) { avisar(mensajeError(e), "error"); } finally { setBajando(null); }
  };

  return (
    <>
      <VolverACursos />
      <PageHeader titulo={t("Mi progreso")} subtitulo={t("Tu avance, tus calificaciones y el tiempo que dedicas.")}
        acciones={<>
          <Button variante="secondary" icono={Download} cargando={bajando === "PDF"} onClick={() => bajar("PDF")}>{t("Exportar PDF")}</Button>
          <Button variante="secondary" icono={Download} cargando={bajando === "EXCEL"} onClick={() => bajar("EXCEL")}>{t("Exportar Excel")}</Button>
        </>} />
      {d.sinActividad ? (
        <Banner tono="info">{d.mensaje || t("Todavía no tienes actividad. ¡Empieza tu primera lección y aquí verás tu avance!")}</Banner>
      ) : (
        <>
          <dl className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {[[t("Tiempo de estudio", "Tiempo estudiando"), fmtDuracion(d.totales.tiempoTotalSegundos)], [t("Contenidos completados", "Lecciones terminadas"), fmtNumero(d.totales.contenidosCompletados)],
              [t("Evaluaciones y tareas calificadas", "Pruebas y trabajos calificados"), fmtNumero((d.totales.evaluacionesRealizadas ?? 0) + (d.totales.tareasCalificadas ?? 0))], [t("Promedio de calificación", "Nota promedio"), d.totales.promedioCalificacion != null ? fmtPct(d.totales.promedioCalificacion) : "—"]].map(([k, v]) => (
              <Card key={k}><dt className="text-sm">{k}</dt><dd className="text-3xl font-extrabold text-strong">{v}</dd></Card>
            ))}
          </dl>
          <section aria-labelledby="cp" className="mb-6">
            <h2 id="cp" className="mb-3 text-xl">{t("Avance por curso", "Progreso por curso")}</h2>
            <ul className="mb-4 grid gap-3">
              {d.cursos.map((c) => (
                <Card as="li" key={c.cursoId} className="list-none">
                  <p className="mb-2 font-bold text-strong">{c.titulo}</p>
                  <ProgressBar valor={Number(c.porcentaje)} etiqueta={`${t("Avance en", "Progreso en")} ${c.titulo}`} />
                </Card>
              ))}
            </ul>
            <div className="overflow-x-auto rounded-card border border-line bg-surface">
              <table className="w-full min-w-[620px] text-left">
                <caption className="sr-only">{t("Detalle del avance por curso")}</caption>
                <thead className="bg-subtle text-sm"><tr><th scope="col" className="p-3">Curso</th><th scope="col" className="p-3">{t("Avance", "Progreso")}</th><th scope="col" className="p-3">{t("Módulos", "Partes")}</th><th scope="col" className="p-3">{t("Contenidos", "Lecciones")}</th><th scope="col" className="p-3">Tiempo</th></tr></thead>
                <tbody>{d.cursos.map((c) => (
                  <tr key={c.cursoId} className="border-t border-line"><th scope="row" className="p-3 font-normal">{c.titulo}</th><td className="p-3">{fmtPct(c.porcentaje)}</td>
                    <td className="p-3">{c.modulosCompletos}/{c.modulosTotales}</td><td className="p-3">{c.contenidosCompletados}</td><td className="p-3">{fmtDuracion(c.tiempoConsumidoSegundos)}</td></tr>
                ))}</tbody>
              </table>
            </div>
          </section>
          <section aria-labelledby="cal">
            <h2 id="cal" className="mb-3 text-xl">{t("Calificaciones", "Notas")}</h2>
            {d.calificaciones.length === 0 ? <p>{t("Aún no has presentado evaluaciones ni tienes tareas calificadas.")}</p> : (
              <div className="overflow-x-auto rounded-card border border-line bg-surface">
                <table className="w-full min-w-[620px] text-left">
                  <caption className="sr-only">{t("Mejor resultado por evaluación y nota de cada tarea calificada; el promedio incluye ambas")}</caption>
                  <thead className="bg-subtle text-sm"><tr><th scope="col" className="p-3">{t("Actividad", "Trabajo o prueba")}</th><th scope="col" className="p-3">Curso</th><th scope="col" className="p-3">{t("Resultado", "Nota")}</th><th scope="col" className="p-3">{t("Intentos", "Veces")}</th><th scope="col" className="p-3">{t("Clasificación", "Nivel")}</th></tr></thead>
                  <tbody>{d.calificaciones.map((c) => (
                    <tr key={`${c.tipo}-${c.evaluacionId}`} className="border-t border-line"><th scope="row" className="p-3 font-normal">{c.evaluacion} <Badge tono={c.tipo === "TAREA" ? "tutor" : "primario"}>{c.tipo === "TAREA" ? t("Tarea") : t("Evaluación")}</Badge></th><td className="p-3">{c.curso}</td><td className="p-3">{fmtPct(c.mejorPorcentaje)}</td><td className="p-3">{c.tipo === "TAREA" ? "—" : c.intentos}</td>
                      <td className="p-3"><Badge tono={TONO[c.clasificacion]}>{t(CLASIFICACION_TEXTO[c.clasificacion])}</Badge></td></tr>
                  ))}</tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
    </>
  );
}
