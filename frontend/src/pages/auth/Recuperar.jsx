import { useState } from "react";
import { Link } from "react-router-dom";
import { api, mensajeError } from "../../api/client.js";
import { AuthShell } from "../../components/layout/Shells.jsx";
import { Banner, Button, TextField } from "../../components/ui/index.jsx";

/** RF-003: solicita el enlace de un solo uso. La respuesta es genérica: no revela si el correo existe. */
export default function Recuperar() {
  const [correo, setCorreo] = useState("");
  const [respuesta, setRespuesta] = useState(null);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();
    setError(null);
    setCargando(true);
    try {
      setRespuesta(await api.auth.recuperar(correo.trim()));
    } catch (err) {
      setError(mensajeError(err));
    } finally {
      setCargando(false);
    }
  };

  return (
    <AuthShell>
      <h1 className="text-3xl">Recupera tu contraseña</h1>
      <p className="mt-1.5 text-lg">Escribe tu correo institucional y te enviaremos un enlace válido por 15 minutos.</p>
      {error && <Banner tono="err" className="mt-5">{error}</Banner>}
      {respuesta ? (
        <div className="mt-6 grid gap-4">
          <Banner tono="ok" rol="status">{respuesta.mensaje}</Banner>
          {respuesta.enlaceRecuperacion && (
            <Banner tono="warn"><strong>Modo desarrollo:</strong> abre este enlace para continuar:{" "}
              <a className="break-all underline" href={respuesta.enlaceRecuperacion}>{respuesta.enlaceRecuperacion}</a></Banner>
          )}
          <Link to="/login" className="font-bold text-primary-600 underline">Volver al inicio de sesión</Link>
        </div>
      ) : (
        <form onSubmit={enviar} className="mt-6 grid gap-5" noValidate>
          <TextField label="Correo institucional" type="email" autoComplete="username" required value={correo} onChange={(e) => setCorreo(e.target.value)} />
          <Button type="submit" bloque cargando={cargando} disabled={!correo.trim()} motivo={!correo.trim() ? "Escribe tu correo para continuar." : undefined}>Enviar enlace</Button>
          <p className="text-center"><Link to="/login" className="font-bold text-primary-600 underline">Volver al inicio de sesión</Link></p>
        </form>
      )}
    </AuthShell>
  );
}
