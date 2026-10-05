import { useState } from "react";
import { Download, Eye } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { fmtFecha } from "../../utils/format.js";
import { Banner, Button, Card, Cargando, PageHeader, SelectField, TextField, Vacio } from "../../components/ui/index.jsx";

/** Barras horizontales accesibles: cada barra con su valor visible; la tabla es el equivalente textual. */
function Barras({ filas, columnas }) {
  const idx = columnas.findIndex((c) => /%|promedio|avance/i.test(c));
  if (idx < 1) return null;
  const datos = filas.map((f) => ({ n: f[0], v: parseFloat(String(f[idx]).replace(",", ".")) })).filter((d) => Number.isFinite(d.v)).slice(0, 12);
  if (datos.length < 2) return null;
  const max = Math.max(100, ...datos.map((d) => d.v));
  return (
    <figure className="mb-4" aria-label={`Gráfico de ${columnas[idx]}`}>
      <figcaption className="mb-2 font-bold text-strong">{columnas[idx]} por {columnas[0].toLowerCase()}</figcaption>
      <svg role="img" aria-label={`Barras de ${columnas[idx]}. Los mismos datos están en la tabla.`} width="100%" height={datos.length * 28} className="block">
        {datos.map((d, i) => (
          <g key={d.n + i} transform={`translate(0 ${i * 28})`}>
            <rect x="0" y="4" width={`${(d.v / max) * 70}%`} height="18" rx="3" fill="#5B21B6" />
            <text x={`${(d.v / max) * 70 + 1}%`} y="18" fontSize="13" fill="currentColor">{d.v}</text>
          </g>
        ))}
      </svg>
    </figure>
  );
}

/** RF-012: informes para instructor y administrador. */
export default function Informes() {
  const { usuario } = useAuth();
  const { avisar } = useNotif();
  const cursos = useAsync(() => (usuario.rol === "ADMINISTRADOR" ? api.admin.cursos() : api.instructor.cursos()), [usuario.rol]);
  const historial = useAsync(() => api.informes.historial(), []);
  const [f, setF] = useState({ cursoId: "", estudianteId: "", desde: "", hasta: "" });
  const [vista, setVista] = useState(null);
  const [error, setError] = useState(null);
  const [trabajando, setTrabajando] = useState(null);

  const filtros = (formato) => ({
    formato, cursoId: f.cursoId ? Number(f.cursoId) : null, estudianteId: f.estudianteId ? Number(f.estudianteId) : null,
    desde: f.desde || null, hasta: f.hasta || null });
  const fechasMal = f.desde && f.hasta && f.desde > f.hasta;

  const ver = async () => {
    setError(null); setTrabajando("vista");
    try { setVista(await api.informes.vista(filtros("PDF"))); } catch (e) { setError(mensajeError(e)); setVista(null); } finally { setTrabajando(null); }
  };
  const bajar = async (formato) => {
    setError(null); setTrabajando(formato);
    try { await api.informes.generar(filtros(formato)); avisar("Informe generado.", "ok"); historial.recargar(); }
    catch (e) { setError(mensajeError(e)); } finally { setTrabajando(null); }
  };

  return (
    <>
      <PageHeader eyebrow="Informes" titulo="Informes de avance" subtitulo="Consulta el desempeño de los estudiantes y descárgalo en PDF o Excel." />
      <Card as="form" onSubmit={(e) => { e.preventDefault(); ver(); }} aria-label="Filtros del informe" className="mb-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4 xl:items-end" noValidate>
        <SelectField label="Curso" value={f.cursoId} onChange={(e) => setF({ ...f, cursoId: e.target.value })}>
          <option value="">Todos mis cursos</option>
          {(cursos.datos ?? []).map((c) => <option key={c.id} value={c.id}>{c.titulo}</option>)}
        </SelectField>
        {usuario.rol === "ADMINISTRADOR" && <TextField label="ID de estudiante (opcional)" type="number" min={1} value={f.estudianteId} onChange={(e) => setF({ ...f, estudianteId: e.target.value })} />}
        <TextField label="Desde" type="date" value={f.desde} onChange={(e) => setF({ ...f, desde: e.target.value })} />
        <TextField label="Hasta" type="date" value={f.hasta} onChange={(e) => setF({ ...f, hasta: e.target.value })} error={fechasMal ? "La fecha final es anterior a la inicial." : undefined} />
        <div className="flex flex-wrap gap-3 md:col-span-2 xl:col-span-4">
          <Button type="submit" icono={Eye} cargando={trabajando === "vista"} disabled={Boolean(fechasMal)}>Vista previa</Button>
          <Button variante="secondary" icono={Download} cargando={trabajando === "PDF"} disabled={Boolean(fechasMal)} onClick={() => bajar("PDF")}>Descargar PDF</Button>
          <Button variante="secondary" icono={Download} cargando={trabajando === "EXCEL"} disabled={Boolean(fechasMal)} onClick={() => bajar("EXCEL")}>Descargar Excel</Button>
        </div>
      </Card>
      {error && <Banner tono="err" className="mb-4" rol="alert">{error}</Banner>}
      {trabajando === "vista" && <Cargando texto="Generando la vista previa…" />}
      {vista && (vista.sinDatos ? (
        <Banner tono="warn" rol="status">{vista.mensaje || "No hay datos para los filtros elegidos."}</Banner>
      ) : (
        <Card as="section" aria-labelledby="iv" className="mb-6">
          <h2 id="iv" className="mb-1 text-xl">{vista.titulo}</h2>
          {vista.contexto?.length > 0 && <ul className="mb-3 text-sm">{vista.contexto.map((c) => <li key={c}>{c}</li>)}</ul>}
          <Barras filas={vista.filas} columnas={vista.columnas} />
          <div className="overflow-x-auto">
            <table className="w-full min-w-[560px] text-left">
              <caption className="sr-only">{vista.titulo}</caption>
              <thead className="bg-subtle text-sm"><tr>{vista.columnas.map((c) => <th key={c} scope="col" className="p-3">{c}</th>)}</tr></thead>
              <tbody>{vista.filas.map((fila, i) => (
                <tr key={i} className="border-t border-line">{fila.map((c, j) => (j === 0 ? <th key={j} scope="row" className="p-3 font-normal">{c}</th> : <td key={j} className="p-3">{c}</td>))}</tr>
              ))}</tbody>
            </table>
          </div>
          {vista.notas?.length > 0 && <ul className="mt-3 list-disc pl-5 text-sm">{vista.notas.map((n) => <li key={n}>{n}</li>)}</ul>}
        </Card>
      ))}
      <section aria-labelledby="ih">
        <h2 id="ih" className="mb-3 text-xl">Informes generados</h2>
        {historial.cargando && !historial.datos ? <Cargando /> : (historial.datos ?? []).length === 0 ? <Vacio titulo="Aún no has generado informes" /> : (
          <ul className="grid gap-2">
            {historial.datos.map((h) => (
              <li key={h.id} className="rounded-opt border border-line bg-surface p-3 text-sm"><strong>{h.formato === "PDF" ? "PDF" : "Excel"}</strong> · {fmtFecha(h.fechaGeneracion)}{h.filtros ? ` · ${h.filtros}` : ""}</li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
