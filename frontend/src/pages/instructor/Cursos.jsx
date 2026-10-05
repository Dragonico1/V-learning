import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Archive, BookOpen, CircleCheck, Pencil, Plus, Send } from "lucide-react";
import { api, detallesError, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { ESTADO_CURSO_TEXTO } from "../../utils/format.js";
import { Badge, Banner, Button, Card, Cargando, Dialogo, EstadoError, PageHeader, TextField, Vacio } from "../../components/ui/index.jsx";
import { tonoEstadoCurso } from "./constantes.js";

function FormularioCurso({ onCreado, onCancelar }) {
  const [titulo, setTitulo] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [error, setError] = useState(null);
  const [campos, setCampos] = useState({});
  const [trabajando, setTrabajando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();
    setError(null);
    setCampos({});
    setTrabajando(true);
    try {
      const creado = await api.instructor.crearCurso({ titulo: titulo.trim(), descripcion: descripcion.trim() || null });
      onCreado(creado);
    } catch (err) {
      setError(mensajeError(err, "No pudimos crear el curso. Revisa los datos e intenta de nuevo."));
      setCampos(detallesError(err) ?? {});
    } finally {
      setTrabajando(false);
    }
  };

  return (
    <form onSubmit={enviar} className="grid gap-4" noValidate>
      {error && <Banner tono="err">{error}</Banner>}
      <TextField label="Título del curso" required maxLength={200} value={titulo} onChange={(e) => setTitulo(e.target.value)} error={campos.titulo} />
      <TextField multilinea label="Descripción" value={descripcion} onChange={(e) => setDescripcion(e.target.value)}
        ayuda="Cuenta en pocas palabras de qué trata el curso. Es opcional." error={campos.descripcion} />
      <div className="flex flex-wrap gap-3">
        <Button type="submit" cargando={trabajando} disabled={!titulo.trim()} motivo={!titulo.trim() ? "Escribe el título del curso." : undefined}>Crear curso</Button>
        <Button variante="secondary" onClick={onCancelar} disabled={trabajando}>Cancelar</Button>
      </div>
    </form>
  );
}

/** Lista de cursos propios con acciones de publicar y archivar. */
export default function Cursos() {
  const { avisar } = useNotif();
  const [params] = useSearchParams();
  const q = (params.get("q") ?? "").trim().toLowerCase();
  const { datos: cursos, cargando, error, recargar } = useAsync(() => api.instructor.cursos(), []);
  const [nuevo, setNuevo] = useState(false);
  const [ocupado, setOcupado] = useState(null);
  const [errores, setErrores] = useState({});

  const accion = async (curso, tipo) => {
    setOcupado(`${tipo}-${curso.id}`);
    setErrores((x) => ({ ...x, [curso.id]: null }));
    try {
      if (tipo === "publicar") await api.instructor.publicarCurso(curso.id);
      else await api.instructor.archivarCurso(curso.id);
      avisar(tipo === "publicar" ? `Publicaste «${curso.titulo}».` : `Archivaste «${curso.titulo}».`, "ok");
      await recargar();
    } catch (e) {
      const msg = mensajeError(e, tipo === "publicar" ? "No pudimos publicar el curso." : "No pudimos archivar el curso.");
      setErrores((x) => ({ ...x, [curso.id]: msg }));
      avisar(msg, "error");
    } finally {
      setOcupado(null);
    }
  };

  const visibles = (cursos ?? []).filter((c) => !q || c.titulo.toLowerCase().includes(q));

  return (
    <>
      <PageHeader eyebrow="Instructor" titulo="Mis cursos" subtitulo="Crea cursos, edítalos y publícalos cuando todo su contenido sea accesible."
        acciones={<Button icono={Plus} onClick={() => setNuevo(true)}>Nuevo curso</Button>} />

      {q && <p className="mb-4 text-body" role="status">Mostrando cursos con «{params.get("q")}»: {visibles.length}.</p>}
      {cargando && !cursos && <Cargando texto="Cargando tus cursos…" />}
      {error && <EstadoError mensaje={error} onReintentar={recargar} />}

      {cursos && visibles.length === 0 && (
        <Vacio icono={BookOpen} titulo={q ? "No encontramos cursos con ese título" : "Todavía no tienes cursos"}>
          <p>{q ? "Prueba con otra palabra o borra la búsqueda." : "Usa «Nuevo curso» para empezar. Será un borrador hasta que lo publiques."}</p>
        </Vacio>
      )}

      {visibles.length > 0 && (
        <ul className="grid gap-4 lg:grid-cols-2">
          {visibles.map((c) => (
            <li key={c.id}>
              <Card className="flex h-full flex-col gap-3">
                <div>
                  <Badge tono={tonoEstadoCurso(c.estado)} icono={c.estado === "PUBLICADO" ? CircleCheck : undefined}>{ESTADO_CURSO_TEXTO[c.estado] ?? c.estado}</Badge>
                  <h2 className="mt-2 text-xl">{c.titulo}</h2>
                  {c.descripcion && <p className="mt-1 text-body">{c.descripcion}</p>}
                </div>
                {errores[c.id] && <Banner tono="err">{errores[c.id]}</Banner>}
                <div className="mt-auto flex flex-wrap items-start gap-3">
                  <Button to={`/cursos/${c.id}/editar`} icono={Pencil}>Editar</Button>
                  <Button variante="secondary" icono={Send} onClick={() => accion(c, "publicar")} cargando={ocupado === `publicar-${c.id}`}
                    disabled={c.estado === "PUBLICADO"} motivo={c.estado === "PUBLICADO" ? "Este curso ya está publicado." : undefined}>Publicar curso</Button>
                  <Button variante="ghost" icono={Archive} onClick={() => accion(c, "archivar")} cargando={ocupado === `archivar-${c.id}`}
                    disabled={c.estado === "ARCHIVADO"} motivo={c.estado === "ARCHIVADO" ? "Este curso ya está archivado." : undefined}>Archivar</Button>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}

      <Dialogo abierto={nuevo} onCerrar={() => setNuevo(false)} titulo="Nuevo curso">
        <FormularioCurso onCancelar={() => setNuevo(false)}
          onCreado={(c) => { setNuevo(false); avisar(`Creaste el curso «${c.titulo}» como borrador.`, "ok"); recargar(); }} />
      </Dialogo>
    </>
  );
}
