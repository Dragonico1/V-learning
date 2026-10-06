import { useState } from "react";
import { ClipboardList, ListChecks, Pencil, Plus, Trash2 } from "lucide-react";
import { api, detallesError, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { fmtFecha } from "../../utils/format.js";
import { Badge, Banner, Button, Cargando, Dialogo, EstadoError, TextField } from "../../components/ui/index.jsx";
import ConfirmarDialogo from "../../components/ConfirmarDialogo.jsx";

function FormularioTarea({ moduloId, tarea, onGuardado, onCancelar }) {
  const [f, setF] = useState({
    titulo: tarea?.titulo ?? "", descripcion: tarea?.descripcion ?? "", puntajeMaximo: tarea?.puntajeMaximo ?? 100,
    fechaLimite: tarea?.fechaLimite ? tarea.fechaLimite.slice(0, 16) : "" });
  const [error, setError] = useState(null);
  const [campos, setCampos] = useState({});
  const [trabajando, setTrabajando] = useState(false);
  const enviar = async (e) => {
    e.preventDefault();
    setError(null); setCampos({}); setTrabajando(true);
    try {
      const cuerpo = { titulo: f.titulo.trim(), descripcion: f.descripcion.trim() || null, puntajeMaximo: Number(f.puntajeMaximo), fechaLimite: f.fechaLimite || null };
      onGuardado(tarea ? await api.tareas.editar(tarea.id, cuerpo) : await api.tareas.crear(moduloId, cuerpo), Boolean(tarea));
    } catch (err) { setError(mensajeError(err)); setCampos(detallesError(err) ?? {}); } finally { setTrabajando(false); }
  };
  const listo = f.titulo.trim() && Number(f.puntajeMaximo) > 0;
  return (
    <form onSubmit={enviar} noValidate className="grid gap-4">
      {!tarea && <Banner tono="info">Al crearla, los estudiantes inscritos reciben una notificación.</Banner>}
      {error && <Banner tono="err" rol="alert">{error}</Banner>}
      <TextField label="Título" required maxLength={200} value={f.titulo} onChange={(e) => setF({ ...f, titulo: e.target.value })} error={campos.titulo} />
      <TextField multilinea label="Instrucciones" value={f.descripcion} onChange={(e) => setF({ ...f, descripcion: e.target.value })} />
      <TextField label="Puntaje máximo" type="number" min={1} step="0.5" required value={f.puntajeMaximo} onChange={(e) => setF({ ...f, puntajeMaximo: e.target.value })} error={campos.puntajeMaximo} />
      <TextField label="Fecha límite (opcional)" type="datetime-local" value={f.fechaLimite} onChange={(e) => setF({ ...f, fechaLimite: e.target.value })}
        ayuda="Sin fecha límite, los estudiantes pueden entregar en cualquier momento." />
      <div className="flex flex-wrap gap-3">
        <Button type="submit" cargando={trabajando} disabled={!listo} motivo={!listo ? "Escribe el título y un puntaje mayor que 0." : undefined}>Guardar tarea</Button>
        <Button variante="secondary" onClick={onCancelar}>Cancelar</Button>
      </div>
    </form>
  );
}

function Calificar({ entrega, tarea, onListo }) {
  const [puntaje, setPuntaje] = useState(entrega.puntaje ?? "");
  const [retro, setRetro] = useState(entrega.retroalimentacion ?? "");
  const [error, setError] = useState(null);
  const [trabajando, setTrabajando] = useState(false);
  const valido = puntaje !== "" && Number(puntaje) >= 0 && Number(puntaje) <= Number(tarea.puntajeMaximo);
  const enviar = async (e) => {
    e.preventDefault(); setError(null); setTrabajando(true);
    try { await api.tareas.calificar(entrega.id, { puntaje: Number(puntaje), retroalimentacion: retro.trim() || null }); onListo(); }
    catch (err) { setError(mensajeError(err)); } finally { setTrabajando(false); }
  };
  return (
    <form onSubmit={enviar} noValidate className="mt-3 grid gap-3 rounded-opt bg-subtle p-3">
      {error && <Banner tono="err" rol="alert">{error}</Banner>}
      <TextField label={`Puntaje (0 a ${tarea.puntajeMaximo})`} type="number" min={0} max={tarea.puntajeMaximo} step="0.5" value={puntaje} onChange={(e) => setPuntaje(e.target.value)}
        error={puntaje !== "" && !valido ? `Debe estar entre 0 y ${tarea.puntajeMaximo}.` : undefined} />
      <TextField multilinea label="Retroalimentación (opcional)" value={retro} onChange={(e) => setRetro(e.target.value)} />
      <div><Button type="submit" cargando={trabajando} disabled={!valido} motivo={!valido ? "Escribe un puntaje válido." : undefined}>{entrega.estado === "CALIFICADA" ? "Actualizar calificación" : "Calificar"}</Button></div>
    </form>
  );
}

function Entregas({ tarea }) {
  const { datos, cargando, error, recargar } = useAsync(() => api.tareas.entregas(tarea.id), [tarea.id]);
  if (cargando && !datos) return <Cargando texto="Cargando entregas…" />;
  if (error && !datos) return <EstadoError mensaje={error} onReintentar={recargar} />;
  if (datos.length === 0) return <p>Aún no hay entregas para esta tarea.</p>;
  return (
    <ul className="grid gap-4">
      {datos.map((e) => (
        <li key={e.id} className="rounded-opt border border-line p-4">
          <p className="flex flex-wrap items-center gap-2 font-bold text-strong">{e.estudiante}
            <Badge tono={e.estado === "CALIFICADA" ? "ok" : "warn"}>{e.estado === "CALIFICADA" ? `Calificada: ${e.puntaje}/${tarea.puntajeMaximo}` : "Pendiente de calificar"}</Badge></p>
          <p className="text-sm">{e.correo} · entregó el {fmtFecha(e.fechaEntrega)}</p>
          {e.texto && <p className="mt-2 whitespace-pre-wrap">{e.texto}</p>}
          {e.enlace && <p className="mt-2"><a className="font-bold text-primary-600 underline" href={e.enlace} target="_blank" rel="noreferrer">Abrir enlace del estudiante<span className="sr-only"> (pestaña nueva)</span></a></p>}
          <Calificar entrega={e} tarea={tarea} onListo={recargar} />
        </li>
      ))}
    </ul>
  );
}

/** Tareas calificables de un módulo (vista del instructor). */
export default function TareasModulo({ moduloId }) {
  const { avisar } = useNotif();
  const { datos, cargando, error, recargar } = useAsync(() => api.tareas.delModuloInstructor(moduloId), [moduloId]);
  const [form, setForm] = useState(null);       // { tarea? }
  const [entregas, setEntregas] = useState(null);
  const [borrar, setBorrar] = useState(null);

  return (
    <div className="mt-4 border-t border-line pt-4">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
        <h4 className="flex items-center gap-2 text-lg font-bold text-strong"><ClipboardList size={20} aria-hidden="true" />Tareas calificables</h4>
        <Button variante="secondary" icono={Plus} onClick={() => setForm({})}>Nueva tarea<span className="sr-only"> en este módulo</span></Button>
      </div>
      {cargando && !datos ? <Cargando /> : error && !datos ? <EstadoError mensaje={error} onReintentar={recargar} /> : datos.length === 0 ? (
        <p className="rounded-opt bg-subtle p-3 text-sm">Sin tareas.</p>
      ) : (
        <ul className="grid gap-2">
          {datos.map((t) => (
            <li key={t.id} className="flex flex-wrap items-center justify-between gap-3 rounded-opt border border-line p-3">
              <div className="min-w-0">
                <p className="font-bold text-strong">{t.titulo}</p>
                <p className="mt-1 flex flex-wrap items-center gap-2 text-sm">
                  <Badge tono="gris">{t.puntajeMaximo} puntos</Badge>
                  <span>{t.entregas} entrega(s), {t.calificadas} calificada(s)</span>
                  {t.fechaLimite && <span>Límite: {fmtFecha(t.fechaLimite)}</span>}
                </p>
              </div>
              <div className="flex flex-wrap gap-2">
                <Button icono={ListChecks} onClick={() => setEntregas(t)}>Entregas<span className="sr-only"> de {t.titulo}</span></Button>
                <Button variante="secondary" icono={Pencil} onClick={() => setForm({ tarea: t })}>Editar<span className="sr-only"> {t.titulo}</span></Button>
                <Button variante="ghost" icono={Trash2} onClick={() => setBorrar(t)}>Eliminar<span className="sr-only"> {t.titulo}</span></Button>
              </div>
            </li>
          ))}
        </ul>
      )}
      <Dialogo abierto={Boolean(form)} onCerrar={() => setForm(null)} titulo={form?.tarea ? "Editar tarea" : "Nueva tarea"} ancho="max-w-2xl">
        {form && <FormularioTarea moduloId={moduloId} tarea={form.tarea} onCancelar={() => setForm(null)}
          onGuardado={(_, editada) => { avisar(editada ? "Tarea actualizada." : "Tarea creada y notificada a los estudiantes.", "ok"); setForm(null); recargar(); }} />}
      </Dialogo>
      <Dialogo abierto={Boolean(entregas)} onCerrar={() => { setEntregas(null); recargar(); }} titulo={`Entregas: ${entregas?.titulo ?? ""}`} ancho="max-w-3xl">
        {entregas && <Entregas tarea={entregas} />}
      </Dialogo>
      <ConfirmarDialogo abierto={Boolean(borrar)} titulo="¿Eliminar esta tarea?" textoConfirmar="Sí, eliminar" onCerrar={() => setBorrar(null)}
        onConfirmar={async () => { await api.tareas.borrar(borrar.id); avisar("Tarea eliminada.", "ok"); recargar(); }}>
        <p>Vas a eliminar «{borrar?.titulo}». Solo se puede si no tiene entregas.</p>
      </ConfirmarDialogo>
    </div>
  );
}
