import { useState } from "react";
import { Plus, Trash2 } from "lucide-react";
import { api, detallesError, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { Banner, Button, Checkbox, SelectField, TextField } from "../../components/ui/index.jsx";
import { TIPOS_EVALUACION, TIPOS_PREGUNTA } from "./constantes.js";

/** Crea o edita una evaluación. Si el servidor dice que ya tiene intentos, se muestra su mensaje. */
export function FormularioEvaluacion({ moduloId, evaluacion, onGuardado, onCancelar }) {
  const editando = Boolean(evaluacion?.id);
  const [titulo, setTitulo] = useState(evaluacion?.titulo ?? "");
  const [descripcion, setDescripcion] = useState(evaluacion?.descripcion ?? "");
  const [tipo, setTipo] = useState(evaluacion?.tipo ?? "QUIZ");
  const [puntaje, setPuntaje] = useState(evaluacion?.puntajeMaximo != null ? String(evaluacion.puntajeMaximo) : "10");
  const [tiempo, setTiempo] = useState(evaluacion?.tiempoLimite ? String(evaluacion.tiempoLimite) : "");
  const [fechaLimite, setFechaLimite] = useState(evaluacion?.fechaLimite ? evaluacion.fechaLimite.slice(0, 16) : "");
  const [alternativa, setAlternativa] = useState(true);
  const [error, setError] = useState(null);
  const [campos, setCampos] = useState({});
  const [trabajando, setTrabajando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();
    setError(null);
    setCampos({});
    setTrabajando(true);
    const datos = {
      titulo: titulo.trim(), descripcion: descripcion.trim() || null, tipo, puntajeMaximo: Number(puntaje),
      tiempoLimite: tiempo ? Number(tiempo) : null, alternativaAccesible: alternativa,
      fechaLimite: fechaLimite || null,
    };
    try {
      const r = editando ? await api.instructor.editarEvaluacion(evaluacion.id, datos) : await api.instructor.crearEvaluacion(moduloId, datos);
      onGuardado(r, editando);
    } catch (err) {
      setError(mensajeError(err, "No pudimos guardar la evaluación. Revisa los datos e intenta de nuevo."));
      setCampos(detallesError(err) ?? {});
    } finally {
      setTrabajando(false);
    }
  };

  const incompleto = !titulo.trim() || !(Number(puntaje) >= 0) || puntaje === "";
  return (
    <form onSubmit={enviar} className="grid gap-4" noValidate>
      {error && <Banner tono="err">{error}</Banner>}
      <TextField label="Título" required maxLength={200} value={titulo} onChange={(e) => setTitulo(e.target.value)} error={campos.titulo} />
      <TextField multilinea label="Descripción (opcional)" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} error={campos.descripcion} />
      <div className="grid gap-4 sm:grid-cols-3">
        <SelectField label="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value)}>
          {TIPOS_EVALUACION.map((t) => <option key={t.valor} value={t.valor}>{t.texto}</option>)}
        </SelectField>
        <TextField label="Puntaje máximo" type="number" min={0} step="0.5" required value={puntaje} onChange={(e) => setPuntaje(e.target.value)} error={campos.puntajeMaximo} />
        <TextField label="Tiempo límite (minutos)" type="number" min={1} inputMode="numeric" value={tiempo} onChange={(e) => setTiempo(e.target.value)} error={campos.tiempoLimite}
          ayuda="Vacío = sin límite. Se amplía con el tiempo adicional de cada estudiante." />
      </div>
      <TextField label="Fecha límite (opcional)" type="datetime-local" value={fechaLimite} onChange={(e) => setFechaLimite(e.target.value)} error={campos.fechaLimite}
        ayuda="Después de esta fecha los estudiantes ya no pueden empezar la evaluación (un intento abierto sí se puede terminar). Si ya hay intentos, puedes ampliarla o quitarla." />
      <Checkbox checked={alternativa} onChange={setAlternativa}>Tiene alternativa accesible (se puede responder solo con teclado, sin arrastrar)</Checkbox>
      {!alternativa && <Banner tono="warn">Si no tiene alternativa accesible, se excluirá de la calificación de estudiantes con perfil motor y te avisaremos.</Banner>}
      <div className="flex flex-wrap gap-3">
        <Button type="submit" cargando={trabajando} disabled={incompleto} motivo={incompleto ? "Completa el título y el puntaje." : undefined}>
          {editando ? "Guardar cambios" : "Crear evaluación"}
        </Button>
        <Button variante="secondary" onClick={onCancelar} disabled={trabajando}>Cancelar</Button>
      </div>
    </form>
  );
}

const opcionVacia = () => ({ texto: "", correcta: false });

