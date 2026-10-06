import { useState } from "react";
import { ClipboardList } from "lucide-react";
import { api, detallesError, mensajeError } from "../api/client.js";
import { useNotif } from "../context/NotifContext.jsx";
import { fmtFecha } from "../utils/format.js";
import { Badge, Banner, Button, Card, Dialogo, TextField } from "./ui/index.jsx";

function Estado({ t }) {
  if (t.estado === "CALIFICADA") return <Badge tono="ok">Calificada: {t.puntaje} de {t.puntajeMaximo}</Badge>;
  if (t.estado === "ENTREGADA") return <Badge tono="primario">Entregada, pendiente de calificar</Badge>;
  if (t.vencida) return <Badge tono="err">Plazo vencido</Badge>;
  return <Badge tono="warn">Por entregar</Badge>;
}

function FormularioEntrega({ tarea, onListo, onCancelar }) {
  const [texto, setTexto] = useState(tarea.texto ?? "");
  const [enlace, setEnlace] = useState(tarea.enlace ?? "");
  const [error, setError] = useState(null);
  const [campos, setCampos] = useState({});
  const [trabajando, setTrabajando] = useState(false);
  const vacio = !texto.trim() && !enlace.trim();
  const enviar = async (e) => {
    e.preventDefault();
    setError(null); setCampos({}); setTrabajando(true);
    try { onListo(await api.tareas.entregar(tarea.id, { texto, enlace })); }
    catch (err) { setError(mensajeError(err)); setCampos(detallesError(err) ?? {}); } finally { setTrabajando(false); }
  };
  return (
    <form onSubmit={enviar} noValidate className="grid gap-4">
      {tarea.descripcion && <p className="whitespace-pre-wrap">{tarea.descripcion}</p>}
      {error && <Banner tono="err" rol="alert">{error}</Banner>}
      <TextField multilinea label="Tu respuesta" maxLength={10000} value={texto} onChange={(e) => setTexto(e.target.value)} error={campos.texto} />
      <TextField label="Enlace a tu trabajo (opcional)" type="url" placeholder="https://" value={enlace} onChange={(e) => setEnlace(e.target.value)}
        ayuda="Por ejemplo un documento en la nube o un repositorio." error={campos.enlace} />
      <div className="flex flex-wrap gap-3">
        <Button type="submit" cargando={trabajando} disabled={vacio} motivo={vacio ? "Escribe tu respuesta o agrega un enlace." : undefined}>
          {tarea.estado ? "Reenviar entrega" : "Enviar entrega"}
        </Button>
        <Button variante="secondary" onClick={onCancelar}>Cancelar</Button>
      </div>
    </form>
  );
}

/** Tarea calificable vista por el estudiante: estado, nota, retroalimentación y entrega. */
export default function TareaEstudianteCard({ tarea, onCambio, acciones }) {
  const { avisar } = useNotif();
  const [abierto, setAbierto] = useState(false);
  const calificada = tarea.estado === "CALIFICADA";
  const bloqueada = calificada || (tarea.vencida && !tarea.estado);
  return (
    <Card as="li" className="list-none">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="flex items-center gap-2 text-lg"><ClipboardList size={20} aria-hidden="true" className="text-primary-600" />{tarea.titulo}</h3>
          <p className="mt-1 flex flex-wrap items-center gap-2 text-sm">
            <Badge tono="gris">Tarea</Badge><Estado t={tarea} />
            <span>{tarea.puntajeMaximo} puntos</span>
            {tarea.fechaLimite && <span>Entrega hasta: {fmtFecha(tarea.fechaLimite)}</span>}
          </p>
          {tarea.descripcion && <p className="mt-2 line-clamp-3 whitespace-pre-wrap">{tarea.descripcion}</p>}
        </div>
        <div className="flex flex-wrap gap-2">
          {acciones}
          <Button disabled={bloqueada} motivo={calificada ? "Ya fue calificada." : bloqueada ? "El plazo de entrega venció." : undefined} onClick={() => setAbierto(true)}>
            {tarea.estado ? "Editar entrega" : "Entregar"}<span className="sr-only"> {tarea.titulo}</span>
          </Button>
        </div>
      </div>
      {tarea.fechaEntrega && <p className="mt-2 text-sm">Última entrega: {fmtFecha(tarea.fechaEntrega)}</p>}
      {calificada && tarea.retroalimentacion && <Banner tono="info" className="mt-3"><strong>Retroalimentación del instructor:</strong> {tarea.retroalimentacion}</Banner>}
      <Dialogo abierto={abierto} onCerrar={() => setAbierto(false)} titulo={tarea.titulo} ancho="max-w-2xl">
        {abierto && <FormularioEntrega tarea={tarea} onCancelar={() => setAbierto(false)}
          onListo={() => { avisar("Entrega guardada.", "ok"); setAbierto(false); onCambio?.(); }} />}
      </Dialogo>
    </Card>
  );
}
