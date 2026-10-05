import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, ArrowRight, Timer } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { fmtCronometro } from "../../utils/format.js";
import { Banner, Button, Cargando, Checkbox, EstadoError, OpcionCard, ProgressBar, TextField } from "../../components/ui/index.jsx";
import ConfirmarDialogo from "../../components/ConfirmarDialogo.jsx";

function Pregunta({ p, valor, onCambio }) {
  const sel = new Set((valor ?? "").split(",").filter(Boolean));
  if (p.tipo === "ABIERTA") {
    return <TextField multilinea label="Tu respuesta" maxLength={2000} value={valor ?? ""} onChange={(e) => onCambio(e.target.value)} />;
  }
  if (p.tipo === "SELECCION_MULTIPLE") {
    return (
      <div role="group" aria-label="Opciones (puedes elegir varias)" className="grid gap-2">
        {p.opciones.map((o) => (
          <Checkbox key={o.id} checked={sel.has(String(o.id))} onChange={(v) => { const n = new Set(sel); v ? n.add(String(o.id)) : n.delete(String(o.id)); onCambio([...n].join(",")); }}>{o.texto}</Checkbox>
        ))}
      </div>
    );
  }
  return (
    <div role="radiogroup" aria-label="Opciones" className="grid gap-3">
      {p.opciones.map((o) => <OpcionCard key={o.id} titulo={o.texto} seleccionada={valor === String(o.id)} onSelect={() => onCambio(String(o.id))} />)}
    </div>
  );
}

