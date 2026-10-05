import { useState } from "react";
import { KeyRound, Plus, Search } from "lucide-react";
import { api, mensajeError, detallesError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { ESTADO_USUARIO_TEXTO, ROL_TEXTO, fmtFecha } from "../../utils/format.js";
import { Badge, Banner, Button, Card, Cargando, Dialogo, EstadoError, PageHeader, SelectField, TextField, Vacio } from "../../components/ui/index.jsx";

const TONO_ESTADO = { ACTIVO: "ok", PENDIENTE_PRIMER_ACCESO: "warn", BLOQUEADO: "err", INACTIVO: "gris" };

function Credenciales({ r, onCerrar }) {
  return (
    <div className="grid gap-4 p-1">
      <Banner tono={r.enviadoPorCorreo ? "ok" : "warn"} rol="status">{r.mensaje}</Banner>
      {r.contrasenaTemporal && (
        <div>
          <p className="font-bold text-strong">Contraseña temporal (entrégala por un canal seguro)</p>
          <p className="mt-1 select-all rounded-opt border border-line bg-subtle p-3 font-mono text-lg" data-testid="pass-temporal">{r.contrasenaTemporal}</p>
          <p className="mt-1 text-sm">Solo se muestra ahora. La persona deberá cambiarla en su primer acceso.</p>
        </div>
      )}
      <div><Button onClick={onCerrar}>Entendido</Button></div>
    </div>
  );
}

function FormularioUsuario({ onCreado, onCancelar }) {
  const [f, setF] = useState({ nombre: "", correo: "", rol: "ESTUDIANTE", programaAcademico: "", especialidad: "", codigo: "" });
  const [error, setError] = useState(null);
  const [campos, setCampos] = useState({});
  const [trabajando, setTrabajando] = useState(false);
  const set = (k) => (e) => setF((x) => ({ ...x, [k]: e.target.value }));
  const enviar = async (e) => {
    e.preventDefault();
    setError(null); setCampos({});
    setTrabajando(true);
    try {
      const cuerpo = { ...f, programaAcademico: f.programaAcademico || null, especialidad: f.especialidad || null, codigo: f.codigo || null };
      onCreado(await api.admin.crearUsuario(cuerpo));
    } catch (err) {
      setError(mensajeError(err));
      setCampos(detallesError(err) ?? {});
    } finally { setTrabajando(false); }
  };
  const listo = f.nombre.trim() && f.correo.trim();
  return (
    <form onSubmit={enviar} noValidate className="grid gap-4">
      {error && <Banner tono="err">{error}</Banner>}
      <TextField label="Nombre completo" required value={f.nombre} onChange={set("nombre")} error={campos.nombre} />
      <TextField label="Correo institucional" type="email" required value={f.correo} onChange={set("correo")} error={campos.correo} />
      <SelectField label="Rol" value={f.rol} onChange={set("rol")}>
        {Object.entries(ROL_TEXTO).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
      </SelectField>
      {f.rol === "ESTUDIANTE" && <TextField label="Programa académico" value={f.programaAcademico} onChange={set("programaAcademico")} error={campos.programaAcademico} />}
      {f.rol === "INSTRUCTOR" && <TextField label="Especialidad" value={f.especialidad} onChange={set("especialidad")} error={campos.especialidad} />}
      {f.rol !== "ADMINISTRADOR" && <TextField label="Código (opcional, se genera si lo dejas vacío)" value={f.codigo} onChange={set("codigo")} error={campos.codigo} />}
      <div className="flex flex-wrap gap-3">
        <Button type="submit" cargando={trabajando} disabled={!listo} motivo={!listo ? "Completa nombre y correo." : undefined}>Crear usuario</Button>
        <Button variante="secondary" onClick={onCancelar}>Cancelar</Button>
      </div>
    </form>
  );
}

/** RF-001: gestión de usuarios. */
export default function Usuarios() {
  const { avisar } = useNotif();
  const [filtros, setFiltros] = useState({ q: "", rol: "", estado: "" });
  const [aplicados, setAplicados] = useState(filtros);
  const [pagina, setPagina] = useState(0);
  const { datos, cargando, error, recargar } = useAsync(
    () => api.admin.usuarios({ q: aplicados.q || undefined, rol: aplicados.rol || undefined, estado: aplicados.estado || undefined, pagina, tamano: 20 }),
    [aplicados, pagina]);
  const [crear, setCrear] = useState(false);
  const [cred, setCred] = useState(null);

  const buscar = (e) => { e.preventDefault(); setPagina(0); setAplicados(filtros); };
  const cambiarEstado = async (u, estado) => {
    try { await api.admin.estadoUsuario(u.id, estado); avisar(`${u.nombre}: ahora está ${ESTADO_USUARIO_TEXTO[estado].toLowerCase()}.`, "ok"); recargar(); }
    catch (e) { avisar(mensajeError(e), "error"); }
  };
  const reenviar = async (u) => {
    try { setCred(await api.admin.reenviar(u.id)); recargar(); } catch (e) { avisar(mensajeError(e), "error"); }
  };
  const paginas = datos ? Math.max(1, Math.ceil(datos.total / datos.tamano)) : 1;

  return (
    <>
      <PageHeader eyebrow="Administración" titulo="Usuarios" subtitulo="Crea cuentas, cambia su estado y reenvía credenciales."
        acciones={<Button icono={Plus} onClick={() => setCrear(true)}>Nuevo usuario</Button>} />
      <Card as="form" onSubmit={buscar} role="search" aria-label="Filtrar usuarios" className="mb-5 grid gap-4 md:grid-cols-[2fr_1fr_1fr_auto] md:items-end">
        <TextField label="Nombre o correo" value={filtros.q} onChange={(e) => setFiltros({ ...filtros, q: e.target.value })} />
        <SelectField label="Rol" value={filtros.rol} onChange={(e) => setFiltros({ ...filtros, rol: e.target.value })}>
          <option value="">Todos</option>
          {Object.entries(ROL_TEXTO).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
        </SelectField>
        <SelectField label="Estado" value={filtros.estado} onChange={(e) => setFiltros({ ...filtros, estado: e.target.value })}>
          <option value="">Todos</option>
          {Object.entries(ESTADO_USUARIO_TEXTO).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
        </SelectField>
        <Button type="submit" icono={Search}>Buscar</Button>
      </Card>

      {cargando && !datos ? <Cargando texto="Cargando usuarios…" /> : error && !datos ? <EstadoError mensaje={error} onReintentar={recargar} /> : datos.contenido.length === 0 ? (
        <Vacio titulo="No hay usuarios con esos filtros">Prueba con otra búsqueda.</Vacio>
      ) : (
        <>
          <p className="mb-2 text-sm" role="status">{datos.total} usuario(s) encontrados.</p>
          <div className="overflow-x-auto rounded-card border border-line bg-surface">
            <table className="w-full min-w-[760px] text-left">
              <caption className="sr-only">Usuarios de la plataforma</caption>
              <thead className="bg-subtle text-sm"><tr>
                <th scope="col" className="p-3">Nombre</th><th scope="col" className="p-3">Rol</th><th scope="col" className="p-3">Estado</th>
                <th scope="col" className="p-3">Último acceso</th><th scope="col" className="p-3">Acciones</th></tr></thead>
              <tbody>
                {datos.contenido.map((u) => (
                  <tr key={u.id} className="border-t border-line align-top">
                    <th scope="row" className="p-3 font-normal"><span className="block font-bold text-strong">{u.nombre}</span><span className="text-sm">{u.correo}</span></th>
                    <td className="p-3">{ROL_TEXTO[u.rol]}</td>
                    <td className="p-3"><Badge tono={TONO_ESTADO[u.estado]}>{ESTADO_USUARIO_TEXTO[u.estado]}</Badge></td>
                    <td className="p-3 text-sm">{fmtFecha(u.ultimoAcceso)}</td>
                    <td className="p-3">
                      <div className="flex flex-wrap gap-2">
                        {u.estado === "BLOQUEADO" || u.estado === "INACTIVO"
                          ? <Button variante="secondary" onClick={() => cambiarEstado(u, "ACTIVO")}>Activar<span className="sr-only"> a {u.nombre}</span></Button>
                          : <Button variante="secondary" onClick={() => cambiarEstado(u, "INACTIVO")}>Desactivar<span className="sr-only"> a {u.nombre}</span></Button>}
                        {u.estado === "ACTIVO" && <Button variante="ghost" onClick={() => cambiarEstado(u, "BLOQUEADO")}>Bloquear<span className="sr-only"> a {u.nombre}</span></Button>}
                        <Button variante="ghost" icono={KeyRound} onClick={() => reenviar(u)}>Reenviar credenciales<span className="sr-only"> a {u.nombre}</span></Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav aria-label="Paginación" className="mt-4 flex items-center gap-3">
            <Button variante="secondary" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>Anterior</Button>
            <span>Página {pagina + 1} de {paginas}</span>
            <Button variante="secondary" disabled={pagina + 1 >= paginas} onClick={() => setPagina(pagina + 1)}>Siguiente</Button>
          </nav>
        </>
      )}

      <Dialogo abierto={crear} onCerrar={() => setCrear(false)} titulo="Nuevo usuario">
        {crear && <FormularioUsuario onCancelar={() => setCrear(false)} onCreado={(r) => { setCrear(false); setCred(r); recargar(); }} />}
      </Dialogo>
      <Dialogo abierto={Boolean(cred)} onCerrar={() => setCred(null)} titulo="Credenciales de acceso">
        {cred && <Credenciales r={cred} onCerrar={() => setCred(null)} />}
      </Dialogo>
    </>
  );
}
