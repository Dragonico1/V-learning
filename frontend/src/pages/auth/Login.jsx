import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { ShieldCheck } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { destinoInicial } from "../../utils/destino.js";
import { AuthShell } from "../../components/layout/Shells.jsx";
import { Banner, Button, PasswordField, TextField } from "../../components/ui/index.jsx";

/** RF-002: credenciales → código OTP → sesión. Redirige según rol y lo que falte por completar. */
export default function Login() {
  const { usuario, verificarOtp, avisoSesion } = useAuth();
  const navigate = useNavigate();
  const [paso, setPaso] = useState(1);
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [codigo, setCodigo] = useState("");
  const [otpDev, setOtpDev] = useState(null);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const [cargando, setCargando] = useState(false);

  if (usuario) return <Navigate to={destinoInicial(usuario)} replace />;

  const enviarCredenciales = async (e) => {
    e.preventDefault();
    setError(null);
    setCargando(true);
    try {
      const r = await api.auth.login(correo.trim(), password);
      setOtpDev(r.codigoOtp ?? null);
      setInfo(r.mensaje);
      setPaso(2);
    } catch (err) {
      setError(mensajeError(err, "No pudimos iniciar sesión. Revisa tu correo y contraseña."));
    } finally {
      setCargando(false);
    }
  };

  const enviarCodigo = async (e) => {
    e.preventDefault();
    setError(null);
    setCargando(true);
    try {
      const u = await verificarOtp(correo.trim(), codigo.trim());
      navigate(destinoInicial(u), { replace: true });
    } catch (err) {
      setError(mensajeError(err, "El código no es válido o venció. Pide uno nuevo volviendo atrás."));
    } finally {
      setCargando(false);
    }
  };

  return (
    <AuthShell>
      <h1 className="text-3xl">{paso === 1 ? "Inicia sesión" : "Revisa tu código"}</h1>
      <p className="mt-1.5 text-lg">{paso === 1 ? "Entra con tu correo institucional para seguir aprendiendo a tu manera." : "Te enviamos un código de 6 dígitos. Escríbelo para entrar."}</p>

      {avisoSesion && paso === 1 && <Banner tono="warn" className="mt-5">{avisoSesion}</Banner>}
      {error && <Banner tono="err" className="mt-5">{error}</Banner>}

      {paso === 1 ? (
        <form onSubmit={enviarCredenciales} className="mt-6 grid gap-5" noValidate>
          <TextField label="Correo institucional" type="email" autoComplete="username" required value={correo}
            onChange={(e) => setCorreo(e.target.value)} placeholder="nombre@tdea.edu.co" />
          <PasswordField label="Contraseña" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
          <Button type="submit" bloque cargando={cargando} disabled={!correo.trim() || !password}
            motivo={!correo.trim() || !password ? "Escribe tu correo y tu contraseña para continuar." : undefined}>Continuar</Button>
          <p className="text-center"><Link to="/recuperar" className="font-bold text-primary-600 underline">¿Olvidaste tu contraseña?</Link></p>
        </form>
      ) : (
        <form onSubmit={enviarCodigo} className="mt-6 grid gap-5" noValidate>
          {info && <Banner tono="info">{info}</Banner>}
          {otpDev && <Banner tono="warn"><strong>Modo desarrollo:</strong> tu código es <span className="font-mono text-lg font-extrabold">{otpDev}</span>. En producción llega por correo.</Banner>}
          <TextField label="Código de 6 dígitos" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} required
            value={codigo} onChange={(e) => setCodigo(e.target.value.replace(/\D/g, ""))} className="max-w-xs" />
          <div className="flex flex-wrap gap-3">
            <Button type="submit" cargando={cargando} disabled={codigo.length !== 6} motivo={codigo.length !== 6 ? "Escribe los 6 dígitos." : undefined}>Entrar</Button>
            <Button variante="secondary" onClick={() => { setPaso(1); setCodigo(""); setError(null); }}>Volver</Button>
          </div>
        </form>
      )}
      <p className="mt-8 flex items-center gap-2 text-xs text-body"><ShieldCheck size={16} aria-hidden="true" className="shrink-0 text-ok-fg" />
        Tus datos se protegen con cifrado en tránsito y tu acceso se verifica en dos pasos.</p>
    </AuthShell>
  );
}
