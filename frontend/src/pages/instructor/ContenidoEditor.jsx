import { useState } from "react";
import { CheckCircle2, EyeOff, Globe } from "lucide-react";
import { api, detallesError, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { Banner, Button, Cargando, EstadoError, SelectField, TextField } from "../../components/ui/index.jsx";
import RecursosEditor from "./RecursosEditor.jsx";
import { FORMATOS } from "./constantes.js";

/** Datos del contenido: título, formato, enlace y cuerpo de texto estructurado. */
function FormularioContenido({ moduloId, contenido, onGuardado, onCancelar }) {
  const editando = Boolean(contenido?.id);
  const [titulo, setTitulo] = useState(contenido?.titulo ?? "");
  const [descripcion, setDescripcion] = useState(contenido?.descripcion ?? "");
  const [duracion, setDuracion] = useState(contenido?.duracionMinutos ? String(contenido.duracionMinutos) : "");
  const [formato, setFormato] = useState(contenido?.formato ?? "LECTURA");
  const [urlRecurso, setUrlRecurso] = useState(contenido?.urlRecurso ?? "");
  const [cuerpo, setCuerpo] = useState(contenido?.cuerpo ?? "");
  const [error, setError] = useState(null);
  const [campos, setCampos] = useState({});
  const [trabajando, setTrabajando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();
    setError(null);
    setCampos({});
    setTrabajando(true);
    const datos = {
      titulo: titulo.trim(),
      descripcion: descripcion.trim() || null,
      duracionMinutos: duracion ? Number(duracion) : null,
      formato,
      urlRecurso: urlRecurso.trim() || null,
      cuerpo: cuerpo.trim() || null,
    };
    try {
      const r = editando ? await api.instructor.editarContenido(contenido.id, datos) : await api.instructor.crearContenido(moduloId, datos);
      onGuardado(r, editando);
    } catch (err) {
      setError(mensajeError(err, "No pudimos guardar el contenido. Revisa los datos e intenta de nuevo."));
      setCampos(detallesError(err) ?? {});
    } finally {
      setTrabajando(false);
    }
  };

  return (
    <form onSubmit={enviar} className="grid gap-4" noValidate>
      {error && <Banner tono="err">{error}</Banner>}
      <TextField label="Título del contenido" required maxLength={200} value={titulo} onChange={(e) => setTitulo(e.target.value)} error={campos.titulo} />
      <div className="grid gap-4 sm:grid-cols-2">
        <SelectField label="Formato" value={formato} onChange={(e) => setFormato(e.target.value)} error={campos.formato}>
          {FORMATOS.map((f) => <option key={f.valor} value={f.valor}>{f.texto}</option>)}
        </SelectField>
        <TextField label="Duración en minutos (opcional)" type="number" min={0} inputMode="numeric" value={duracion} onChange={(e) => setDuracion(e.target.value)} error={campos.duracionMinutos} />
      </div>
      <TextField multilinea label="Descripción" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} error={campos.descripcion}
        ayuda="Resume en una o dos frases de qué trata." />
      {formato !== "LECTURA" && (
        <TextField label="Enlace del recurso (video, audio o simulación)" type="url" maxLength={500} value={urlRecurso}
          onChange={(e) => setUrlRecurso(e.target.value)} error={campos.urlRecurso} placeholder="https://…"
          ayuda="Usa un enlace que se pueda reproducir en el navegador (por ejemplo un archivo .mp4 o .mp3)." />
      )}
      <div>
        <TextField multilinea label={formato === "LECTURA" ? "Texto de la lección" : "Texto de apoyo (opcional)"} value={cuerpo}
          onChange={(e) => setCuerpo(e.target.value)} error={campos.cuerpo} rows={10} className="[&_textarea]:min-h-56" />
        <details className="mt-2 rounded-opt bg-subtle p-3 text-sm">
          <summary className="cursor-pointer font-bold text-strong">Cómo escribir el texto para que sea accesible</summary>
          <ul className="mt-2 list-disc space-y-1 pl-5">
            <li>Usa <code>## Título</code> para separar secciones; cada una será una unidad corta en el apoyo cognitivo.</li>
            <li>Escribe pasos como lista numerada: <code>1. Primer paso</code>.</li>
            <li>Para el glosario crea una sección <code>## Glosario</code> con líneas <code>Término: definición</code>.</li>
            <li>Cada imagen necesita texto alternativo: <code>![Descripción de la imagen](ruta)</code>. Si falta, avisaremos al equipo y mostraremos una descripción genérica.</li>
          </ul>
        </details>
      </div>
      <div className="flex flex-wrap gap-3">
        <Button type="submit" cargando={trabajando} disabled={!titulo.trim()} motivo={!titulo.trim() ? "Escribe el título del contenido." : undefined}>
          {editando ? "Guardar cambios" : "Crear contenido"}
        </Button>
        <Button variante="secondary" onClick={onCancelar} disabled={trabajando}>{editando ? "Cerrar" : "Cancelar"}</Button>
      </div>
    </form>
  );
}

