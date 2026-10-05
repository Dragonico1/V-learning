import { useState } from "react";
import { mensajeError } from "../api/client.js";
import { Banner, Button, Dialogo } from "./ui/index.jsx";

/**
 * Confirmación antes de una acción que no se puede deshacer.
 * `onConfirmar` debe ser async y lanzar el error si falla; el mensaje se muestra dentro del diálogo.
 */
export default function ConfirmarDialogo({ abierto, titulo, textoConfirmar = "Sí, continuar", onConfirmar, onCerrar, children }) {
  const [trabajando, setTrabajando] = useState(false);
  const [error, setError] = useState(null);

  const cerrar = () => { setError(null); onCerrar(); };

  const confirmar = async () => {
    setTrabajando(true);
    setError(null);
    try {
      await onConfirmar();
      setError(null);
      onCerrar();
    } catch (e) {
      setError(mensajeError(e, "No pudimos completar la acción. Intenta de nuevo."));
    } finally {
      setTrabajando(false);
    }
  };

  return (
    <Dialogo abierto={abierto} onCerrar={cerrar} titulo={titulo}>
      <div className="grid gap-4">
        <div className="text-body">{children}</div>
        {error && <Banner tono="err">{error}</Banner>}
        <div className="flex flex-wrap gap-3">
          <Button onClick={confirmar} cargando={trabajando}>{textoConfirmar}</Button>
          <Button variante="secondary" onClick={cerrar} disabled={trabajando}>Cancelar</Button>
        </div>
      </div>
    </Dialogo>
  );
}
