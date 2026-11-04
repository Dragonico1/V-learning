import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { CheckCircle2, XCircle } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { useTexto } from "../../utils/useTexto.js";
import { CLASIFICACION_TEXTO, FORMATO_TEXTO, fmtPct } from "../../utils/format.js";
import { Badge, Banner, Button, Card, Cargando, EstadoError, PageHeader } from "../../components/ui/index.jsx";

const TONO = { EXCELENTE: "ok", ACEPTABLE: "warn", INSUFICIENTE: "err" };

/** RF-008: retroalimentación del intento. */
export default function ResultadoEvaluacion() {
  const t = useTexto();
  const { id } = useParams();
  const nav = useNavigate();
  const { avisar } = useNotif();
  const { datos: r, cargando, error, recargar } = useAsync(async () => {
    const res = await api.evaluaciones.resultado(id);
    window.dispatchEvent(new Event("vl:puntos"));
    return res;
  }, [id]);
  const [reintentando, setReintentando] = useState(false);

  if (cargando && !r) return <Cargando texto={t("Calculando tu resultado…", "Calculando tu nota…")} />;
  if (error && !r) return <EstadoError mensaje={error} onReintentar={recargar} />;

  const repetir = async () => {
    setReintentando(true);
    try {
      const i = await api.evaluaciones.iniciar(r.evaluacionId);
      try { sessionStorage.setItem(`vl.eval.${i.id}`, String(r.evaluacionId)); } catch { /* ignorar */ }
      nav(`/intentos/${i.id}`, { state: { intento: i } });
    } catch (e) { avisar(mensajeError(e), "error"); } finally { setReintentando(false); }
  };

  return (
    <>
      <PageHeader eyebrow={t("Resultado", "Nota")} titulo={r.titulo} />
      <Card className="mb-6 max-w-2xl">
        <p className="text-4xl font-extrabold text-strong">{fmtPct(r.porcentaje)}</p>
        <p className="mb-2">{r.puntaje} de {r.puntajeMaximo} puntos</p>
        <Badge tono={TONO[r.clasificacion]}>{t(CLASIFICACION_TEXTO[r.clasificacion])}</Badge>
        <p className="mt-3 text-lg">{r.mensaje}</p>
        {r.mejorPorcentajePrevio != null && <p className="mt-2 text-sm">{r.mejoro ? `${t("Mejoraste respecto a tu mejor resultado anterior", "Mejoraste. Tu mejor nota anterior fue")} (${fmtPct(r.mejorPorcentajePrevio)}).` : `${t("Tu mejor resultado anterior fue", "Tu mejor nota anterior fue")} ${fmtPct(r.mejorPorcentajePrevio)}.`}</p>}
        <div className="mt-4 flex flex-wrap gap-3">
          <Button cargando={reintentando} onClick={repetir}>{t("Intentar de nuevo")}</Button>
          <Button variante="secondary" to="/evaluaciones">{t("Volver a evaluaciones")}</Button>
        </div>
      </Card>
      {r.recomendaciones.length > 0 && (
        <section className="mb-6" aria-labelledby="rec">
          <h2 id="rec" className="mb-2 text-xl">{t("Para reforzar")}</h2>
          <ul className="grid gap-2">
            {r.recomendaciones.map((x) => (
              <li key={x.contenidoId} className="rounded-opt border border-line bg-surface p-3">
                <Link className="font-bold text-primary-600 underline" to={`/contenidos/${x.contenidoId}`}>{x.titulo}</Link> <Badge tono="primario">{t(FORMATO_TEXTO[x.formato])}</Badge>
                <p className="text-sm">{x.motivo}</p>
              </li>
            ))}
          </ul>
        </section>
      )}
      <section aria-labelledby="det">
        <h2 id="det" className="mb-2 text-xl">{t("Detalle por pregunta")}</h2>
        <ol className="grid gap-3">
          {r.detalle.map((d, i) => (
            <Card as="li" key={d.preguntaId} className="list-none">
              <p className="flex items-start gap-2 font-bold text-strong">
                {d.correcta ? <CheckCircle2 size={20} aria-hidden="true" className="mt-0.5 shrink-0 text-ok-fg" /> : <XCircle size={20} aria-hidden="true" className="mt-0.5 shrink-0 text-err-fg" />}
                <span>{i + 1}. {d.enunciado} <span className="sr-only">{d.correcta ? "(correcta)" : "(incorrecta)"}</span></span>
              </p>
              <p className="mt-1 text-sm">{t("Tu respuesta", "Tu respuesta")}: {d.tuRespuesta || t("Sin responder")} · {d.obtenido} de {d.puntaje} puntos</p>
              {!d.correcta && d.respuestaCorrecta?.length > 0 && <Banner tono="info" className="mt-2">{t("Respuesta correcta:")} {d.respuestaCorrecta.join("; ")}</Banner>}
            </Card>
          ))}
        </ol>
      </section>
    </>
  );
}
