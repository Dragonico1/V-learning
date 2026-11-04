import { useEffect, useId, useRef, useState } from "react";
import { ImagePlus } from "lucide-react";
import { api, mensajeError } from "../api/client.js";
import { Banner, Button, TextField } from "./ui/index.jsx";

const TAMANO_MAXIMO = 5 * 1024 * 1024;

/** Campo reutilizable para la portada de un curso: archivo subido o enlace externo. */
export default function CampoPortada({ valor, onChange }) {
  const id = useId();
  const input = useRef(null);
  const [subiendo, setSubiendo] = useState(false);
  const [error, setError] = useState(null);
  const [rota, setRota] = useState(false);

  useEffect(() => {
    setRota(false);
  }, [valor]);

  const elegir = async (e) => {
    const archivo = e.target.files?.[0];
    e.target.value = "";
    if (!archivo) return;
    if (archivo.size > TAMANO_MAXIMO) {
      setError("La imagen no puede superar los 5 MB.");
      return;
    }
    setError(null);
    setSubiendo(true);
    try {
      const r = await api.instructor.subirPortada(archivo);
      onChange(r.url);
    } catch (err) {
      setError(mensajeError(err, "No pudimos subir la imagen."));
    } finally {
      setSubiendo(false);
    }
  };

  return (
    <fieldset className="grid gap-3">
      <legend className="mb-1.5 font-bold text-strong">Portada del curso</legend>
      <p className="text-sm text-muted">
        Adjunta una imagen (JPG, PNG, WEBP o GIF, máximo 5 MB) o pega un enlace. Si la dejas vacía, usaremos una portada acorde a la temática.
      </p>
      <div className="flex flex-wrap items-center gap-3">
        <input
          ref={input}
          id={id}
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          onChange={elegir}
          className="sr-only"
          tabIndex={-1}
        />
        <Button type="button" variante="secondary" icono={ImagePlus} cargando={subiendo} onClick={() => input.current?.click()}>
          Adjuntar imagen
        </Button>
        {valor && <Button type="button" variante="ghost" onClick={() => onChange("")}>Quitar portada</Button>}
      </div>
      <TextField
        label="O pega el enlace de la imagen"
        value={valor}
        onChange={(e) => onChange(e.target.value)}
        placeholder="https://…"
        ayuda="Puedes usar la URL de una imagen publicada en internet."
      />
      {valor && !rota && (
        <img
          src={valor}
          alt="Vista previa de la portada"
          onError={() => setRota(true)}
          className="h-32 w-full max-w-sm rounded-lg border border-line object-cover"
        />
      )}
      {valor && rota && <p className="text-sm text-muted">No pudimos cargar la vista previa. Revisa el enlace.</p>}
      {error && <Banner tono="err">{error}</Banner>}
    </fieldset>
  );
}
