import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { Check, Flag, Send, Trash2, Wifi, WifiOff } from "lucide-react";
import { api, mensajeError, token } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { ROL_TEXTO, fmtFecha, fmtHora, fmtDia } from "../../utils/format.js";
import { Badge, Banner, Button, Card, Cargando, EstadoError, PageHeader, Pestanas, SelectField, TextField, Vacio } from "../../components/ui/index.jsx";
import ConfirmarDialogo from "../../components/ConfirmarDialogo.jsx";
import VolverACursos from "../../components/VolverACursos.jsx";

function cursosDelRol(rol) {
  if (rol === "ESTUDIANTE") return api.cursos.mios();
  if (rol === "INSTRUCTOR") return api.instructor.cursos();
  return api.admin.cursos();
}

function Sala({ curso, onModerado }) {
  const { usuario } = useAuth();
  const { avisar } = useNotif();
  const [mensajes, setMensajes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [conectado, setConectado] = useState(false);
  const [aviso, setAviso] = useState(null);
  const [aEliminar, setAEliminar] = useState(null);
  const fin = useRef(null);
  const puedeModerar = usuario.rol !== "ESTUDIANTE";

  const agregar = useCallback((m) => setMensajes((l) => (l.some((x) => x.id === m.id) ? l : [...l, m])), []);

  useEffect(() => {
    let activo = true;
    setCargando(true); setError(null); setMensajes([]);
    api.chat.historial(curso, 80)
      .then((h) => { if (activo) setMensajes(h); })
      .catch((e) => { if (activo) setError(mensajeError(e)); })
      .finally(() => { if (activo) setCargando(false); });
    return () => { activo = false; };
  }, [curso]);

  // Tiempo real por WebSocket. Si no conecta, la pantalla sigue funcionando por la API normal.
  useEffect(() => {
    let ws; let cerrado = false; let reintento;
    const conectar = () => {
      const proto = window.location.protocol === "https:" ? "wss" : "ws";
      ws = new WebSocket(`${proto}://${window.location.host}/ws/cursos/${curso}?token=${encodeURIComponent(token.get() ?? "")}`);
      ws.onopen = () => setConectado(true);
      ws.onclose = () => { setConectado(false); if (!cerrado) reintento = setTimeout(conectar, 5000); };
      ws.onmessage = (ev) => {
        try {
          const d = JSON.parse(ev.data);
          if (d.tipo === "MENSAJE") { agregar(d.mensaje); if (d.mensaje.usuarioId !== usuario.id) setAviso(`Nuevo mensaje de ${d.mensaje.autor}.`); }
          if (d.tipo === "ERROR") setAviso(d.mensaje);
        } catch { /* mensaje ilegible */ }
      };
    };
    conectar();
    return () => { cerrado = true; clearTimeout(reintento); ws?.close(); };
  }, [curso, usuario.id, agregar]);

  useEffect(() => { fin.current?.scrollIntoView({ block: "end" }); }, [mensajes.length]);

  const enviar = async (e) => {
    e.preventDefault();
    if (!texto.trim()) return;
    setEnviando(true); setAviso(null);
    try { agregar(await api.chat.enviar(curso, texto.trim())); setTexto(""); }
    catch (err) { setAviso(mensajeError(err)); }
    finally { setEnviando(false); }
  };
  // Reportar NO borra el mensaje: queda «en revisión» hasta que el instructor decida.
  const reportar = async (m) => {
    try {
      const r = await api.chat.reportar(m.id);
      setMensajes((l) => l.map((x) => (x.id === m.id && x.estado === "ENVIADO" ? { ...x, estado: "REPORTADO" } : x)));
      avisar(r.mensaje, "ok");
    } catch (e) { avisar(mensajeError(e), "error"); }
  };
  const eliminar = async (m) => {
    await api.chat.ocultar(m.id);
    setMensajes((l) => l.filter((x) => x.id !== m.id));
    avisar("El mensaje se eliminó.", "ok");
    onModerado?.();
  };
  const mantener = async (m) => {
    try {
      const r = await api.chat.mantener(m.id);
      setMensajes((l) => l.map((x) => (x.id === m.id ? { ...x, estado: "ENVIADO" } : x)));
      avisar(r.mensaje, "ok");
      onModerado?.();
    } catch (e) { avisar(mensajeError(e), "error"); }
  };

  if (cargando) return <Cargando texto="Cargando la conversación…" />;
  if (error) return <EstadoError mensaje={error} />;

  return (
    <Card className="flex flex-col gap-4">
      <p className="flex items-center gap-2 text-sm font-bold" role="status">
        {conectado ? <><Wifi size={16} aria-hidden="true" className="text-ok-fg" />Conversación en tiempo real</> : <><WifiOff size={16} aria-hidden="true" />Sin conexión en vivo; los mensajes se envían igual</>}
      </p>
      <div className="h-[max(26rem,calc(100dvh-24rem))] overflow-y-auto rounded-opt bg-subtle p-4" tabIndex={0} role="log" aria-label="Mensajes del curso" aria-live="polite">
        {mensajes.length === 0 && <p>Aún no hay mensajes. Escribe el primero.</p>}
        <ul className="grid gap-3">
          {mensajes.map((m, i) => {
            const dia = fmtDia(m.fechaEnvio);
            const separador = i === 0 || dia !== fmtDia(mensajes[i - 1].fechaEnvio);
            const mio = m.usuarioId === usuario.id;
            return (
              <li key={m.id} className="list-none">
                {separador && <p className="mb-2 text-center text-xs font-bold uppercase text-muted">{dia}</p>}
                <div className={`rounded-opt border p-3 ${mio ? "border-primary-300 bg-primary-100" : "border-line bg-surface"} ${m.estado === "REPORTADO" ? "border-warn-fg" : ""}`}>
                  <div className="flex flex-wrap items-center justify-between gap-x-3">
                    <p className="flex flex-wrap items-center gap-2 text-sm">
                      <strong className="text-strong">{mio ? "Tú" : m.autor}</strong>
                      {m.rol !== "ESTUDIANTE" && <Badge tono="tutor">{ROL_TEXTO[m.rol]}</Badge>}
                      <span>{fmtHora(m.fechaEnvio)}</span>
                      {m.estado === "REPORTADO" && <Badge tono="warn">Reportado · en revisión</Badge>}
                    </p>
                    <div className="flex flex-wrap gap-1">
                      {!mio && m.estado !== "REPORTADO" && <Button variante="ghost" icono={Flag} onClick={() => reportar(m)}>Reportar<span className="sr-only"> mensaje de {m.autor}</span></Button>}
                      {puedeModerar && m.estado === "REPORTADO" && <Button variante="ghost" icono={Check} onClick={() => mantener(m)}>Mantener<span className="sr-only"> mensaje de {m.autor}</span></Button>}
                      {puedeModerar && <Button variante="ghost" icono={Trash2} onClick={() => setAEliminar(m)}>Eliminar<span className="sr-only"> mensaje de {m.autor}</span></Button>}
                    </div>
                  </div>
                  <p className="whitespace-pre-wrap break-words text-strong">{m.contenido}</p>
                </div>
              </li>
            );
          })}
        </ul>
        <div ref={fin} />
      </div>
      {aviso && <Banner tono="info" rol="status">{aviso}</Banner>}
      <form onSubmit={enviar} className="flex flex-wrap items-end gap-3" noValidate>
        <TextField className="min-w-60 flex-1" label="Escribe un mensaje" value={texto} onChange={(e) => setTexto(e.target.value)} maxLength={1000} />
        <Button type="submit" icono={Send} cargando={enviando} disabled={!texto.trim()} motivo={!texto.trim() ? "Escribe un mensaje." : undefined}>Enviar</Button>
      </form>
      <ConfirmarDialogo abierto={Boolean(aEliminar)} titulo="¿Eliminar este mensaje?" textoConfirmar="Sí, eliminar" onCerrar={() => setAEliminar(null)}
        onConfirmar={() => eliminar(aEliminar)}>
        <p>El mensaje de {aEliminar?.autor} dejará de verse en la conversación. Esta acción no se puede deshacer.</p>
      </ConfirmarDialogo>
    </Card>
  );
}

/** Sección «Mensajes reportados» del instructor: todos los cursos, él decide si se elimina o se mantiene. */
function Reportados({ onCambio }) {
  const { avisar } = useNotif();
  const { datos, cargando, error, recargar } = useAsync(() => api.chat.reportados(), []);
  const [aEliminar, setAEliminar] = useState(null);

  if (cargando && !datos) return <Cargando texto="Cargando los mensajes reportados…" />;
  if (error && !datos) return <EstadoError mensaje={error} onReintentar={recargar} />;

  const cambio = async () => { await recargar(); onCambio?.(); };
  const eliminar = async (m) => { await api.chat.ocultar(m.id); avisar("El mensaje se eliminó.", "ok"); await cambio(); };
  const mantener = async (m) => {
    try { const r = await api.chat.mantener(m.id); avisar(r.mensaje, "ok"); await cambio(); }
    catch (e) { avisar(mensajeError(e), "error"); }
  };

  if (datos.length === 0) return <Vacio titulo="No hay mensajes reportados">Cuando alguien reporte un mensaje en tus cursos aparecerá aquí para que decidas si se elimina o se mantiene.</Vacio>;
  return (
    <section aria-labelledby="rep-titulo" className="grid gap-3">
      <h2 id="rep-titulo" className="text-xl">Mensajes reportados ({datos.length})</h2>
      <Banner tono="info">El mensaje sigue visible, marcado «en revisión», hasta que decidas. Tú eres quien modera tus cursos.</Banner>
      <ul className="grid gap-3">
        {datos.map((m) => (
          <Card as="li" key={m.id} className="list-none">
            <p className="flex flex-wrap items-center gap-2 text-sm">
              <Badge tono="warn">En revisión</Badge>
              <strong className="text-strong">{m.curso}</strong>
              <Link className="font-bold text-primary-600 underline" to={`/comunidad/${m.cursoId}`}>Ver conversación<span className="sr-only"> de {m.curso}</span></Link>
            </p>
            <p className="mt-2 text-sm"><strong className="text-strong">{m.autor}</strong> · {ROL_TEXTO[m.rolAutor]} · enviado {fmtFecha(m.fechaEnvio)}</p>
            <blockquote className="mt-1 whitespace-pre-wrap break-words rounded-opt border border-line bg-subtle p-3 text-strong">{m.contenido}</blockquote>
            <p className="mt-2 text-sm">Reportado por {m.reportadoPor.length ? m.reportadoPor.join(", ") : "—"} · {fmtFecha(m.fechaReporte)}</p>
            <div className="mt-3 flex flex-wrap gap-2">
              <Button icono={Trash2} onClick={() => setAEliminar(m)}>Eliminar mensaje<span className="sr-only"> de {m.autor}</span></Button>
              <Button variante="secondary" icono={Check} onClick={() => mantener(m)}>Mantener mensaje<span className="sr-only"> de {m.autor}</span></Button>
            </div>
          </Card>
        ))}
      </ul>
      <ConfirmarDialogo abierto={Boolean(aEliminar)} titulo="¿Eliminar este mensaje?" textoConfirmar="Sí, eliminar" onCerrar={() => setAEliminar(null)}
        onConfirmar={() => eliminar(aEliminar)}>
        <p>El mensaje de {aEliminar?.autor} dejará de verse en la conversación. Esta acción no se puede deshacer.</p>
      </ConfirmarDialogo>
    </section>
  );
}

/** RF-010: comunidad del curso (chat). */
export default function Comunidad() {
  const { cursoId } = useParams();
  const nav = useNavigate();
  const { usuario } = useAuth();
  const { datos, cargando, error, recargar } = useAsync(() => cursosDelRol(usuario.rol), [usuario.rol]);
  const [params, setParams] = useSearchParams();
  const esInstructor = usuario.rol === "INSTRUCTOR";
  const { datos: reportados, recargar: recargarReportados } = useAsync(() => (esInstructor ? api.chat.reportados() : Promise.resolve([])), [esInstructor]);
  const vista = esInstructor && params.get("vista") === "reportes" ? "reportes" : "conversacion";
  const cambiarVista = (v) => setParams(v === "reportes" ? { vista: "reportes" } : {}, { replace: true });

  if (cargando && !datos) return <Cargando texto="Cargando tus cursos…" />;
  if (error && !datos) return <EstadoError mensaje={error} onReintentar={recargar} />;
  const lista = (datos ?? []).filter((c) => usuario.rol !== "ESTUDIANTE" || c.inscrito !== false);
  const actual = lista.find((c) => String(c.id) === cursoId);

  return (
    <>
      <VolverACursos />
      <PageHeader eyebrow="Comunidad" titulo="Conversación del curso" subtitulo="Resuelve dudas con tu grupo. Sé respetuoso: los mensajes se pueden reportar." />
      {esInstructor && (
        <Pestanas etiqueta="Secciones de la comunidad" activa={vista} onCambiar={cambiarVista} pestanas={[
          { id: "conversacion", texto: "Conversación" },
          { id: "reportes", texto: `Mensajes reportados${reportados?.length ? ` (${reportados.length})` : ""}` }]} />
      )}
      {vista === "reportes" ? (
        <div role="tabpanel" id="panel-reportes" aria-labelledby="tab-reportes"><Reportados onCambio={recargarReportados} /></div>
      ) : lista.length === 0 ? (
        <Vacio titulo="Todavía no tienes cursos con comunidad">{usuario.rol === "ESTUDIANTE" && <p>Inscríbete en un curso desde <Link className="font-bold underline" to="/cursos">Mis cursos</Link>.</p>}</Vacio>
      ) : (
        <div className="grid gap-4" id="panel-conversacion" role={esInstructor ? "tabpanel" : undefined} aria-labelledby={esInstructor ? "tab-conversacion" : undefined}>
          <SelectField label="Curso" className="max-w-md" value={cursoId ?? ""} onChange={(e) => nav(e.target.value ? `/comunidad/${e.target.value}` : "/comunidad")}>
            <option value="">Elige un curso…</option>
            {lista.map((c) => <option key={c.id} value={c.id}>{c.titulo}</option>)}
          </SelectField>
          {actual ? <Sala key={actual.id} curso={actual.id} onModerado={recargarReportados} /> : <Banner tono="info">Elige un curso para ver su conversación.</Banner>}
        </div>
      )}
    </>
  );
}
