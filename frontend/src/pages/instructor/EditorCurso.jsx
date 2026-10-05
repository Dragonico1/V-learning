import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Archive, ArrowLeft, Globe, Pencil, Plus, Trash2 } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { ESTADO_CURSO_TEXTO, FORMATO_TEXTO, TIPO_EVAL_TEXTO } from "../../utils/format.js";
import { Badge, Banner, Button, Card, Cargando, Dialogo, EstadoError, PageHeader, Pestanas, TextField, Vacio } from "../../components/ui/index.jsx";
import ConfirmarDialogo from "../../components/ConfirmarDialogo.jsx";
import InscribirEstudiantes from "../../components/InscribirEstudiantes.jsx";
import ModuloEditor from "./ModuloEditor.jsx";
import ContenidoEditor from "./ContenidoEditor.jsx";
import { FormularioEvaluacion, FormularioPregunta } from "./EvaluacionEditor.jsx";
import { tonoEstadoCurso } from "./constantes.js";

function DatosCurso({ curso, onGuardado }) {
  const { avisar } = useNotif();
  const [titulo, setTitulo] = useState(curso.titulo);
  const [descripcion, setDescripcion] = useState(curso.descripcion ?? "");
  const [error, setError] = useState(null);
  const [trabajando, setTrabajando] = useState(false);
  const enviar = async (e) => {
    e.preventDefault();
    setError(null);
    setTrabajando(true);
    try {
      await api.instructor.editarCurso(curso.id, { titulo: titulo.trim(), descripcion: descripcion.trim() || null });
      avisar("Guardaste los datos del curso.", "ok");
      onGuardado();
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setTrabajando(false);
    }
  };
  return (
    <Card as="form" onSubmit={enviar} noValidate className="grid max-w-2xl gap-4">
      {error && <Banner tono="err">{error}</Banner>}
      <TextField label="Título del curso" required maxLength={200} value={titulo} onChange={(e) => setTitulo(e.target.value)} />
      <TextField multilinea label="Descripción" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} />
      <div><Button type="submit" cargando={trabajando} disabled={!titulo.trim()} motivo={!titulo.trim() ? "Escribe el título del curso." : undefined}>Guardar datos</Button></div>
    </Card>
  );
}