/** RF-007: presentar una evaluación con guardado automático y cronómetro. */
export default function TomarEvaluacion() {
  const { id } = useParams();
  const nav = useNavigate();
  const { state } = useLocation();
  const [intento, setIntento] = useState(state?.intento ?? null);
  const [error, setError] = useState(null);
  const [respuestas, setRespuestas] = useState(() => state?.intento?.respuestas ?? {});
  const [indice, setIndice] = useState(0);
  const [segundos, setSegundos] = useState(state?.intento?.segundosRestantes ?? null);
  const [estadoGuardado, setEstadoGuardado] = useState("");
  const [confirmar, setConfirmar] = useState(false);
  const [finalizando, setFinalizando] = useState(false);
  const foco = useRef(null);
  const enviado = useRef(false);

  // Si se recarga la página, se reanuda el intento en curso a través de iniciar().
  useEffect(() => {
    if (intento) return;
    let evaluacionId = null;
    try { evaluacionId = sessionStorage.getItem(`vl.eval.${id}`); } catch { /* ignorar */ }
    if (!evaluacionId) { nav("/evaluaciones", { replace: true }); return; }
    api.evaluaciones.iniciar(Number(evaluacionId))
      .then((i) => { setIntento(i); setRespuestas(i.respuestas ?? {}); setSegundos(i.segundosRestantes ?? null); })
      .catch((e) => setError(mensajeError(e)));
  }, [intento, id, nav]);

  const finalizar = useCallback(async () => {
    if (enviado.current) return;
    enviado.current = true;
    setFinalizando(true);
    try { await api.evaluaciones.finalizar(Number(id)); nav(`/intentos/${id}/resultado`, { replace: true }); }
    catch (e) { enviado.current = false; setError(mensajeError(e)); setFinalizando(false); }
  }, [id, nav]);

  // Cronómetro: al llegar a 0 se envía automáticamente.
  useEffect(() => {
    if (segundos === null) return undefined;
    if (segundos <= 0) { finalizar(); return undefined; }
    const t = setTimeout(() => setSegundos((s) => s - 1), 1000);
    return () => clearTimeout(t);
  }, [segundos, finalizar]);

  useEffect(() => { foco.current?.focus(); }, [indice]);

  if (error && !intento) return <EstadoError mensaje={error} />;
  if (!intento) return <Cargando texto="Preparando la evaluación…" />;

  const preguntas = intento.preguntas;
  const p = preguntas[indice];
  const respondidas = preguntas.filter((x) => (respuestas[x.id] ?? "") !== "").length;
  const guardar = async (valor) => {
    setRespuestas((r) => ({ ...r, [p.id]: valor }));
    setEstadoGuardado("Guardando…");
    try { const g = await api.evaluaciones.guardar(Number(id), p.id, valor); if (g.segundosRestantes != null) setSegundos(g.segundosRestantes); setEstadoGuardado("Respuesta guardada"); }
    catch (e) { setEstadoGuardado(mensajeError(e, "No se pudo guardar. Se volverá a intentar al cambiar de pregunta.")); }
  };
  const bajo = segundos !== null && segundos <= 60;

  return (
    <>
      <p className="mb-3"><Link to="/evaluaciones" className="inline-flex min-h-11 items-center gap-1.5 font-bold text-primary-600 underline"><ArrowLeft size={16} aria-hidden="true" />Salir y continuar después</Link></p>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl">{intento.titulo}</h1>
        {segundos !== null && (
          <p className={`flex items-center gap-2 rounded-full border px-4 py-2 font-extrabold ${bajo ? "border-err-fg bg-err-bg text-err-fg" : "border-line bg-surface text-strong"}`}>
            <Timer size={18} aria-hidden="true" /><span className="sr-only">Tiempo restante: </span>{fmtCronometro(segundos)}
          </p>
        )}
      </div>
      {segundos !== null && <p className="sr-only" role="status" aria-live="polite">{bajo ? "Queda menos de un minuto." : ""}</p>}
      {intento.reanudado && <Banner tono="info" className="mb-4">Retomaste tu intento donde lo dejaste.</Banner>}
      {error && <Banner tono="err" className="mb-4" rol="alert">{error}</Banner>}
      <p className="mb-1 text-sm font-bold text-primary-700" aria-live="polite">Pregunta {indice + 1} de {preguntas.length}</p>
      <ProgressBar valor={(respondidas / preguntas.length) * 100} etiqueta="Preguntas respondidas" className="mb-6" />
      <div className="max-w-3xl">
        <h2 ref={foco} tabIndex={-1} className="mb-4 text-xl outline-none">{p.enunciado}</h2>
        <Pregunta key={p.id} p={p} valor={respuestas[p.id]} onCambio={guardar} />
        <p className="mt-3 min-h-6 text-sm" role="status">{estadoGuardado}</p>
        <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
          <Button variante="secondary" icono={ArrowLeft} disabled={indice === 0} onClick={() => setIndice(indice - 1)}>Anterior</Button>
          {indice < preguntas.length - 1
            ? <Button icono={ArrowRight} onClick={() => setIndice(indice + 1)}>Siguiente</Button>
            : <Button cargando={finalizando} onClick={() => setConfirmar(true)}>Terminar evaluación</Button>}
        </div>
        <nav aria-label="Ir a una pregunta" className="mt-6 flex flex-wrap gap-2">
          {preguntas.map((x, i) => (
            <button key={x.id} type="button" onClick={() => setIndice(i)} aria-current={i === indice ? "step" : undefined}
              className={`min-h-11 min-w-11 rounded-lg border-2 font-bold ${i === indice ? "border-primary-600 bg-primary-100 text-primary-700" : (respuestas[x.id] ?? "") !== "" ? "border-ok-fg bg-tutor-100 text-ok-fg" : "border-line bg-surface text-body"}`}>
              {i + 1}<span className="sr-only">{(respuestas[x.id] ?? "") !== "" ? " (respondida)" : " (sin responder)"}</span>
            </button>
          ))}
        </nav>
      </div>
      <ConfirmarDialogo abierto={confirmar} onCerrar={() => setConfirmar(false)} titulo="¿Terminar la evaluación?" textoConfirmar="Sí, terminar" onConfirmar={finalizar}>
        <p>Respondiste {respondidas} de {preguntas.length} preguntas.{respondidas < preguntas.length ? " Las que dejaste sin responder cuentan como incorrectas." : ""}</p>
      </ConfirmarDialogo>
    </>
  );
}
