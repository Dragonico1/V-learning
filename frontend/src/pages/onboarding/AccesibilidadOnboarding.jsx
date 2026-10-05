import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Brain, Ear, Eye, Hand, Check } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { usePrefs } from "../../context/PrefsContext.jsx";
import { CATEGORIAS } from "../../utils/format.js";
import { Banner, Button, OpcionCard } from "../../components/ui/index.jsx";
import { OnboardingShell } from "../../components/layout/Shells.jsx";

const ICONO = { VISUAL: Eye, AUDITIVA: Ear, MOTORA: Hand, COGNITIVA: Brain };

function resumenConfig(c) {
  const l = [];
  if (!c) return l;
  l.push(`Texto de ${c.tamanoFuente} px, interlineado ${Number(c.espaciadoLinea).toFixed(1)}`);
  if (c.altoContraste) l.push("Alto contraste");
  if (c.lectorPantalla) l.push("Optimizado para lector de pantalla");
  if (c.navegacionTeclado) l.push("Navegación completa con teclado");
  if (c.subtitulos) l.push("Subtítulos activados");
  if (c.transcripcion) l.push("Transcripciones activadas");
  if (c.textoAVoz) l.push("Lectura en voz alta disponible");
  if (c.tiempoAdicional > 0) l.push(`${c.tiempoAdicional} % de tiempo adicional en evaluaciones`);
  return l;
}

/** RF-014: perfil de accesibilidad (paso 2 del onboarding). */
export default function AccesibilidadOnboarding() {
  const nav = useNavigate();
  const { cerrarSesion, refrescar } = useAuth();
  const { reemplazar } = usePrefs();
  const [sel, setSel] = useState(new Set());
  const [ninguna, setNinguna] = useState(false);
  const [prev, setPrev] = useState(null);
  const [error, setError] = useState(null);
  const [trabajando, setTrabajando] = useState(false);

  const previsualizar = async (cats) => {
    try { setPrev(await api.accesibilidad.previsualizar([...cats])); } catch { setPrev(null); }
  };
  const alternar = (k) => {
    const s = new Set(sel);
    s.has(k) ? s.delete(k) : s.add(k);
    setNinguna(false); setSel(s); previsualizar(s);
  };
  const elegirNinguna = () => { setNinguna(true); setSel(new Set()); previsualizar(new Set()); };

  const guardar = async (manual) => {
    setTrabajando(true); setError(null);
    try {
      const p = await api.accesibilidad.guardarPerfil([...sel]);
      reemplazar(p.configuracion, p.categorias);
      await refrescar();
      nav(manual ? "/accesibilidad" : "/inicio", { replace: true });
    } catch (e) { setError(mensajeError(e)); } finally { setTrabajando(false); }
  };
  const listo = ninguna || sel.size > 0;
  const lineas = resumenConfig(prev?.configuracion);

  return (
    <OnboardingShell titulo="Tu perfil de accesibilidad" paso={2} onSalir={cerrarSesion}>
      <div className="mx-auto grid max-w-5xl gap-8 lg:grid-cols-[1.2fr_1fr]">
        <div>
          <h1 className="mb-2">¿Qué apoyos te ayudarían?</h1>
          <p className="prose-vl mb-6 text-lg">Puedes elegir varios. Ajustaremos la plataforma y podrás cambiarlo cuando quieras.</p>
          {error && <Banner tono="err" className="mb-4" rol="alert">{error}</Banner>}
          <div role="group" aria-label="Categorías de accesibilidad" className="grid gap-3">
            {Object.entries(CATEGORIAS).map(([k, v]) => (
              <OpcionCard key={k} multiple icono={ICONO[k]} titulo={v.nombre} descripcion={v.detalle} seleccionada={sel.has(k)} onSelect={() => alternar(k)} />
            ))}
            <OpcionCard multiple icono={Check} titulo="Ninguna" descripcion="No necesito ajustes especiales por ahora." seleccionada={ninguna} onSelect={elegirNinguna} />
          </div>
          <div className="mt-8 flex flex-wrap gap-3">
            <Button cargando={trabajando} disabled={!listo} motivo={!listo ? "Elige al menos una opción o «Ninguna»." : undefined} onClick={() => guardar(false)}>Guardar y empezar</Button>
            <Button variante="secondary" disabled={!listo || trabajando} onClick={() => guardar(true)}>Guardar y ajustar manualmente</Button>
          </div>
        </div>
        <aside aria-labelledby="prev" className="h-fit rounded-card border border-line bg-surface p-5">
          <h2 id="prev" className="mb-2 text-lg">Vista previa</h2>
          {!prev ? <p>Elige una categoría para ver cómo cambiará la plataforma.</p> : (
            <ul className="grid gap-2" aria-live="polite">
              {lineas.map((t) => <li key={t} className="flex gap-2"><Check size={18} aria-hidden="true" className="mt-1 shrink-0 text-ok-fg" />{t}</li>)}
            </ul>
          )}
        </aside>
      </div>
    </OnboardingShell>
  );
}
