import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { BookOpen, Dumbbell, Headphones, Eye } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { VARK } from "../../utils/format.js";
import { Banner, Button, Cargando, EstadoError, OpcionCard } from "../../components/ui/index.jsx";
import { OnboardingShell } from "../../components/layout/Shells.jsx";

const ICONO = { VISUAL: Eye, AUDITIVO: Headphones, LECTURA_ESCRITURA: BookOpen, KINESTESICO: Dumbbell };
const PUNTOS = (r, k) => ({ VISUAL: r.visual, AUDITIVO: r.auditivo, LECTURA_ESCRITURA: r.lecturaEscritura, KINESTESICO: r.kinestesico }[k]);

/** RF-005: resultado del test y elección opcional de un segundo método. */
export default function VarkResultado() {
  const nav = useNavigate();
  const { cerrarSesion } = useAuth();
  const { datos, cargando, error, recargar } = useAsync(() => api.vark.metodos(), []);
  const [segundo, setSegundo] = useState(undefined);
  const [guardando, setGuardando] = useState(false);
  const [errorGuardar, setErrorGuardar] = useState(null);

  const envoltura = (hijo) => <OnboardingShell titulo="Tu resultado" paso={1} onSalir={cerrarSesion}>{hijo}</OnboardingShell>;
  if (cargando && !datos) return envoltura(<Cargando texto="Cargando tu resultado…" />);
  if (error && !datos) return envoltura(<EstadoError mensaje={error} onReintentar={recargar} />);
  const r = datos.ultimoResultado;
  if (!r) { nav("/onboarding/test", { replace: true }); return null; }

  const principal = r.estiloPredominante;
  const otros = Object.keys(VARK).filter((k) => k !== principal);
  const elegido = segundo === undefined ? (datos.secundario ?? null) : segundo;

  const continuar = async (conSegundo) => {
    setGuardando(true); setErrorGuardar(null);
    try {
      await api.vark.guardarMetodos(principal, conSegundo ? elegido : null);
      nav("/onboarding/accesibilidad", { replace: true });
    } catch (e) { setErrorGuardar(mensajeError(e)); } finally { setGuardando(false); }
  };

  return envoltura(
    <div className="mx-auto max-w-3xl">
      <p className="text-sm font-bold text-primary-700">Resultado del test</p>
      <h1 className="mb-2">Tu método recomendado: {VARK[principal].nombre}</h1>
      <p className="prose-vl mb-6 text-lg">{VARK[principal].frase} {VARK[principal].cambia}</p>
      <div className="mb-8 rounded-card border-2 border-primary-600 bg-primary-100 p-5">
        <p className="mb-1 font-bold text-primary-700">Recomendado para ti</p>
        <p className="text-xl font-extrabold text-strong">{VARK[principal].nombre} · {PUNTOS(r, principal)} puntos</p>
      </div>
      <h2 className="mb-1 text-xl">¿Quieres combinarlo con un segundo método? (opcional)</h2>
      <p className="mb-4">Verás primero los contenidos de tu método principal y, enseguida, los del segundo.</p>
      {errorGuardar && <Banner tono="err" className="mb-4" rol="alert">{errorGuardar}</Banner>}
      <div role="radiogroup" aria-label="Segundo método" className="grid gap-3">
        {otros.map((k) => (
          <OpcionCard key={k} icono={ICONO[k]} titulo={`${VARK[k].nombre} · ${PUNTOS(r, k)} puntos`} descripcion={VARK[k].frase}
            etiqueta={r.segundoSugerido === k ? "Sugerido" : undefined} seleccionada={elegido === k}
            onSelect={() => setSegundo(elegido === k ? null : k)} />
        ))}
      </div>
      <div className="mt-8 flex flex-wrap gap-3">
        <Button cargando={guardando} onClick={() => continuar(true)}>Continuar</Button>
        <Button variante="secondary" disabled={guardando} onClick={() => continuar(false)}>Omitir segundo método</Button>
      </div>
    </div>
  );
}