function PestanaContenido({ curso, recargar }) {
  const { avisar } = useNotif();
  const [dlgModulo, setDlgModulo] = useState(null);      // { modulo? }
  const [dlgContenido, setDlgContenido] = useState(null); // { moduloId, contenido? }

  return (
    <div className="grid gap-5">
      <div><Button icono={Plus} onClick={() => setDlgModulo({})}>Nuevo módulo</Button></div>
      {curso.modulos.length === 0 && (
        <Vacio titulo="Este curso aún no tiene módulos">Crea el primer módulo y luego agrégale contenidos en varios formatos.</Vacio>
      )}
      {curso.modulos.map((m) => (
        <Card key={m.id} as="section" aria-labelledby={`m-${m.id}`}>
          <div className="mb-3 flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0">
              <h3 id={`m-${m.id}`}>Módulo {m.orden}: {m.titulo}</h3>
              {m.descripcion && <p className="mt-0.5 text-sm">{m.descripcion}</p>}
            </div>
            <div className="flex flex-wrap gap-2">
              <Button variante="secondary" icono={Pencil} onClick={() => setDlgModulo({ modulo: m })}>Editar<span className="sr-only"> módulo {m.titulo}</span></Button>
              <Button icono={Plus} onClick={() => setDlgContenido({ moduloId: m.id })}>Nuevo contenido<span className="sr-only"> en {m.titulo}</span></Button>
            </div>
          </div>
          {m.contenidos.length === 0 ? (
            <p className="rounded-opt bg-subtle p-3 text-sm">Sin contenidos todavía.</p>
          ) : (
            <ul className="grid gap-2">
              {m.contenidos.map((c) => (
                <li key={c.id} className="flex flex-wrap items-center justify-between gap-3 rounded-opt border border-line p-3">
                  <div className="min-w-0">
                    <p className="font-bold text-strong">{c.titulo}</p>
                    <p className="mt-1 flex flex-wrap items-center gap-2">
                      <Badge tono="primario">{FORMATO_TEXTO[c.formato]}</Badge>
                      <Badge tono={c.publicado ? "ok" : "warn"}>{c.publicado ? "Publicado" : "Borrador"}</Badge>
                      {c.conforme === false && <Badge tono="err">No conforme</Badge>}
                      {c.conforme === true && <Badge tono="ok">Accesible</Badge>}
                      {c.duracionMinutos ? <span className="text-sm">{c.duracionMinutos} min</span> : null}
                    </p>
                    {c.faltantes?.length > 0 && <p className="mt-1 text-sm text-warn-fg">Falta: {c.faltantes.join("; ")}.</p>}
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button variante="secondary" icono={Pencil} onClick={() => setDlgContenido({ moduloId: m.id, contenido: c })}>Editar<span className="sr-only"> {c.titulo}</span></Button>
                    <Button variante="ghost" to={`/contenidos/${c.id}`}>Vista previa<span className="sr-only"> de {c.titulo}</span></Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Card>
      ))}

      <Dialogo abierto={Boolean(dlgModulo)} onCerrar={() => setDlgModulo(null)} titulo={dlgModulo?.modulo ? "Editar módulo" : "Nuevo módulo"}>
        {dlgModulo && (
          <ModuloEditor cursoId={curso.id} modulo={dlgModulo.modulo} onCancelar={() => setDlgModulo(null)}
            onGuardado={(_, editado) => { avisar(editado ? "Módulo actualizado." : "Módulo creado.", "ok"); setDlgModulo(null); recargar(); }} />
        )}
      </Dialogo>
      <Dialogo abierto={Boolean(dlgContenido)} onCerrar={() => { setDlgContenido(null); recargar(); }} ancho="max-w-3xl"
        titulo={dlgContenido?.contenido ? "Editar contenido" : "Nuevo contenido"}>
        {dlgContenido && (
          <ContenidoEditor moduloId={dlgContenido.moduloId} contenido={dlgContenido.contenido}
            onCancelar={() => { setDlgContenido(null); recargar(); }} onCambio={recargar}
            onGuardado={(r, editado) => {
              avisar(editado ? "Contenido actualizado." : "Contenido creado. Ahora agrega sus recursos accesibles para poder publicarlo.", "ok");
              if (editado) { recargar(); } else { setDlgContenido({ moduloId: dlgContenido.moduloId, contenido: { id: r.id, titulo: r.titulo, formato: r.formato, publicado: r.publicado } }); recargar(); }
            }} />
        )}
      </Dialogo>
    </div>
  );
}

function PestanaEvaluaciones({ curso, recargar }) {
  const { avisar } = useNotif();
  const [crear, setCrear] = useState(null);        // { moduloId, evaluacion? }
  const [preguntas, setPreguntas] = useState(null); // { evaluacion }
  const [aBorrar, setABorrar] = useState(null);
  const [agregadas, setAgregadas] = useState({});

  return (
    <div className="grid gap-5">
      <Banner tono="info">Una evaluación se habilita para el estudiante cuando completa el módulo. Si ya tiene intentos, no se puede reescribir ni eliminar.</Banner>
      {curso.modulos.length === 0 && <Vacio titulo="Primero crea un módulo">Las evaluaciones se asocian a un módulo.</Vacio>}
      {curso.modulos.map((m) => (
        <Card key={m.id} as="section" aria-labelledby={`e-${m.id}`}>
          <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
            <h3 id={`e-${m.id}`}>Módulo {m.orden}: {m.titulo}</h3>
            <Button icono={Plus} onClick={() => setCrear({ moduloId: m.id })}>Nueva evaluación<span className="sr-only"> en {m.titulo}</span></Button>
          </div>
          {m.evaluaciones.length === 0 ? <p className="rounded-opt bg-subtle p-3 text-sm">Sin evaluaciones.</p> : (
            <ul className="grid gap-2">
              {m.evaluaciones.map((e) => (
                <li key={e.id} className="flex flex-wrap items-center justify-between gap-3 rounded-opt border border-line p-3">
                  <div className="min-w-0">
                    <p className="font-bold text-strong">{e.titulo}</p>
                    <p className="mt-1 flex flex-wrap items-center gap-2">
                      <Badge tono="primario">{TIPO_EVAL_TEXTO[e.tipo]}</Badge>
                      {!e.calificable && <Badge tono="warn">Sin alternativa accesible</Badge>}
                      {agregadas[e.id] ? <span className="text-sm">{agregadas[e.id]} pregunta(s) agregada(s) en esta sesión</span> : null}
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button icono={Plus} onClick={() => setPreguntas({ evaluacion: e })}>Agregar preguntas<span className="sr-only"> a {e.titulo}</span></Button>
                    <Button variante="secondary" icono={Pencil} onClick={() => setCrear({ moduloId: m.id, evaluacion: e })}>Editar<span className="sr-only"> {e.titulo}</span></Button>
                    <Button variante="ghost" icono={Trash2} onClick={() => setABorrar(e)}>Eliminar<span className="sr-only"> {e.titulo}</span></Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Card>
      ))}

      <Dialogo abierto={Boolean(crear)} onCerrar={() => setCrear(null)} titulo={crear?.evaluacion ? "Editar evaluación" : "Nueva evaluación"} ancho="max-w-2xl">
        {crear && (
          <FormularioEvaluacion moduloId={crear.moduloId} evaluacion={crear.evaluacion} onCancelar={() => setCrear(null)}
            onGuardado={(r, editado) => {
              avisar(editado ? "Evaluación actualizada." : "Evaluación creada. Agrégale preguntas para que los estudiantes puedan presentarla.", "ok");
              setCrear(null);
              recargar();
              if (!editado) setPreguntas({ evaluacion: { id: r.id, titulo: r.titulo } });
            }} />
        )}
      </Dialogo>
      <Dialogo abierto={Boolean(preguntas)} onCerrar={() => setPreguntas(null)} titulo={`Preguntas: ${preguntas?.evaluacion?.titulo ?? ""}`} ancho="max-w-3xl">
        {preguntas && (
          <FormularioPregunta key={(agregadas[preguntas.evaluacion.id] ?? 0)} evaluacionId={preguntas.evaluacion.id}
            siguienteOrden={(agregadas[preguntas.evaluacion.id] ?? 0) + 1} onCancelar={() => setPreguntas(null)}
            onGuardado={() => setAgregadas((a) => ({ ...a, [preguntas.evaluacion.id]: (a[preguntas.evaluacion.id] ?? 0) + 1 }))} />
        )}
      </Dialogo>
      <ConfirmarDialogo abierto={Boolean(aBorrar)} titulo="¿Eliminar esta evaluación?" textoConfirmar="Sí, eliminar" onCerrar={() => setABorrar(null)}
        onConfirmar={async () => { await api.instructor.borrarEvaluacion(aBorrar.id); avisar("Evaluación eliminada.", "ok"); recargar(); }}>
        <p>Vas a eliminar «{aBorrar?.titulo}» con todas sus preguntas. No se puede deshacer.</p>
      </ConfirmarDialogo>
    </div>
  );
}

/** Editor completo del curso (instructor): contenidos, evaluaciones, estudiantes y datos. */
export default function EditorCurso() {
  const { id } = useParams();
  const { avisar } = useNotif();
  const { datos: curso, cargando, error, recargar } = useAsync(() => api.instructor.curso(id), [id]);
  const [pestana, setPestana] = useState("contenido");
  const [confirmar, setConfirmar] = useState(null); // 'publicar' | 'archivar'

  if (cargando && !curso) return <Cargando texto="Cargando el curso…" />;
  if (error && !curso) return <EstadoError mensaje={error} onReintentar={recargar} />;
  if (!curso) return null;

  const accion = async () => {
    if (confirmar === "publicar") {
      await api.instructor.publicarCurso(curso.id);
      avisar("Curso publicado. Los estudiantes ya pueden inscribirse.", "ok");
    } else {
      await api.instructor.archivarCurso(curso.id);
      avisar("Curso archivado.", "ok");
    }
    recargar();
  };

  return (
    <>
      <p className="mb-3"><Link to="/cursos" className="inline-flex min-h-11 items-center gap-1.5 font-bold text-primary-600 underline"><ArrowLeft size={16} aria-hidden="true" />Volver a mis cursos</Link></p>
      <PageHeader eyebrow="Editor de curso" titulo={curso.titulo}
        subtitulo={curso.descripcion || "Agrega módulos, contenidos y evaluaciones."}
        acciones={(
          <>
            <Badge tono={tonoEstadoCurso(curso.estado)}>{ESTADO_CURSO_TEXTO[curso.estado]}</Badge>
            {curso.estado !== "PUBLICADO" && <Button icono={Globe} onClick={() => setConfirmar("publicar")}>Publicar curso</Button>}
            {curso.estado !== "ARCHIVADO" && <Button variante="secondary" icono={Archive} onClick={() => setConfirmar("archivar")}>Archivar</Button>}
            <Button variante="secondary" to={`/comunidad/${curso.id}`}>Comunidad</Button>
          </>
        )} />

      <Pestanas etiqueta="Secciones del curso" activa={pestana} onCambiar={setPestana} pestanas={[
        { id: "contenido", texto: "Contenido" }, { id: "evaluaciones", texto: "Evaluaciones" },
        { id: "estudiantes", texto: "Estudiantes" }, { id: "datos", texto: "Datos del curso" }]} />
      <div role="tabpanel" id={`panel-${pestana}`} aria-labelledby={`tab-${pestana}`}>
        {pestana === "contenido" && <PestanaContenido curso={curso} recargar={recargar} />}
        {pestana === "evaluaciones" && <PestanaEvaluaciones curso={curso} recargar={recargar} />}
        {pestana === "estudiantes" && (
          <Card className="max-w-2xl">
            <h2 className="mb-1 text-xl">Inscribir estudiantes</h2>
            {curso.estado !== "PUBLICADO" && <Banner tono="warn" className="mb-4">Solo puedes inscribir estudiantes cuando el curso está publicado.</Banner>}
            <InscribirEstudiantes inscribir={(correos) => api.instructor.inscribir(curso.id, correos)} />
          </Card>
        )}
        {pestana === "datos" && <DatosCurso curso={curso} onGuardado={recargar} />}
      </div>

      <ConfirmarDialogo abierto={Boolean(confirmar)} onCerrar={() => setConfirmar(null)} onConfirmar={accion}
        titulo={confirmar === "publicar" ? "¿Publicar el curso?" : "¿Archivar el curso?"}
        textoConfirmar={confirmar === "publicar" ? "Sí, publicar" : "Sí, archivar"}>
        {confirmar === "publicar"
          ? <p>El curso quedará visible para los estudiantes. El servidor revisará que tenga contenido publicado y conforme.</p>
          : <p>Los estudiantes dejarán de ver el curso en el catálogo.</p>}
      </ConfirmarDialogo>
    </>
  );
}
