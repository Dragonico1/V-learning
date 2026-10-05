import { useState } from "react";
import { mensajeError } from "../api/client.js";
import { useNotif } from "../context/NotifContext.jsx";
import { Banner, Button, TextField } from "./ui/index.jsx";

/** Divide por comas, punto y coma o saltos de línea; quita vacíos y repetidos. */
function separarCorreos(texto) {
  const lista = texto.split(/[\s,;]+/).map((c) => c.trim()).filter(Boolean);
  return [...new Set(lista.map((c) => c.toLowerCase()))];
}

function Lista({ titulo, correos, tono }) {
  if (!correos?.length) return null;
  return (
    <Banner tono={tono}>
      <p className="font-bold">{titulo} ({correos.length})</p>
      <ul className="mt-1 list-disc pl-5 font-normal">{correos.map((c) => <li key={c}>{c}</li>)}</ul>
    </Banner>
  );
}

/**
 * Inscribe estudiantes pegando correos. `inscribir(correos)` es la llamada al servidor
 * y devuelve { inscritos, yaInscritos, noEncontrados }.
 */
export default function InscribirEstudiantes({ inscribir, onListo }) {
  const { avisar } = useNotif();
  const [texto, setTexto] = useState("");
  const [trabajando, setTrabajando] = useState(false);
  const [error, setError] = useState(null);
  const [resultado, setResultado] = useState(null);
  const correos = separarCorreos(texto);

  const enviar = async (e) => {
    e.preventDefault();
    setError(null);
    setResultado(null);
    if (correos.length > 200) {
      setError("Puedes inscribir máximo 200 correos por vez. Divide la lista y envíala en partes.");
      return;
    }
    setTrabajando(true);
    try {
      const r = await inscribir(correos);
      setResultado(r);
      setTexto("");
      avisar(`Inscripción lista: ${r.inscritos?.length ?? 0} estudiante(s) nuevos.`, "ok");
      onListo?.();
    } catch (err) {
      setError(mensajeError(err, "No pudimos inscribir a los estudiantes. Revisa los correos e intenta de nuevo."));
    } finally {
      setTrabajando(false);
    }
  };

  return (
    <form onSubmit={enviar} className="grid gap-4" noValidate>
      <TextField multilinea label="Correos de los estudiantes" value={texto} onChange={(e) => setTexto(e.target.value)}
        ayuda="Pega uno o varios correos institucionales, separados por coma o uno por línea."
        placeholder={"ana@tdea.edu.co\nluis@tdea.edu.co"} />
      <p className="text-sm text-body" aria-live="polite">
        {correos.length === 0 ? "Aún no hay correos." : `${correos.length} correo(s) detectado(s).`}
      </p>
      {error && <Banner tono="err">{error}</Banner>}
      <div>
        <Button type="submit" cargando={trabajando} disabled={correos.length === 0}
          motivo={correos.length === 0 ? "Escribe al menos un correo para inscribir." : undefined}>Inscribir estudiantes</Button>
      </div>
      {resultado && (
        <div className="grid gap-3" role="status">
          <Lista titulo="Inscritos ahora" correos={resultado.inscritos} tono="ok" />
          <Lista titulo="Ya estaban inscritos" correos={resultado.yaInscritos} tono="info" />
          <Lista titulo="No encontramos estos correos como estudiantes; revisa que estén bien escritos y que la persona tenga cuenta"
            correos={resultado.noEncontrados} tono="warn" />
        </div>
      )}
    </form>
  );
}
