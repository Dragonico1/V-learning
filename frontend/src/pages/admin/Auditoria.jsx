import { useState } from "react";
import { Search } from "lucide-react";
import { api } from "../../api/client.js";
import { useAsync } from "../../utils/useAsync.js";
import { fmtFecha } from "../../utils/format.js";
import { Badge, Button, Card, Cargando, EstadoError, PageHeader, SelectField, TextField, Vacio } from "../../components/ui/index.jsx";

const RESULTADO = { PERMITIDO: ["ok", "Permitido"], DENEGADO: ["err", "Denegado"], ERROR: ["warn", "Error"] };

/** RF-014: consulta de auditoría. */
export default function Auditoria() {
  const [f, setF] = useState({ accion: "", resultado: "", desde: "", hasta: "" });
  const [aplicados, setAplicados] = useState(f);
  const [pagina, setPagina] = useState(0);
  const { datos, cargando, error, recargar } = useAsync(() => api.admin.auditoria({
    accion: aplicados.accion || undefined, resultado: aplicados.resultado || undefined,
    desde: aplicados.desde || undefined, hasta: aplicados.hasta || undefined, pagina, tamano: 25 }), [aplicados, pagina]);
  const buscar = (e) => { e.preventDefault(); setPagina(0); setAplicados(f); };
  const paginas = datos ? Math.max(1, Math.ceil(datos.total / datos.tamano)) : 1;
  const fechasMal = f.desde && f.hasta && f.desde > f.hasta;

  return (
    <>
      <PageHeader eyebrow="Administración" titulo="Auditoría" subtitulo="Registro de acciones sensibles realizadas en la plataforma." />
      <Card as="form" onSubmit={buscar} role="search" aria-label="Filtrar auditoría" className="mb-5 grid gap-4 md:grid-cols-5 md:items-end">
        <TextField label="Acción" value={f.accion} onChange={(e) => setF({ ...f, accion: e.target.value })} />
        <SelectField label="Resultado" value={f.resultado} onChange={(e) => setF({ ...f, resultado: e.target.value })}>
          <option value="">Todos</option>
          {Object.entries(RESULTADO).map(([v, [, t]]) => <option key={v} value={v}>{t}</option>)}
        </SelectField>
        <TextField label="Desde" type="date" value={f.desde} onChange={(e) => setF({ ...f, desde: e.target.value })} />
        <TextField label="Hasta" type="date" value={f.hasta} onChange={(e) => setF({ ...f, hasta: e.target.value })} error={fechasMal ? "La fecha final es anterior a la inicial." : undefined} />
        <Button type="submit" icono={Search} disabled={Boolean(fechasMal)} motivo={fechasMal ? "Corrige el rango de fechas." : undefined}>Buscar</Button>
      </Card>
      {cargando && !datos ? <Cargando texto="Cargando registros…" /> : error && !datos ? <EstadoError mensaje={error} onReintentar={recargar} /> : datos.registros.length === 0 ? (
        <Vacio titulo="No hay registros con esos filtros" />
      ) : (
        <>
          <p className="mb-2 text-sm" role="status">{datos.total} registro(s).</p>
          <div className="overflow-x-auto rounded-card border border-line bg-surface">
            <table className="w-full min-w-[760px] text-left">
              <caption className="sr-only">Registros de auditoría</caption>
              <thead className="bg-subtle text-sm"><tr>
                <th scope="col" className="p-3">Fecha</th><th scope="col" className="p-3">Usuario</th><th scope="col" className="p-3">Acción</th>
                <th scope="col" className="p-3">Recurso</th><th scope="col" className="p-3">Resultado</th><th scope="col" className="p-3">IP</th></tr></thead>
              <tbody>
                {datos.registros.map((r) => (
                  <tr key={r.id} className="border-t border-line align-top text-sm">
                    <td className="p-3">{fmtFecha(r.fecha)}</td><td className="p-3">{r.usuarioCorreo ?? "—"}</td><td className="p-3">{r.accion}</td>
                    <td className="p-3">{r.recurso ?? "—"}</td>
                    <td className="p-3"><Badge tono={RESULTADO[r.resultado]?.[0]}>{RESULTADO[r.resultado]?.[1] ?? r.resultado}</Badge></td>
                    <td className="p-3">{r.ipOrigen ?? "—"}</td>
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
    </>
  );
}
