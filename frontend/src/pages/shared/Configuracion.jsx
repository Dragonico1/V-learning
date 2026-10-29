import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, mensajeError } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { VARK } from "../../utils/format.js";
import { fijarSonido, reproducirAviso, sonidoActivado } from "../../utils/sonido.js";
import { Banner, Button, Card, Cargando, EstadoError, OpcionCard, PageHeader, PasswordField, SelectField, Toggle } from "../../components/ui/index.jsx";
import VolverACursos from "../../components/VolverACursos.jsx";

function Notificaciones() {
  const { avisar } = useNotif();
  const { datos, cargando, error, recargar } = useAsync(() => api.notificaciones.preferencias(), []);
  const [habilitadas, setHabilitadas] = useState(true);
  const [canal, setCanal] = useState("PLATAFORMA");
  const [trabajando, setTrabajando] = useState(false);
  const [sonido, setSonido] = useState(sonidoActivado);
  const cambiarSonido = (v) => { setSonido(v); fijarSonido(v); if (v) reproducirAviso(); };
  useEffect(() => { if (datos) { setHabilitadas(datos.habilitadas); setCanal(datos.canal ?? "PLATAFORMA"); } }, [datos]);
  if (cargando && !datos) return <Cargando />;
  if (error && !datos) return <EstadoError mensaje={error} onReintentar={recargar} />;
  const guardar = async () => {
    setTrabajando(true);
    try { await api.notificaciones.guardarPreferencias(habilitadas, canal); avisar("Guardamos tus preferencias de notificación.", "ok"); }
    catch (e) { avisar(mensajeError(e), "error"); } finally { setTrabajando(false); }
  };
  return (
    <Card as="section" aria-labelledby="nt">
      <h2 id="nt" className="mb-2 text-xl">Notificaciones</h2>
      <Toggle label="Recibir recordatorios y avisos" descripcion="Cursos con avance detenido, evaluaciones pendientes y logros." checked={habilitadas} onChange={setHabilitadas} />
      <Toggle label="Sonido al recibir una notificación" descripcion="Un tono corto en este dispositivo. El aviso visual siempre aparece." checked={sonido} onChange={cambiarSonido} />
      <SelectField label="¿Por dónde quieres recibirlas?" value={canal} disabled={!habilitadas} onChange={(e) => setCanal(e.target.value)} className="mb-4 max-w-sm">
        <option value="PLATAFORMA">Solo en la plataforma</option>
        <option value="CORREO">Plataforma y correo institucional</option>
      </SelectField>
      <Button cargando={trabajando} onClick={guardar}>Guardar notificaciones</Button>
    </Card>
  );
}

/** Verificación en dos pasos: obligatoria para instructor y administrador; el estudiante puede desactivarla. */
function DobleFactor() {
  const { usuario, setUsuario } = useAuth();
  const { avisar } = useNotif();
  const [confirmando, setConfirmando] = useState(false);
  const [password, setPassword] = useState("");
  const [trabajando, setTrabajando] = useState(false);
  const [error, setError] = useState(null);
  const activo = usuario.dobleFactorHabilitado !== false;

  if (usuario.dobleFactorObligatorio) {
    return (
      <Card as="section" aria-labelledby="df">
        <h2 id="df" className="mb-2 text-xl">Verificación en dos pasos</h2>
        <Banner tono="info">Es obligatoria para tu rol: cada vez que inicies sesión te enviaremos un código a tu correo institucional.</Banner>
      </Card>
    );
  }

  const cambiar = async (habilitado, pass) => {
    setTrabajando(true);
    setError(null);
    try {
      const u = await api.auth.dobleFactor(habilitado, pass);
      setUsuario(u);
      setConfirmando(false);
      setPassword("");
      avisar(habilitado ? "Activaste la verificación en dos pasos." : "Desactivaste la verificación en dos pasos.", "ok");
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setTrabajando(false);
    }
  };

  return (
    <Card as="section" aria-labelledby="df">
      <h2 id="df" className="mb-2 text-xl">Verificación en dos pasos</h2>
      <Toggle
        label="Pedir un código al iniciar sesión"
        descripcion="Te enviamos un código de 6 dígitos a tu correo institucional. Recomendado para proteger tu cuenta."
        checked={activo}
        disabled={trabajando}
        onChange={(v) => { setError(null); if (v) cambiar(true); else setConfirmando(true); }}
      />
      {error && <Banner tono="err" className="mt-2">{error}</Banner>}
      {activo === false && <Banner tono="warn" className="mt-2">Sin este paso, tu cuenta solo está protegida por la contraseña.</Banner>}
      {confirmando && activo && (
        <form className="mt-3 grid max-w-sm gap-3" onSubmit={(e) => { e.preventDefault(); cambiar(false, password); }}>
          <PasswordField label="Confirma tu contraseña para desactivarla" autoComplete="current-password" required
            value={password} onChange={(e) => setPassword(e.target.value)} />
          <div className="flex flex-wrap gap-3">
            <Button type="submit" cargando={trabajando} disabled={!password}>Desactivar</Button>
            <Button variante="secondary" onClick={() => { setConfirmando(false); setPassword(""); setError(null); }}>Cancelar</Button>
          </div>
        </form>
      )}
    </Card>
  );
}

