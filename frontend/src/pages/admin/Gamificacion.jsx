import { useState } from "react";
import { Pencil, Plus } from "lucide-react";
import { api, mensajeError, detallesError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { Badge, Banner, Button, Card, Cargando, Checkbox, Dialogo, EstadoError, PageHeader, Pestanas, SelectField, TextField, Vacio } from "../../components/ui/index.jsx";
import { EVENTOS_GAMIFICACION } from "../instructor/constantes.js";

const textoEvento = (v) => EVENTOS_GAMIFICACION.find((e) => e.valor === v)?.texto ?? v;

function FormRegla({ regla, onGuardado, onCancelar }) {
  const [f, setF] = useState({ nombre: regla?.nombre ?? "", descripcion: regla?.descripcion ?? "", evento: regla?.evento ?? EVENTOS_GAMIFICACION[0].valor, puntos: regla?.puntos ?? 10, activo: regla?.activo ?? true });
  const [error, setError] = useState(null);
  const [campos, setCampos] = useState({});
  const [trabajando, setTrabajando] = useState(false);
  const enviar = async (e) => {
    e.preventDefault(); setError(null); setCampos({}); setTrabajando(true);
    try {
      const cuerpo = { ...f, puntos: Number(f.puntos), descripcion: f.descripcion || null };
      onGuardado(regla ? await api.admin.editarRegla(regla.id, cuerpo) : await api.admin.crearRegla(cuerpo), Boolean(regla));
    } catch (err) { setError(mensajeError(err)); setCampos(detallesError(err) ?? {}); } finally { setTrabajando(false); }
  };
  return (
    <form onSubmit={enviar} noValidate className="grid gap-4">
      {error && <Banner tono="err">{error}</Banner>}
      <TextField label="Nombre" required value={f.nombre} onChange={(e) => setF({ ...f, nombre: e.target.value })} error={campos.nombre} />
      <TextField multilinea label="Descripción" value={f.descripcion} onChange={(e) => setF({ ...f, descripcion: e.target.value })} />
      <SelectField label="Evento que otorga puntos" value={f.evento} onChange={(e) => setF({ ...f, evento: e.target.value })} error={campos.evento}>
        {EVENTOS_GAMIFICACION.map((ev) => <option key={ev.valor} value={ev.valor}>{ev.texto}</option>)}
      </SelectField>
      <TextField label="Puntos" type="number" min={0} required value={f.puntos} onChange={(e) => setF({ ...f, puntos: e.target.value })} error={campos.puntos} />
      <Checkbox checked={f.activo} onChange={(v) => setF({ ...f, activo: v })}>Regla activa</Checkbox>
      <div className="flex flex-wrap gap-3">
        <Button type="submit" cargando={trabajando} disabled={!f.nombre.trim()} motivo={!f.nombre.trim() ? "Escribe el nombre." : undefined}>Guardar regla</Button>
        <Button variante="secondary" onClick={onCancelar}>Cancelar</Button>
      </div>
    </form>
  );
}

function FormInsignia({ insignia, onGuardado, onCancelar }) {
  const [f, setF] = useState({ nombre: insignia?.nombre ?? "", descripcion: insignia?.descripcion ?? "", puntosRequeridos: insignia?.puntosRequeridos ?? 100 });
  const [error, setError] = useState(null);
  const [campos, setCampos] = useState({});
  const [trabajando, setTrabajando] = useState(false);
  const enviar = async (e) => {
    e.preventDefault(); setError(null); setCampos({}); setTrabajando(true);
    try {
      const cuerpo = { ...f, puntosRequeridos: Number(f.puntosRequeridos), descripcion: f.descripcion || null };
      onGuardado(insignia ? await api.admin.editarInsignia(insignia.id, cuerpo) : await api.admin.crearInsignia(cuerpo), Boolean(insignia));
    } catch (err) { setError(mensajeError(err)); setCampos(detallesError(err) ?? {}); } finally { setTrabajando(false); }
  };
  return (
    <form onSubmit={enviar} noValidate className="grid gap-4">
      {error && <Banner tono="err">{error}</Banner>}
      <TextField label="Nombre" required value={f.nombre} onChange={(e) => setF({ ...f, nombre: e.target.value })} error={campos.nombre} />
      <TextField multilinea label="Descripción" value={f.descripcion} onChange={(e) => setF({ ...f, descripcion: e.target.value })} />
      <TextField label="Puntos requeridos" type="number" min={0} required value={f.puntosRequeridos} onChange={(e) => setF({ ...f, puntosRequeridos: e.target.value })} error={campos.puntosRequeridos} />
      <div className="flex flex-wrap gap-3">
        <Button type="submit" cargando={trabajando} disabled={!f.nombre.trim()} motivo={!f.nombre.trim() ? "Escribe el nombre." : undefined}>Guardar insignia</Button>
        <Button variante="secondary" onClick={onCancelar}>Cancelar</Button>
      </div>
    </form>
  );
}

/** RF-017/RF-018: reglas de puntos e insignias. */
export default function Gamificacion() {
  const { avisar } = useNotif();
  const [pestana, setPestana] = useState("reglas");
  const reglas = useAsync(() => api.admin.reglas(), []);
  const insignias = useAsync(() => api.admin.insignias(), []);
  const [dlg, setDlg] = useState(null); // { tipo, item? }

  const guardado = (editado, que) => { avisar(`${que} ${editado ? "actualizada" : "creada"}.`, "ok"); setDlg(null); (dlg.tipo === "regla" ? reglas : insignias).recargar(); };

  return (
    <>
      <PageHeader eyebrow="Administración" titulo="Gamificación" subtitulo="Define cuántos puntos otorga cada evento y qué insignias se desbloquean."
        acciones={<Button icono={Plus} onClick={() => setDlg({ tipo: pestana === "reglas" ? "regla" : "insignia" })}>{pestana === "reglas" ? "Nueva regla" : "Nueva insignia"}</Button>} />
      <Pestanas etiqueta="Gamificación" activa={pestana} onCambiar={setPestana} pestanas={[{ id: "reglas", texto: "Reglas de puntos" }, { id: "insignias", texto: "Insignias" }]} />
      <div role="tabpanel" id={`panel-${pestana}`} aria-labelledby={`tab-${pestana}`}>
        {pestana === "reglas" && (reglas.cargando && !reglas.datos ? <Cargando /> : reglas.error && !reglas.datos ? <EstadoError mensaje={reglas.error} onReintentar={reglas.recargar} /> :
          reglas.datos.length === 0 ? <Vacio titulo="Aún no hay reglas" /> : (
            <ul className="grid gap-3">
              {reglas.datos.map((r) => (
                <Card as="li" key={r.id} className="flex list-none flex-wrap items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="font-bold text-strong">{r.nombre}</p>
                    <p className="text-sm">{textoEvento(r.evento)} → <strong>{r.puntos} puntos</strong></p>
                  </div>
                  <div className="flex items-center gap-3">
                    <Badge tono={r.activo ? "ok" : "gris"}>{r.activo ? "Activa" : "Inactiva"}</Badge>
                    <Button variante="secondary" icono={Pencil} onClick={() => setDlg({ tipo: "regla", item: r })}>Editar<span className="sr-only"> {r.nombre}</span></Button>
                  </div>
                </Card>
              ))}
            </ul>
          ))}
        {pestana === "insignias" && (insignias.cargando && !insignias.datos ? <Cargando /> : insignias.error && !insignias.datos ? <EstadoError mensaje={insignias.error} onReintentar={insignias.recargar} /> :
          insignias.datos.length === 0 ? <Vacio titulo="Aún no hay insignias" /> : (
            <ul className="grid gap-3">
              {insignias.datos.map((i) => (
                <Card as="li" key={i.id} className="flex list-none flex-wrap items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="font-bold text-strong">{i.nombre}</p>
                    {i.descripcion && <p className="text-sm">{i.descripcion}</p>}
                    <p className="text-sm">Se obtiene con <strong>{i.puntosRequeridos} puntos</strong></p>
                  </div>
                  <Button variante="secondary" icono={Pencil} onClick={() => setDlg({ tipo: "insignia", item: i })}>Editar<span className="sr-only"> {i.nombre}</span></Button>
                </Card>
              ))}
            </ul>
          ))}
      </div>
      <Dialogo abierto={Boolean(dlg)} onCerrar={() => setDlg(null)} titulo={dlg ? `${dlg.item ? "Editar" : "Nueva"} ${dlg.tipo}` : ""}>
        {dlg?.tipo === "regla" && <FormRegla regla={dlg.item} onCancelar={() => setDlg(null)} onGuardado={(_, ed) => guardado(ed, "Regla")} />}
        {dlg?.tipo === "insignia" && <FormInsignia insignia={dlg.item} onCancelar={() => setDlg(null)} onGuardado={(_, ed) => guardado(ed, "Insignia")} />}
      </Dialogo>
    </>
  );
}
