import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Check, ExternalLink, Square, Volume2 } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { useNotif } from "../../context/NotifContext.jsx";
import { usePrefs } from "../../context/PrefsContext.jsx";
import { useTutor } from "../../context/TutorContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { FORMATO_TEXTO, fmtPct } from "../../utils/format.js";
import { Badge, Banner, Button, Card, Cargando, EstadoError, PageHeader, Pestanas, ProgressBar } from "../../components/ui/index.jsx";

function ApoyoCognitivo({ id }) {
  const { datos: a, cargando, error, recargar } = useAsync(() => api.contenidos.apoyo(id), [id]);
  if (cargando && !a) return <Cargando texto="Preparando el apoyo…" />;
  if (error && !a) return <EstadoError mensaje={error} onReintentar={recargar} />;
  return (
    <div className="grid gap-5">
      {a.mensajes?.map((m) => <Banner key={m} tono="info">{m}</Banner>)}
      {a.resumen && <Card as="section" aria-labelledby="ac-r"><h3 id="ac-r" className="mb-1 text-lg">Resumen</h3><p className="lesson-text">{a.resumen}</p></Card>}
      {a.conceptosClave?.length > 0 && <Card as="section" aria-labelledby="ac-c"><h3 id="ac-c" className="mb-1 text-lg">Conceptos clave</h3><ul className="lesson-text list-disc pl-5">{a.conceptosClave.map((c) => <li key={c}>{c}</li>)}</ul></Card>}
      {a.pasos?.length > 0 && <Card as="section" aria-labelledby="ac-p"><h3 id="ac-p" className="mb-1 text-lg">Paso a paso</h3><ol className="lesson-text list-decimal pl-5">{a.pasos.map((p) => <li key={p}>{p}</li>)}</ol></Card>}
      {a.unidades?.length > 0 && (
        <Card as="section" aria-labelledby="ac-u"><h3 id="ac-u" className="mb-2 text-lg">Contenido en partes cortas</h3>
          {a.unidades.map((u) => <div key={u.numero} className="mb-3"><h4 className="font-bold text-strong">{u.numero}. {u.titulo}</h4><p className="lesson-text">{u.texto}</p></div>)}</Card>
      )}
      {a.versionSimplificada && <Card as="section" aria-labelledby="ac-s"><h3 id="ac-s" className="mb-1 text-lg">Versión simplificada</h3><p className="lesson-text whitespace-pre-wrap">{a.versionSimplificada}</p></Card>}
      {a.glosario?.length > 0 && <Card as="section" aria-labelledby="ac-g"><h3 id="ac-g" className="mb-1 text-lg">Glosario</h3><dl>{a.glosario.map((g) => <div key={g.termino} className="mb-2"><dt className="font-bold text-strong">{g.termino}</dt><dd>{g.definicion}</dd></div>)}</dl></Card>}
    </div>
  );
}

