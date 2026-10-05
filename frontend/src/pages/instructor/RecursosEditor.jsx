import { useState } from "react";
import { Trash2 } from "lucide-react";
import { api, detallesError, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { Badge, Banner, Button, Cargando, EstadoError, SelectField, TextField } from "../../components/ui/index.jsx";
import ConfirmarDialogo from "../../components/ConfirmarDialogo.jsx";
import { TIPOS_RECURSO } from "./constantes.js";

function FormularioRecurso({ contenidoId, onAgregado }) {
  const { avisar } = useNotif();
  const [tipo, setTipo] = useState("TRANSCRIPCION");
  const [url, setUrl] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [error, setError] = useState(null);
  const [campos, setCampos] = useState({});
  const [trabajando, setTrabajando] = useState(false);
  const def = TIPOS_RECURSO[tipo];
  const falta = def.usaUrl ? !url.trim() : !descripcion.trim();
  const motivo = def.usaUrl ? "Escribe el enlace del recurso." : "Escribe el texto del recurso.";

  const enviar = async (e) => {
    e.preventDefault();
    setError(null);
    setCampos({});
    setTrabajando(true);
    try {
      await api.instructor.crearRecurso(contenidoId, {
        tipo,
        url: def.usaUrl ? url.trim() : url.trim() || null,
        descripcion: descripcion.trim() || null,
      });
      avisar(`Agregaste el recurso: ${def.texto}.`, "ok");
      setUrl("");
      setDescripcion("");
      onAgregado();
    } catch (err) {
      setError(mensajeError(err, "No pudimos agregar el recurso. Revisa los datos e intenta de nuevo."));
      setCampos(detallesError(err) ?? {});
    } finally {
      setTrabajando(false);
    }
  };

  return (
    <form onSubmit={enviar} className="grid gap-4 rounded-opt border border-line bg-subtle p-4" noValidate>
      <h4 className="text-lg font-bold">Agregar un recurso accesible</h4>
      {error && <Banner tono="err">{error}</Banner>}
      <SelectField label="Tipo de recurso" value={tipo} onChange={(e) => setTipo(e.target.value)}>
        {Object.entries(TIPOS_RECURSO).map(([v, t]) => <option key={v} value={v}>{t.texto}</option>)}
      </SelectField>
      <p className="text-sm text-body">{def.ayuda}</p>
      {def.usaUrl ? (
        <>
          <TextField label="Enlace (URL)" type="url" required maxLength={500} value={url} onChange={(e) => setUrl(e.target.value)} error={campos.url}
            placeholder="https://…" />
          <TextField label="Nota (opcional)" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} error={campos.descripcion} />
        </>
      ) : (
        <TextField multilinea label="Texto" required value={descripcion} onChange={(e) => setDescripcion(e.target.value)} error={campos.descripcion} />
      )}
      <div>
        <Button type="submit" cargando={trabajando} disabled={falta} motivo={falta ? motivo : undefined}>Agregar recurso</Button>
      </div>
    </form>
  );
}

/** Lista y gestiona los recursos accesibles de un contenido (subtítulos, transcripción, etc.). */
export default function RecursosEditor({ contenidoId, onCambio }) {
  const { avisar } = useNotif();
  const { datos: recursos, cargando, error, recargar } = useAsync(() => api.instructor.recursos(contenidoId), [contenidoId]);
  const [aBorrar, setABorrar] = useState(null);

  const cambio = async () => { await recargar(); onCambio?.(); };

  return (
    <section aria-labelledby="h-recursos" className="mt-6 grid gap-4 border-t border-line pt-5">
      <h3 id="h-recursos" className="text-xl">Recursos accesibles</h3>
      <p className="text-body">Estos recursos permiten que más personas aprendan con este contenido: subtítulos, transcripciones, audio, lengua de señas o versiones más simples.</p>
      {cargando && !recursos && <Cargando texto="Cargando recursos…" />}
      {error && <EstadoError mensaje={error} onReintentar={recargar} />}
      {recursos && recursos.length === 0 && <Banner tono="warn">Este contenido todavía no tiene recursos accesibles. Agrega al menos los que pide su formato para poder publicarlo.</Banner>}
      {recursos && recursos.length > 0 && (
        <ul className="grid gap-2">
          {recursos.map((r) => (
            <li key={r.id} className="flex flex-wrap items-start justify-between gap-3 rounded-opt border border-line p-3">
              <div className="min-w-0">
                <Badge tono="primario">{TIPOS_RECURSO[r.tipo]?.texto ?? r.tipo}</Badge>
                {r.url && <p className="mt-1 break-all text-sm"><a href={r.url} target="_blank" rel="noreferrer" className="font-bold text-primary-600 underline">{r.url}<span className="sr-only"> (se abre en otra pestaña)</span></a></p>}
                {r.descripcion && <p className="mt-1 whitespace-pre-line text-sm text-body">{r.descripcion.length > 300 ? `${r.descripcion.slice(0, 300)}…` : r.descripcion}</p>}
                {!r.disponible && <p className="mt-1 text-sm font-bold text-warn-fg">Este recurso no está disponible por ahora.</p>}
              </div>
              <Button variante="ghost" icono={Trash2} onClick={() => setABorrar(r)}>Quitar<span className="sr-only"> {TIPOS_RECURSO[r.tipo]?.texto ?? r.tipo}</span></Button>
            </li>
          ))}
        </ul>
      )}
      <FormularioRecurso contenidoId={contenidoId} onAgregado={cambio} />

      <ConfirmarDialogo abierto={Boolean(aBorrar)} titulo="¿Quitar este recurso?" textoConfirmar="Sí, quitar recurso" onCerrar={() => setABorrar(null)}
        onConfirmar={async () => {
          await api.instructor.borrarRecurso(aBorrar.id);
          avisar("Quitaste el recurso.", "ok");
          await cambio();
        }}>
        <p>Vas a quitar «{aBorrar ? TIPOS_RECURSO[aBorrar.tipo]?.texto ?? aBorrar.tipo : ""}». Si el contenido ya está publicado, puede dejar de cumplir con accesibilidad.</p>
      </ConfirmarDialogo>
    </section>
  );
}