/** Agrega una pregunta con sus opciones. En «abierta», las opciones son las respuestas aceptadas. */
export function FormularioPregunta({ evaluacionId, siguienteOrden, onGuardado, onCancelar }) {
  const { avisar } = useNotif();
  const [enunciado, setEnunciado] = useState("");
  const [tipo, setTipo] = useState("SELECCION_UNICA");
  const [puntaje, setPuntaje] = useState("1");
  const [orden, setOrden] = useState(String(siguienteOrden));
  const [opciones, setOpciones] = useState([opcionVacia(), opcionVacia()]);
  const [error, setError] = useState(null);
  const [trabajando, setTrabajando] = useState(false);

  const cambiarTipo = (t) => {
    setTipo(t);
    if (t === "VERDADERO_FALSO") setOpciones([{ texto: "Verdadero", correcta: true }, { texto: "Falso", correcta: false }]);
    else if (t === "ABIERTA") setOpciones([{ texto: "", correcta: true }]);
    else setOpciones((o) => (o.length >= 2 && tipo !== "VERDADERO_FALSO" && tipo !== "ABIERTA" ? o : [opcionVacia(), opcionVacia()]));
  };

  const poner = (i, parcial) => setOpciones((lista) => lista.map((o, j) => {
    if (j !== i) return tipo === "SELECCION_UNICA" && parcial.correcta ? { ...o, correcta: false } : o;
    return { ...o, ...parcial };
  }));

  const fija = tipo === "VERDADERO_FALSO";
  const abierta = tipo === "ABIERTA";
  const llenas = opciones.filter((o) => o.texto.trim());
  const hayCorrecta = abierta ? llenas.length > 0 : llenas.some((o) => o.correcta);
  const motivo = !enunciado.trim() ? "Escribe el enunciado de la pregunta." : llenas.length < (abierta ? 1 : 2) ? (abierta ? "Escribe al menos una respuesta aceptada." : "Escribe al menos dos opciones.")
    : !hayCorrecta ? "Marca cuál opción es la correcta." : undefined;

  const enviar = async (e) => {
    e.preventDefault();
    setError(null);
    setTrabajando(true);
    try {
      const r = await api.instructor.agregarPregunta(evaluacionId, {
        enunciado: enunciado.trim(), tipo, puntaje: Number(puntaje), orden: Number(orden),
        opciones: llenas.map((o) => ({ texto: o.texto.trim(), correcta: abierta ? true : o.correcta })),
      });
      avisar(`Pregunta ${r.orden} agregada.`, "ok");
      onGuardado(r);
    } catch (err) {
      setError(mensajeError(err, "No pudimos agregar la pregunta. Revisa los datos e intenta de nuevo."));
    } finally {
      setTrabajando(false);
    }
  };

  return (
    <form onSubmit={enviar} className="grid gap-4" noValidate>
      {error && <Banner tono="err">{error}</Banner>}
      <TextField multilinea label="Enunciado" required value={enunciado} onChange={(e) => setEnunciado(e.target.value)} />
      <div className="grid gap-4 sm:grid-cols-3">
        <SelectField label="Tipo de pregunta" value={tipo} onChange={(e) => cambiarTipo(e.target.value)}>
          {TIPOS_PREGUNTA.map((t) => <option key={t.valor} value={t.valor}>{t.texto}</option>)}
        </SelectField>
        <TextField label="Puntaje" type="number" min={0} step="0.5" value={puntaje} onChange={(e) => setPuntaje(e.target.value)} />
        <TextField label="Posición" type="number" min={1} inputMode="numeric" value={orden} onChange={(e) => setOrden(e.target.value)} />
      </div>
      <fieldset className="grid gap-2">
        <legend className="mb-1 font-bold text-strong">{abierta ? "Respuestas aceptadas" : "Opciones de respuesta"}</legend>
        {abierta && <p className="text-sm text-body">Se comparan sin tildes ni mayúsculas. Agrega las variantes que quieras aceptar.</p>}
        {opciones.map((o, i) => (
          <div key={i} className="flex flex-wrap items-center gap-3">
            <div className="min-w-48 flex-1">
              <TextField label={`${abierta ? "Respuesta" : "Opción"} ${i + 1}`} value={o.texto} disabled={fija}
                onChange={(e) => poner(i, { texto: e.target.value })} />
            </div>
            {!abierta && (
              <div className="pt-7">
                <Checkbox checked={o.correcta} onChange={(v) => poner(i, { correcta: v })}>Es correcta</Checkbox>
              </div>
            )}
            {!fija && opciones.length > (abierta ? 1 : 2) && (
              <div className="pt-7">
                <Button variante="ghost" icono={Trash2} onClick={() => setOpciones((l) => l.filter((_, j) => j !== i))}>Quitar<span className="sr-only"> opción {i + 1}</span></Button>
              </div>
            )}
          </div>
        ))}
        {!fija && opciones.length < 8 && (
          <div><Button variante="secondary" icono={Plus} onClick={() => setOpciones((l) => [...l, opcionVacia()])}>{abierta ? "Agregar otra respuesta" : "Agregar opción"}</Button></div>
        )}
      </fieldset>
      <div className="flex flex-wrap gap-3">
        <Button type="submit" cargando={trabajando} disabled={Boolean(motivo)} motivo={motivo}>Agregar pregunta</Button>
        <Button variante="secondary" onClick={onCancelar} disabled={trabajando}>Cerrar</Button>
      </div>
    </form>
  );
}
