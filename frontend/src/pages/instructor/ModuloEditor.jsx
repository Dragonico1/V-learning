import { useState } from "react";
import { api, detallesError, mensajeError } from "../../api/client.js";
import { Banner, Button, TextField } from "../../components/ui/index.jsx";

/** Formulario para crear o editar un módulo. Si dejas el orden vacío, se coloca al final. */
export default function ModuloEditor({ cursoId, modulo, onGuardado, onCancelar }) {
  const editando = Boolean(modulo);
  const [titulo, setTitulo] = useState(modulo?.titulo ?? "");
  const [descripcion, setDescripcion] = useState(modulo?.descripcion ?? "");
  const [orden, setOrden] = useState(modulo?.orden ? String(modulo.orden) : "");
  const [error, setError] = useState(null);
  const [campos, setCampos] = useState({});
  const [trabajando, setTrabajando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();
    setError(null);
    setCampos({});
    setTrabajando(true);
    const datos = { titulo: titulo.trim(), descripcion: descripcion.trim() || null, orden: orden ? Number(orden) : null };
    try {
      const r = editando ? await api.instructor.editarModulo(modulo.id, datos) : await api.instructor.crearModulo(cursoId, datos);
      onGuardado(r, editando);
    } catch (err) {
      setError(mensajeError(err, "No pudimos guardar el módulo. Revisa los datos e intenta de nuevo."));
      setCampos(detallesError(err) ?? {});
    } finally {
      setTrabajando(false);
    }
  };

  return (
    <form onSubmit={enviar} className="grid gap-4" noValidate>
      {error && <Banner tono="err">{error}</Banner>}
      <TextField label="Título del módulo" required maxLength={200} value={titulo} onChange={(e) => setTitulo(e.target.value)} error={campos.titulo} />
      <TextField multilinea label="Descripción" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} error={campos.descripcion}
        ayuda="Explica en una o dos frases qué se aprende en este módulo." />
      <TextField label="Posición (orden)" type="number" min={1} inputMode="numeric" value={orden} onChange={(e) => setOrden(e.target.value)}
        error={campos.orden} ayuda="El 1 va primero. Si lo dejas vacío, el módulo queda al final." className="max-w-xs" />
      <div className="flex flex-wrap gap-3">
        <Button type="submit" cargando={trabajando} disabled={!titulo.trim()} motivo={!titulo.trim() ? "Escribe el título del módulo." : undefined}>
          {editando ? "Guardar cambios" : "Crear módulo"}
        </Button>
        <Button variante="secondary" onClick={onCancelar} disabled={trabajando}>Cancelar</Button>
      </div>
    </form>
  );
}
