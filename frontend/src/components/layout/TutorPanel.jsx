import { useCallback, useEffect, useRef, useState } from "react";
import { Bot, Lightbulb, ListChecks, MoreVertical, RefreshCw, Send, ThumbsDown, ThumbsUp, X } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { useTutor } from "../../context/TutorContext.jsx";
import { fmtHora } from "../../utils/format.js";
import { Banner } from "../ui/index.jsx";
import { cx } from "../../utils/cx.js";

const ATAJOS = [
  { t: "Explícamelo de otra forma", q: "Explícamelo de otra forma, con palabras más sencillas.", i: RefreshCw },
  { t: "Dame un ejemplo", q: "Dame un ejemplo de lo que estoy viendo.", i: Lightbulb },
  { t: "Resume", q: "Resume esta lección en pocas frases.", i: ListChecks },
];

/** Panel del Tutor IA (RF-024). Usa la lección y el perfil de accesibilidad; nunca el método VARK. */
export function TutorPanel() {
  const { usuario } = useAuth();
  const { cerrar, contexto } = useTutor();
  const [mensajes, setMensajes] = useState([]);
  const [texto, setTexto] = useState("");
  const [pensando, setPensando] = useState(false);
  const [error, setError] = useState(null);
  const [menu, setMenu] = useState(false);
  const fin = useRef(null);
  const entrada = useRef(null);

  useEffect(() => {
    let activo = true;
    api.tutor.historial(20)
      .then((h) => { if (activo) setMensajes([...h].reverse()); })
      .catch(() => {});
    entrada.current?.focus();
    return () => { activo = false; };
  }, []);

  useEffect(() => { fin.current?.scrollIntoView({ block: "end" }); }, [mensajes, pensando]);

  useEffect(() => {
    const tecla = (e) => { if (e.key === "Escape") cerrar(); };
    document.addEventListener("keydown", tecla);
    return () => document.removeEventListener("keydown", tecla);
  }, [cerrar]);

  const enviar = useCallback(async (pregunta) => {
    const p = pregunta.trim();
    if (!p || pensando) return;
    setError(null);
    setPensando(true);
    setTexto("");
    const conMinuto = contexto?.minuto ? `[Minuto ${contexto.minuto} del video] ${p}` : p;
    try {
      const r = await api.tutor.consultar(conMinuto, contexto?.contenidoId ?? null);
      setMensajes((m) => [...m, { ...r, pregunta: p }]);
    } catch (e) {
      setError(mensajeError(e, "El Tutor IA no está disponible ahora. Puedes seguir con tus contenidos."));
      setTexto(p);
    } finally {
      setPensando(false);
    }
  }, [contexto, pensando]);

  const marcar = async (m, util) => {
    try {
      const r = await api.tutor.util(m.id, util);
      setMensajes((lista) => lista.map((x) => (x.id === m.id ? { ...x, util: r.util, valorado: true } : x)));
    } catch (e) { setError(mensajeError(e)); }
  };

  return (
    <aside aria-label="Tutor IA" className="flex h-full w-full flex-col bg-surface">
      <div className="flex items-center gap-3 bg-tutor-700 px-4 py-3 text-white">
        <span aria-hidden="true" className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/15"><Bot size={22} /></span>
        <div className="min-w-0 flex-1">
          <h2 className="text-lg text-white">Tutor IA</h2>
          <p className="text-xs text-white/90">En línea · {contexto ? "Usa el contexto de esta lección" : "Pregúntame sobre tus cursos"}</p>
        </div>
        <div className="relative">
          <button type="button" onClick={() => setMenu((m) => !m)} aria-expanded={menu} aria-label="Más opciones del tutor"
            className="flex h-11 w-11 items-center justify-center rounded-lg hover:bg-white/10"><MoreVertical size={20} aria-hidden="true" /></button>
          {menu && (
            <div className="absolute right-0 z-10 mt-1 w-56 rounded-xl border border-line bg-surface p-1.5 text-strong shadow-lg">
              <button type="button" onClick={() => { setMensajes([]); setMenu(false); }} className="min-h-11 w-full rounded-lg px-3 text-left font-bold hover:bg-subtle">Limpiar la pantalla</button>
              <button type="button" onClick={cerrar} className="min-h-11 w-full rounded-lg px-3 text-left font-bold hover:bg-subtle">Cerrar el panel</button>
            </div>
          )}
        </div>
        <button type="button" onClick={cerrar} aria-label="Cerrar el Tutor IA" className="flex h-11 w-11 items-center justify-center rounded-lg hover:bg-white/10">
          <X size={22} aria-hidden="true" />
        </button>
      </div>

      {contexto && (
        <p className="mx-4 mt-3 rounded-xl bg-tutor-100 px-3 py-2 text-sm font-semibold text-tutor-700">
          Estoy viendo «{contexto.titulo}»{contexto.minuto ? ` en ${contexto.minuto}` : ""}.
        </p>
      )}

      <div className="flex-1 space-y-4 overflow-y-auto p-4" role="log" aria-live="polite" aria-label="Conversación con el Tutor IA">
        {mensajes.length === 0 && !pensando && (
          <p className="rounded-xl bg-subtle p-4 text-sm">Hola, {usuario.nombre.split(" ")[0]}. Escríbeme tu duda o usa un atajo. Respondo en texto sencillo y según lo que necesites para estudiar.</p>
        )}
        {mensajes.map((m) => (
          <div key={m.id} className="space-y-2">
            <div className="ml-auto max-w-[85%] rounded-2xl rounded-tr-sm bg-primary-100 p-3">
              <p className="mb-0.5 text-xs font-bold text-primary-700">{usuario.nombre.split(" ")[0]}</p>
              <p className="whitespace-pre-wrap text-strong">{m.pregunta}</p>
            </div>
            <div className="max-w-[92%] rounded-2xl rounded-tl-sm bg-tutor-100 p-3">
              <p className="mb-0.5 flex items-center gap-1 text-xs font-bold uppercase tracking-[0.04em] text-tutor-700"><Bot size={13} aria-hidden="true" />Tutor IA · {fmtHora(m.fecha)}</p>
              <p className="whitespace-pre-wrap text-strong">{m.respuesta}</p>
              <div className="mt-2 flex flex-wrap items-center gap-2 text-sm">
                <span className="text-body">¿Te sirvió?</span>
                <button type="button" onClick={() => marcar(m, true)} aria-pressed={m.util === true}
                  className={cx("flex min-h-9 items-center gap-1 rounded-full border px-3 font-bold", m.util ? "border-tutor-700 bg-tutor-700 text-white" : "border-tutor-700 text-tutor-700")}>
                  <ThumbsUp size={14} aria-hidden="true" />Sí</button>
                <button type="button" onClick={() => marcar(m, false)} aria-pressed={m.valorado && m.util === false}
                  className={cx("flex min-h-9 items-center gap-1 rounded-full border px-3 font-bold", m.valorado && !m.util ? "border-tutor-700 bg-tutor-700 text-white" : "border-tutor-700 text-tutor-700")}>
                  <ThumbsDown size={14} aria-hidden="true" />No</button>
              </div>
            </div>
          </div>
        ))}
        {pensando && <p role="status" className="rounded-2xl bg-tutor-100 p-3 text-sm font-semibold text-tutor-700">El Tutor IA está pensando… puede tardar unos segundos.</p>}
        <div ref={fin} />
      </div>

      <div className="border-t border-line p-4">
        {error && <Banner tono="err" className="mb-3">{error}</Banner>}
        <div className="mb-3 flex flex-wrap gap-2">
          {ATAJOS.map((a) => (
            <button key={a.t} type="button" onClick={() => enviar(a.q)} disabled={pensando}
              className="flex min-h-10 items-center gap-1.5 rounded-full border-[1.5px] border-tutor-700 bg-surface px-3 text-sm font-bold text-tutor-700 hover:bg-tutor-100 disabled:opacity-50">
              <a.i size={15} aria-hidden="true" />{a.t}</button>
          ))}
        </div>
        <form onSubmit={(e) => { e.preventDefault(); enviar(texto); }} className="flex items-end gap-2">
          <div className="flex-1">
            <label htmlFor="tutor-pregunta" className="sr-only">Escribe tu pregunta para el Tutor IA</label>
            <textarea id="tutor-pregunta" ref={entrada} value={texto} onChange={(e) => setTexto(e.target.value)} rows={2} maxLength={1000}
              placeholder="Escribe tu pregunta…" onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); enviar(texto); } }}
              className="vl-input w-full resize-none rounded-ctl border border-line px-3 py-2 text-strong" />
          </div>
          <button type="submit" disabled={pensando || !texto.trim()} aria-label="Enviar pregunta"
            className="flex h-12 w-12 shrink-0 items-center justify-center rounded-ctl bg-tutor-700 text-white hover:bg-[#08463c] disabled:bg-[#E2E8F0] disabled:text-[#475569]">
            <Send size={20} aria-hidden="true" />
          </button>
        </form>
        <p className="mt-2 text-xs font-semibold text-body">El Tutor IA puede equivocarse. Contrasta la respuesta con el curso.</p>
      </div>
    </aside>
  );
}
