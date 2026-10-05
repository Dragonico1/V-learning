import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, ArrowRight, HelpCircle } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { Banner, Button, Cargando, EstadoError, OpcionCard, ProgressBar } from "../../components/ui/index.jsx";
import { OnboardingShell } from "../../components/layout/Shells.jsx";
import { useAsync } from "../../utils/useAsync.js";

/** RF-004: test VARK, una pregunta por pantalla, con guardado parcial. */
export default function VarkTest() {
  const nav = useNavigate();
  const { cerrarSesion, refrescar } = useAuth();
  const { datos: cuestionario, cargando, error, recargar } = useAsync(async () => {
    const [c, p] = await Promise.all([api.vark.preguntas(), api.vark.parcial()]);
    return { c, p };
  }, []);
  const [indice, setIndice] = useState(null);
  const [respuestas, setRespuestas] = useState({});
  const [ayuda, setAyuda] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [errorGuardar, setErrorGuardar] = useState(null);
  const foco = useRef(null);

  useEffect(() => {
    if (!cuestionario || indice !== null) return;
    const resp = cuestionario.p.respuestas ?? {};
    setRespuestas(resp);
    const primera = cuestionario.c.preguntas.findIndex((q) => !resp[q.numero]);
    setIndice(primera === -1 ? 0 : primera);
  }, [cuestionario, indice]);

  useEffect(() => { foco.current?.focus(); setAyuda(false); }, [indice]);

  if (cargando && !cuestionario) return <OnboardingShell titulo="Test de métodos de aprendizaje" paso={1} onSalir={cerrarSesion}><Cargando texto="Preparando el test…" /></OnboardingShell>;
  if (error && !cuestionario) return <OnboardingShell titulo="Test de métodos de aprendizaje" paso={1} onSalir={cerrarSesion}><EstadoError mensaje={error} onReintentar={recargar} /></OnboardingShell>;
  if (indice === null) return null;

  const preguntas = cuestionario.c.preguntas;
  const total = preguntas.length;
  const q = preguntas[indice];
  const elegida = respuestas[q.numero];
  const respondidas = Object.keys(respuestas).length;
  const esUltima = indice === total - 1;
  const todas = respondidas === total;

  const elegir = async (opcion) => {
    setRespuestas((r) => ({ ...r, [q.numero]: opcion }));
    setErrorGuardar(null);
    try { await api.vark.responder(q.numero, opcion); }
    catch (e) { setErrorGuardar(mensajeError(e, "No pudimos guardar esta respuesta. Inténtalo de nuevo.")); }
  };

  const finalizar = async () => {
    setGuardando(true);
    setErrorGuardar(null);
    try {
      await api.vark.enviar();
      await refrescar();
      nav("/onboarding/resultado", { replace: true });
    } catch (e) {
      setErrorGuardar(mensajeError(e));
    } finally { setGuardando(false); }
  };

  return (
    <OnboardingShell titulo="Test de métodos de aprendizaje" paso={1} onSalir={cerrarSesion}>
      <div className="mx-auto max-w-3xl">
        <p className="mb-2 text-sm font-bold text-primary-700" aria-live="polite">Pregunta {indice + 1} de {total}</p>
        <ProgressBar valor={(respondidas / total) * 100} etiqueta="Avance del test" className="mb-6" />
        <h1 ref={foco} tabIndex={-1} className="mb-4 text-2xl outline-none">{q.enunciado}</h1>
        {errorGuardar && <Banner tono="err" className="mb-4" rol="alert">{errorGuardar}</Banner>}
        <div role="radiogroup" aria-label="Opciones de respuesta" className="grid gap-3">
          {q.opciones.map((o) => (
            <OpcionCard key={o.id} seleccionada={elegida === o.id} onSelect={() => elegir(o.id)} titulo={o.texto} />
          ))}
        </div>
        <div className="mt-4">
          <Button variante="link" icono={HelpCircle} onClick={() => setAyuda((a) => !a)} aria-expanded={ayuda}>Ayuda con esta pregunta</Button>
          {ayuda && <Banner tono="info" className="mt-2" rol="status">{q.ayuda || "Elige la opción que más se parezca a lo que harías normalmente. No hay respuestas buenas ni malas."}</Banner>}
        </div>
        <div className="mt-8 flex flex-wrap items-center justify-between gap-3">
          <Button variante="secondary" icono={ArrowLeft} disabled={indice === 0} onClick={() => setIndice(indice - 1)}>Anterior</Button>
          {!esUltima ? (
            <Button icono={ArrowRight} disabled={!elegida} motivo={!elegida ? "Elige una opción para continuar." : undefined} onClick={() => setIndice(indice + 1)}>Siguiente</Button>
          ) : (
            <Button cargando={guardando} disabled={!todas} motivo={!todas ? `Te faltan ${total - respondidas} pregunta(s) por responder.` : undefined} onClick={finalizar}>Ver mi resultado</Button>
          )}
        </div>
        <p className="mt-4 text-sm">Tus respuestas se guardan solas. Puedes salir y continuar después.</p>
      </div>
    </OnboardingShell>
  );
}
