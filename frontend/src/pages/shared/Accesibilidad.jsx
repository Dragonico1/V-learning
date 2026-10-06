import { useState } from "react";
import { Brain, Ear, Eye, Hand, RotateCcw } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { usePrefs } from "../../context/PrefsContext.jsx";
import { useNotif } from "../../context/NotifContext.jsx";
import { CATEGORIAS } from "../../utils/format.js";
import { Banner, Button, Card, OpcionCard, PageHeader, SelectField, Toggle } from "../../components/ui/index.jsx";
import ConfirmarDialogo from "../../components/ConfirmarDialogo.jsx";
import VolverACursos from "../../components/VolverACursos.jsx";

const ICONO = { VISUAL: Eye, AUDITIVA: Ear, MOTORA: Hand, COGNITIVA: Brain };
const FUENTES = ["Atkinson Hyperlegible Next", "Lexend", "Arial", "Verdana", "Sistema"];
const hhmm = (d) => d.toLocaleTimeString("es-CO", { hour: "2-digit", minute: "2-digit" });

/** RF-014 / RF-016: ajustes de accesibilidad con aplicación inmediata. */
export default function Accesibilidad() {
  const { usuario } = useAuth();
  const { config, categorias, actualizar, reemplazar, restablecer, guardadoEn, guardando, error, advertencias, sugerencia, setSugerencia, persistenteEnServidor } = usePrefs();
  const { avisar } = useNotif();
  const esEstudiante = usuario.rol === "ESTUDIANTE";
  const [confirmar, setConfirmar] = useState(false);
  const [errorCat, setErrorCat] = useState(null);

  const alternarCategoria = async (k) => {
    const s = new Set(categorias);
    s.has(k) ? s.delete(k) : s.add(k);
    setErrorCat(null);
    try {
      const p = await api.accesibilidad.guardarPerfil([...s]);
      reemplazar(p.configuracion, p.categorias);
      avisar("Actualizamos tu perfil de accesibilidad.", "ok");
    } catch (e) { setErrorCat(mensajeError(e)); }
  };
  const aplicarSugerencia = () => { actualizar(sugerencia); setSugerencia(null); };

  return (
    <>
      <VolverACursos />
      <PageHeader eyebrow="Accesibilidad" titulo="Ajusta la plataforma a ti" subtitulo="Los cambios se aplican de inmediato y se guardan solos." />
      {!persistenteEnServidor && <Banner tono="info" className="mb-5">Como {usuario.rol === "INSTRUCTOR" ? "instructor" : "administrador"}, tus ajustes se guardan solo en este dispositivo.</Banner>}
      <p className="mb-4 min-h-6 font-semibold" role="status">
        {error ? <span className="text-err-fg">{error}</span> : guardando ? "Guardando…" : guardadoEn ? `Cambios guardados automáticamente a las ${hhmm(guardadoEn)}` : ""}
      </p>
      {advertencias.length > 0 && (
        <Banner tono="warn" className="mb-5" rol="status">
          <ul className="list-disc pl-5">{advertencias.map((a) => <li key={a}>{a}</li>)}</ul>
          {sugerencia && <Button className="mt-3" variante="secondary" onClick={aplicarSugerencia}>Aplicar la combinación sugerida</Button>}
        </Banner>
      )}
      <div className="grid gap-6 xl:grid-cols-2">
        {esEstudiante && (
          <Card as="section" aria-labelledby="cat">
            <h2 id="cat" className="mb-3 text-xl">Tu perfil</h2>
            {errorCat && <Banner tono="err" className="mb-3" rol="alert">{errorCat}</Banner>}
            <div role="group" aria-labelledby="cat" className="grid gap-3">
              {Object.entries(CATEGORIAS).map(([k, v]) => (
                <OpcionCard key={k} multiple icono={ICONO[k]} titulo={v.nombre} descripcion={v.detalle} seleccionada={categorias.includes(k)} onSelect={() => alternarCategoria(k)} />
              ))}
            </div>
          </Card>
        )}
        <Card as="section" aria-labelledby="aj">
          <h2 id="aj" className="mb-2 text-xl">Ajustes manuales</h2>
          <div className="grid gap-4">
            <div>
              <label htmlFor="tam" className="mb-1.5 block font-bold text-strong">Tamaño del texto: {config.tamanoFuente} px</label>
              <input id="tam" type="range" min={12} max={32} step={1} value={config.tamanoFuente} onChange={(e) => actualizar({ tamanoFuente: Number(e.target.value) })} className="h-8 w-full accent-primary-600" />
            </div>
            <div>
              <label htmlFor="lh" className="mb-1.5 block font-bold text-strong">Interlineado: {Number(config.espaciadoLinea).toFixed(1)}</label>
              <input id="lh" type="range" min={1} max={3} step={0.1} value={config.espaciadoLinea} onChange={(e) => actualizar({ espaciadoLinea: Number(Number(e.target.value).toFixed(1)) })} className="h-8 w-full accent-primary-600" />
            </div>
            <SelectField label="Tipografía" value={config.tipografia} onChange={(e) => actualizar({ tipografia: e.target.value })}>
              {FUENTES.map((f) => <option key={f}>{f}</option>)}
            </SelectField>
            {esEstudiante && (
              <div>
                <label htmlFor="ta" className="mb-1.5 block font-bold text-strong">Tiempo adicional en evaluaciones: {config.tiempoAdicional} %</label>
                <input id="ta" type="range" min={0} max={200} step={10} value={config.tiempoAdicional} onChange={(e) => actualizar({ tiempoAdicional: Number(e.target.value) })} className="h-8 w-full accent-primary-600" />
              </div>
            )}
            <div className="divide-y divide-line">
              <Toggle label="Alto contraste" descripcion="Colores con más contraste." checked={config.altoContraste} onChange={(v) => actualizar({ altoContraste: v })} />
              <Toggle label="Navegación con teclado" descripcion="Muestra siempre el foco y los atajos." checked={config.navegacionTeclado} onChange={(v) => actualizar({ navegacionTeclado: v })} />
              <Toggle label="Optimizar para lector de pantalla" descripcion="Prioriza contenidos con texto alternativo." checked={config.lectorPantalla} onChange={(v) => actualizar({ lectorPantalla: v })} />
              {esEstudiante && <>
                <Toggle label="Subtítulos" checked={config.subtitulos} onChange={(v) => actualizar({ subtitulos: v })} />
                <Toggle label="Transcripciones" checked={config.transcripcion} onChange={(v) => actualizar({ transcripcion: v })} />
                <Toggle label="Lectura en voz alta" descripcion="Botón «Leer en voz alta» en las lecciones." checked={config.textoAVoz} onChange={(v) => actualizar({ textoAVoz: v })} />
              </>}
            </div>
            <div><Button variante="secondary" icono={RotateCcw} onClick={() => setConfirmar(true)}>Restablecer valores predeterminados</Button></div>
          </div>
        </Card>
      </div>
      <Card className="mt-6">
        <h2 className="mb-2 text-xl">Vista previa del texto</h2>
        <p className="lesson-text">Así se verán las lecciones con tus ajustes. Un buen contraste, un tamaño cómodo y un interlineado amplio ayudan a leer con menos esfuerzo.</p>
      </Card>
      <ConfirmarDialogo abierto={confirmar} onCerrar={() => setConfirmar(false)} titulo="¿Restablecer los ajustes?" textoConfirmar="Sí, restablecer"
        onConfirmar={async () => { await restablecer(); avisar("Restablecimos los valores predeterminados.", "ok"); }}>
        <p>Tus ajustes volverán a los valores originales. Tu perfil de categorías se conserva.</p>
      </ConfirmarDialogo>
    </>
  );
}