/** Estado de conformidad y publicación (RF-019): un video sin subtítulos y transcripción no se publica. */
function Publicacion({ contenidoId, publicado, onCambio, refresco }) {
  const { avisar } = useNotif();
  const { datos, cargando, error, recargar } = useAsync(() => api.instructor.conformidad(contenidoId), [contenidoId, refresco]);
  const [trabajando, setTrabajando] = useState(false);
  const [rechazo, setRechazo] = useState(null);

  const cambiar = async (publicar) => {
    setTrabajando(true);
    setRechazo(null);
    try {
      const r = publicar ? await api.instructor.publicarContenido(contenidoId) : await api.instructor.despublicarContenido(contenidoId);
      avisar(publicar ? "Contenido publicado." : "Contenido despublicado.", "ok");
      r.advertencias?.forEach((a) => avisar(a, "info"));
      onCambio(r.publicado);
      recargar();
    } catch (err) {
      setRechazo(mensajeError(err, "No pudimos cambiar la publicación. Intenta de nuevo."));
    } finally {
      setTrabajando(false);
    }
  };

  return (
    <section aria-labelledby="h-pub" className="mt-6 grid gap-3 border-t border-line pt-5">
      <h3 id="h-pub" className="text-xl">Conformidad y publicación</h3>
      {cargando && !datos && <Cargando texto="Revisando conformidad…" />}
      {error && <EstadoError mensaje={error} onReintentar={recargar} />}
      {datos && datos.conforme && <Banner tono="ok"><span className="flex items-center gap-2"><CheckCircle2 size={18} aria-hidden="true" />Cumple con los requisitos de accesibilidad de su formato.</span></Banner>}
      {datos && !datos.conforme && (
        <Banner tono="warn">
          <p className="font-bold">No se puede publicar todavía. Falta:</p>
          <ul className="mt-1 list-disc pl-5 font-normal">{datos.faltantes.map((f) => <li key={f}>{f}</li>)}</ul>
        </Banner>
      )}
      {datos?.advertencias?.length > 0 && (
        <Banner tono="info"><ul className="list-disc pl-5 font-normal">{datos.advertencias.map((a) => <li key={a}>{a}</li>)}</ul></Banner>
      )}
      {rechazo && <Banner tono="err">{rechazo}</Banner>}
      <div>
        {publicado ? (
          <Button variante="secondary" icono={EyeOff} cargando={trabajando} onClick={() => cambiar(false)}>Despublicar contenido</Button>
        ) : (
          <Button icono={Globe} cargando={trabajando} disabled={datos ? !datos.conforme : true}
            motivo={datos && !datos.conforme ? "Agrega los recursos que faltan para poder publicar." : undefined} onClick={() => cambiar(true)}>Publicar contenido</Button>
        )}
      </div>
    </section>
  );
}

/** Edición completa de un contenido: datos, recursos accesibles y publicación. */
export default function ContenidoEditor({ moduloId, contenido, onGuardado, onCancelar, onCambio }) {
  const [version, setVersion] = useState(0);
  const [publicado, setPublicado] = useState(Boolean(contenido?.publicado));
  const editando = Boolean(contenido?.id);
  const { datos: completo, cargando, error, recargar } = useAsync(
    () => (editando ? api.contenidos.ver(contenido.id) : Promise.resolve(null)), [contenido?.id]);

  if (editando && cargando && !completo) return <Cargando texto="Cargando contenido…" />;
  if (editando && error) return <EstadoError mensaje={error} onReintentar={recargar} />;

  const inicial = completo ? { ...contenido, descripcion: completo.descripcion, urlRecurso: completo.urlRecurso, cuerpo: completo.cuerpo, duracionMinutos: completo.duracionMinutos } : contenido;
  return (
    <div>
      <FormularioContenido moduloId={moduloId} contenido={inicial} onGuardado={onGuardado} onCancelar={onCancelar} />
      {editando && (
        <>
          <RecursosEditor contenidoId={contenido.id} onCambio={() => { setVersion((v) => v + 1); onCambio?.(); }} />
          <Publicacion contenidoId={contenido.id} publicado={publicado} refresco={version}
            onCambio={(p) => { setPublicado(p); onCambio?.(); }} />
        </>
      )}
    </div>
  );
}
