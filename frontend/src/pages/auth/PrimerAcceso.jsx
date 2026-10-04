import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, mensajeError } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { destinoInicial } from "../../utils/destino.js";
import { AuthShell } from "../../components/layout/Shells.jsx";
import { PasswordRules } from "../../components/PasswordRules.jsx";
import { passwordValida } from "../../utils/password.js";
import { Banner, Button, PasswordField } from "../../components/ui/index.jsx";

/** Primer acceso: la contraseña temporal debe cambiarse antes de usar la plataforma. */
export default function PrimerAcceso() {
  const { usuario, setUsuario, cerrarSesion } = useAuth();
  const navigate = useNavigate();
  const [actual, setActual] = useState("");
  const [nueva, setNueva] = useState("");
  const [repetir, setRepetir] = useState("");
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

  const coincide = nueva === repetir;
  const motivo = !actual ? "Escribe tu contraseña temporal." : !passwordValida(nueva) ? "La contraseña nueva aún no cumple los requisitos."
    : !coincide ? "Las dos contraseñas nuevas deben ser iguales." : undefined;

  const enviar = async (e) => {
    e.preventDefault();
    setError(null);
    setCargando(true);
    try {
      const u = await api.auth.primerAcceso(actual, nueva);
      setUsuario(u);
      navigate(destinoInicial(u), { replace: true });
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  };

  return (
    <AuthShell ayuda={false}>
      <h1 className="text-3xl">Hola, {usuario.nombre.split(" ")[0]}</h1>
      <p className="mt-1.5 text-lg">Por seguridad, cambia la contraseña temporal que recibiste antes de continuar.</p>
      {error && <Banner tono="err" className="mt-5">{error}</Banner>}
      <form onSubmit={enviar} className="mt-6 grid gap-5" noValidate>
        <PasswordField label="Contraseña temporal" autoComplete="current-password" value={actual} onChange={(e) => setActual(e.target.value)} />
        <div>
          <PasswordField label="Contraseña nueva" autoComplete="new-password" value={nueva} onChange={(e) => setNueva(e.target.value)} />
          <PasswordRules valor={nueva} />
        </div>
        <PasswordField label="Repite la contraseña nueva" autoComplete="new-password" value={repetir} onChange={(e) => setRepetir(e.target.value)}
          error={repetir && !coincide ? "Las contraseñas no coinciden." : undefined} />
        <Button type="submit" bloque cargando={cargando} disabled={Boolean(motivo)} motivo={motivo}>Guardar y continuar</Button>
        <button type="button" onClick={async () => { await cerrarSesion(); navigate("/login", { replace: true }); }} className="font-bold text-primary-600 underline">Salir y continuar después</button>
      </form>
    </AuthShell>
  );
}
