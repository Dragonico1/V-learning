import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api, mensajeError } from "../../api/client.js";
import { AuthShell } from "../../components/layout/Shells.jsx";
import { PasswordRules } from "../../components/PasswordRules.jsx";
import { passwordValida } from "../../utils/password.js";
import { Banner, Button, PasswordField } from "../../components/ui/index.jsx";

/** RF-003: abre el enlace e ingresa la nueva contraseña. Enlace vencido o usado → pide otro. */
export default function Restablecer() {
  const [params] = useSearchParams();
  const tokenRec = params.get("token") ?? "";
  const [nueva, setNueva] = useState("");
  const [repetir, setRepetir] = useState("");
  const [error, setError] = useState(null);
  const [hecho, setHecho] = useState(null);
  const [cargando, setCargando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();
    setError(null);
    setCargando(true);
    try {
      const r = await api.auth.restablecer(tokenRec, nueva);
      setHecho(r.mensaje);
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  };

  const coincide = nueva === repetir;
  const motivo = !passwordValida(nueva) ? "La contraseña aún no cumple los requisitos." : !coincide ? "Las dos contraseñas deben ser iguales." : undefined;

  return (
    <AuthShell>
      <h1 className="text-3xl">Crea una contraseña nueva</h1>
      {!tokenRec ? (
        <div className="mt-6 grid gap-4">
          <Banner tono="err">Este enlace no es válido. Pide uno nuevo.</Banner>
          <Link to="/recuperar" className="font-bold text-primary-600 underline">Pedir otro enlace</Link>
        </div>
      ) : hecho ? (
        <div className="mt-6 grid gap-4">
          <Banner tono="ok" rol="status">{hecho}</Banner>
          <Button to="/login">Ir a iniciar sesión</Button>
        </div>
      ) : (
        <form onSubmit={enviar} className="mt-6 grid gap-5" noValidate>
          {error && (
            <Banner tono="err">{error} <Link to="/recuperar" className="font-bold underline">Pedir otro enlace</Link></Banner>
          )}
          <div>
            <PasswordField label="Contraseña nueva" autoComplete="new-password" value={nueva} onChange={(e) => setNueva(e.target.value)} />
            <PasswordRules valor={nueva} />
          </div>
          <PasswordField label="Repite la contraseña" autoComplete="new-password" value={repetir} onChange={(e) => setRepetir(e.target.value)}
            error={repetir && !coincide ? "Las contraseñas no coinciden." : undefined} />
          <Button type="submit" bloque cargando={cargando} disabled={Boolean(motivo)} motivo={motivo}>Guardar contraseña</Button>
        </form>
      )}
    </AuthShell>
  );
}