/** RF-005 / RF-019 / RF-022: lección con formatos alternativos y apoyo cognitivo. */
export default function Leccion() {
  const { id } = useParams();
  const { usuario } = useAuth();
  const { avisar } = useNotif();
  const { config } = usePrefs();
  const { setContexto } = useTutor();
  const esEstudiante = usuario.rol === "ESTUDIANTE";
  const { datos: c, cargando, error, recargar } = useAsync(() => api.contenidos.ver(id), [id]);
  const [pestana, setPestana] = useState("contenido");
  const [avance, setAvance] = useState(null);
  const [leyendo, setLeyendo] = useState(false);
  const medio = useRef(null);
  const pendiente = useRef(0);
  const inicio = useRef(0);

  useEffect(() => {
    if (c) setContexto({ contenidoId: c.id, titulo: c.titulo, minuto: 0 });
    return () => setContexto(null);
  }, [c?.id, c?.titulo, setContexto]); // eslint-disable-line react-hooks/exhaustive-deps

  const enviarProgreso = useCallback(async (extra = {}) => {
    if (!esEstudiante) return null;
    const segundos = inicio.current ? Math.round((Date.now() - inicio.current) / 1000) : 0;
    inicio.current = Date.now();
    try {
      const r = await api.contenidos.progreso(Number(id), { segundos: Math.min(segundos, 86400), ...extra });
      setAvance(r);
      if (extra.completado) window.dispatchEvent(new Event("vl:puntos"));
      return r;
    } catch (e) { avisar(mensajeError(e), "error"); return null; }
  }, [id, esEstudiante, avisar]);

  // Registro periódico de tiempo consumido y al salir.
  useEffect(() => {
    if (!esEstudiante) return undefined;
    inicio.current = Date.now();
    const t = setInterval(() => enviarProgreso(), 30000);
    return () => { clearInterval(t); enviarProgreso(); };
  }, [enviarProgreso, esEstudiante]);

  useEffect(() => () => window.speechSynthesis?.cancel(), []);

  if (cargando && !c) return <Cargando texto="Cargando la lección…" />;
  if (error && !c) return <EstadoError mensaje={error} onReintentar={recargar} />;

  const recurso = (tipo) => c.recursos.find((r) => r.tipo === tipo && r.disponible);
  const subt = recurso("SUBTITULO");
  const trans = recurso("TRANSCRIPCION");
  const audio = recurso("AUDIO");
  const alt = recurso("TEXTO_ALTERNATIVO");
  const simple = recurso("VERSION_SIMPLIFICADA");
  const pct = Number(avance?.porcentaje ?? c.porcentaje ?? 0);
  const textoLeible = [c.cuerpo, trans?.descripcion].filter(Boolean).join(". ");

  const alternarVoz = () => {
    const s = window.speechSynthesis;
    if (!s) { avisar("Tu navegador no admite lectura en voz alta.", "error"); return; }
    if (leyendo) { s.cancel(); setLeyendo(false); return; }
    const u = new SpeechSynthesisUtterance(textoLeible || c.descripcion || c.titulo);
    u.lang = "es-CO"; u.onend = () => setLeyendo(false);
    s.speak(u); setLeyendo(true);
  };
  const alActualizarTiempo = (e) => {
    const m = e.currentTarget;
    if (m.duration && m.currentTime / m.duration >= 0.95 && !pendiente.current) { pendiente.current = 1; enviarProgreso({ porcentaje: 100, completado: true }); }
    setContexto({ contenidoId: c.id, titulo: c.titulo, minuto: Math.floor(m.currentTime) });
  };

  const paneles = [{ id: "contenido", texto: "Contenido" }];
  if (c.apoyoCognitivo) paneles.push({ id: "apoyo", texto: "Apoyo para estudiar" });
  if (trans || alt || simple) paneles.push({ id: "texto", texto: "Versión en texto" });

  return (
    <>
      <p className="mb-3"><Link to={`/cursos/${c.cursoId}`} className="inline-flex min-h-11 items-center gap-1.5 font-bold text-primary-600 underline"><ArrowLeft size={16} aria-hidden="true" />Volver al curso</Link></p>
      <PageHeader eyebrow={FORMATO_TEXTO[c.formato]} titulo={c.titulo} subtitulo={c.descripcion}
        acciones={config.textoAVoz || esEstudiante ? <Button variante="secondary" icono={leyendo ? Square : Volume2} onClick={alternarVoz}>{leyendo ? "Detener lectura" : "Leer en voz alta"}</Button> : null} />
      {!esEstudiante && <Banner tono="info" className="mb-4">Vista previa de solo lectura: no se registra progreso.</Banner>}
      {c.advertencias?.map((a) => <Banner key={a} tono="warn" className="mb-3">{a}</Banner>)}
      {c.lenguaSenasDisponible ? <Banner tono="ok" className="mb-3">Este contenido incluye interpretación en lengua de señas.</Banner>
        : c.mensajeLenguaSenas && <Banner tono="info" className="mb-3">{c.mensajeLenguaSenas}</Banner>}
      {c.opcionesAlternativas?.length > 0 && <p className="mb-3 flex flex-wrap items-center gap-2 text-sm">También disponible como: {c.opcionesAlternativas.map((o) => <Badge key={o} tono="tutor">{o.replaceAll("_", " ").toLowerCase()}</Badge>)}</p>}
      {esEstudiante && <Card className="mb-5 max-w-2xl"><ProgressBar valor={pct} etiqueta="Avance de esta lección" /></Card>}
      {paneles.length > 1 && <Pestanas etiqueta="Formas de estudiar" pestanas={paneles} activa={pestana} onCambiar={setPestana} />}

      <div role="tabpanel" id={`panel-${pestana}`} aria-labelledby={`tab-${pestana}`}>
        {pestana === "contenido" && (
          <div className="grid gap-5">
            {c.formato === "VIDEO" && c.urlRecurso && (
              <video ref={medio} controls className="w-full max-w-4xl rounded-card bg-black" src={c.urlRecurso} onTimeUpdate={alActualizarTiempo} crossOrigin="anonymous">
                {subt && <track kind="captions" srcLang="es" label="Español" src={subt.url} default={config.subtitulos} />}
                Tu navegador no puede reproducir este video.
              </video>
            )}
            {(c.formato === "PODCAST" && (c.urlRecurso || audio)) && (
              <audio ref={medio} controls className="w-full max-w-3xl" src={c.urlRecurso || audio.url} onTimeUpdate={alActualizarTiempo}>Tu navegador no puede reproducir este audio.</audio>
            )}
            {c.formato === "SIMULACION" && c.urlRecurso && (
              <Card><p className="mb-3">Esta práctica se abre en una pestaña nueva. Cuando termines, vuelve y márcala como completada.</p>
                <a href={c.urlRecurso} target="_blank" rel="noreferrer" className="vl-btn inline-flex min-h-12 items-center gap-2 rounded-ctl border-[1.5px] border-primary-600 bg-primary-600 px-5 font-bold text-white"><ExternalLink size={18} aria-hidden="true" />Abrir simulación<span className="sr-only"> (se abre en una pestaña nueva)</span></a></Card>
            )}
            {c.cuerpo && <Card as="article"><div className="lesson-text whitespace-pre-wrap">{c.cuerpo}</div></Card>}
            {!c.cuerpo && !c.urlRecurso && !audio && <Banner tono="info">Este contenido todavía no tiene material para mostrar.</Banner>}
            {config.transcripcion && trans?.descripcion && <Card as="section" aria-labelledby="tr"><h2 id="tr" className="mb-1 text-lg">Transcripción</h2><p className="lesson-text whitespace-pre-wrap">{trans.descripcion}</p></Card>}
          </div>
        )}
        {pestana === "apoyo" && <ApoyoCognitivo id={c.id} />}
        {pestana === "texto" && (
          <div className="grid gap-5">
            {trans && <Card as="section" aria-labelledby="t1"><h2 id="t1" className="mb-1 text-lg">Transcripción</h2><p className="lesson-text whitespace-pre-wrap">{trans.descripcion || "Disponible en el enlace del recurso."}</p>{trans.url && <p className="mt-2"><a className="font-bold underline" href={trans.url} target="_blank" rel="noreferrer">Abrir transcripción</a></p>}</Card>}
            {alt && <Card as="section" aria-labelledby="t2"><h2 id="t2" className="mb-1 text-lg">Descripción del contenido visual</h2><p className="lesson-text whitespace-pre-wrap">{alt.descripcion}</p></Card>}
            {simple && <Card as="section" aria-labelledby="t3"><h2 id="t3" className="mb-1 text-lg">Versión simplificada</h2><p className="lesson-text whitespace-pre-wrap">{simple.descripcion}</p></Card>}
          </div>
        )}
      </div>

      {esEstudiante && (
        <div className="mt-8">
          {(avance?.estado ?? c.estado) === "COMPLETADO"
            ? <p className="flex items-center gap-2 font-bold text-ok-fg"><Check size={20} aria-hidden="true" />Completaste este contenido ({fmtPct(100)})</p>
            : <Button icono={Check} onClick={async () => { const r = await enviarProgreso({ porcentaje: 100, completado: true }); if (r) { avisar("¡Contenido completado!", "ok"); if (r.moduloCompleto) avisar("¡Completaste el módulo! Ya puedes presentar su evaluación.", "ok"); if (r.cursoFinalizado) avisar("¡Finalizaste el curso!", "ok"); } }}>Marcar como completado</Button>}
        </div>
      )}
    </>
  );
}