function Metodos() {
  const { avisar } = useNotif();
  const { datos, cargando, error, recargar } = useAsync(() => api.vark.metodos(), []);
  const [principal, setPrincipal] = useState(null);
  const [secundario, setSecundario] = useState(null);
  const [trabajando, setTrabajando] = useState(false);
  useEffect(() => { if (datos) { setPrincipal(datos.principal); setSecundario(datos.secundario ?? null); } }, [datos]);
  if (cargando && !datos) return <Cargando />;
  if (error && !datos) return <EstadoError mensaje={error} onReintentar={recargar} />;
  const guardar = async () => {
    setTrabajando(true);
    try { await api.vark.guardarMetodos(principal, secundario); avisar("Actualizamos tus métodos de aprendizaje.", "ok"); }
    catch (e) { avisar(mensajeError(e), "error"); } finally { setTrabajando(false); }
  };
  if (!datos.principal && !datos.ultimoResultado) {
    return <Card><h2 className="mb-2 text-xl">Métodos de aprendizaje</h2><p>Aún no has hecho el test. <Link className="font-bold underline" to="/onboarding/test">Hacer el test VARK</Link></p></Card>;
  }
  return (
    <Card as="section" aria-labelledby="mt">
      <h2 id="mt" className="mb-2 text-xl">Métodos de aprendizaje</h2>
      <p className="mb-3">Define qué formato ves primero en los cursos.</p>
      <div role="radiogroup" aria-label="Método principal" className="mb-4 grid gap-2">
        {Object.entries(VARK).map(([k, v]) => (
          <OpcionCard key={k} titulo={v.nombre} descripcion={v.cambia} seleccionada={principal === k}
            onSelect={() => { setPrincipal(k); if (secundario === k) setSecundario(null); }} etiqueta={datos.ultimoResultado?.estiloPredominante === k ? "Recomendado para ti" : undefined} />
        ))}
      </div>
      <SelectField label="Segundo método (opcional)" value={secundario ?? ""} onChange={(e) => setSecundario(e.target.value || null)} className="mb-4 max-w-sm">
        <option value="">Ninguno</option>
        {Object.entries(VARK).filter(([k]) => k !== principal).map(([k, v]) => <option key={k} value={k}>{v.nombre}</option>)}
      </SelectField>
      <div className="flex flex-wrap gap-3">
        <Button cargando={trabajando} disabled={!principal} onClick={guardar}>Guardar métodos</Button>
        <Button variante="secondary" to="/onboarding/test">Repetir el test</Button>
      </div>
    </Card>
  );
}

export default function Configuracion() {
  const { usuario } = useAuth();
  return (
    <>
      <VolverACursos />
      <PageHeader eyebrow="Configuración" titulo="Tus preferencias" subtitulo="Notificaciones, seguridad, métodos de aprendizaje y accesibilidad." />
      <div className="grid max-w-3xl gap-6">
        <Notificaciones />
        <DobleFactor />
        {usuario.rol === "ESTUDIANTE" && <Metodos />}
        <Banner tono="info">Los ajustes de texto, contraste y teclado están en <Link className="font-bold underline" to="/accesibilidad">Accesibilidad</Link>.</Banner>
      </div>
    </>
  );
}
