import { useEffect } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { ArrowLeft, CheckCircle2, ClipboardCheck, Circle, PlayCircle } from "lucide-react";
import { api } from "../../api/client.js";
import { useAsync } from "../../utils/useAsync.js";
import { ESTADO_PROGRESO_TEXTO, FORMATO_TEXTO, TIPO_EVAL_TEXTO, VARK, fmtFecha } from "../../utils/format.js";
import { Badge, Banner, Button, Card, Cargando, EstadoError, PageHeader, ProgressBar } from "../../components/ui/index.jsx";
import TareasDelModulo from "./TareasDelModulo.jsx";

export default function CursoDetalle() {
  const { id } = useParams();
  const { datos: c, cargando, error, recargar } = useAsync(() => api.cursos.detalle(id), [id]);
  const { hash } = useLocation();
  // Llegar desde «Ir al módulo»: desplaza y enfoca el módulo indicado en la dirección (#m-ID).
  useEffect(() => {
    if (!c || !hash) return;
    const el = document.getElementById(hash.slice(1));
    if (el) { el.scrollIntoView({ block: "start" }); el.focus({ preventScroll: true }); }
  }, [c, hash]);
  if (cargando && !c) return <Cargando texto="Cargando el curso…" />;
  if (error && !c) return <EstadoError mensaje={error} onReintentar={recargar} />;

  return (
    <>
      <p className="mb-3"><Link to="/cursos" className="inline-flex min-h-11 items-center gap-1.5 font-bold text-primary-600 underline"><ArrowLeft size={16} aria-hidden="true" />Volver a mis cursos</Link></p>
      <PageHeader eyebrow={`Instructor: ${c.instructor ?? "—"}`} titulo={c.titulo} subtitulo={c.descripcion}
        acciones={<Button variante="secondary" to={`/comunidad/${c.id}`}>Comunidad</Button>} />
      <Card className="mb-5 max-w-2xl"><ProgressBar valor={Number(c.porcentaje ?? 0)} etiqueta="Avance del curso" />
        {c.metodoPrincipal && <p className="mt-2 text-sm">Contenidos ordenados para tu método: <strong>{VARK[c.metodoPrincipal]?.nombre}</strong>{c.metodoSecundario ? ` y ${VARK[c.metodoSecundario]?.nombre}` : ""}.</p>}
      </Card>
      <div className="grid gap-5">
        {c.modulos.map((m) => (
          <Card as="section" key={m.id} aria-labelledby={`m-${m.id}`}>
            <h2 id={`m-${m.id}`} tabIndex={-1} className="scroll-mt-4 text-xl outline-none">Módulo {m.orden}: {m.titulo} {m.completo && <Badge tono="ok">Completo</Badge>}</h2>
            {m.descripcion && <p className="mb-2">{m.descripcion}</p>}
            {m.sinFormatoAfin && <Banner tono="info" className="my-3">Este módulo no tiene contenidos en tu método preferido; te mostramos los formatos disponibles.</Banner>}
            <ul className="grid gap-2">
              {m.contenidos.map((ct) => (
                <li key={ct.id} className="flex flex-wrap items-center justify-between gap-3 rounded-opt border border-line p-3">
                  <div className="flex min-w-0 items-start gap-3">
                    {ct.estado === "COMPLETADO" ? <CheckCircle2 size={22} aria-hidden="true" className="mt-0.5 shrink-0 text-ok-fg" /> : ct.estado === "EN_PROGRESO" ? <PlayCircle size={22} aria-hidden="true" className="mt-0.5 shrink-0 text-primary-600" /> : <Circle size={22} aria-hidden="true" className="mt-0.5 shrink-0 text-muted" />}
                    <div className="min-w-0">
                      <Link to={`/contenidos/${ct.id}`} className="font-bold text-primary-600 underline">{ct.titulo}</Link>
                      <p className="mt-1 flex flex-wrap items-center gap-2 text-sm">
                        <Badge tono="primario">{FORMATO_TEXTO[ct.formato]}</Badge>
                        {ct.afin && <Badge tono="tutor">Recomendado para ti</Badge>}
                        <span>{ESTADO_PROGRESO_TEXTO[ct.estado] ?? "Pendiente"}</span>
                        {ct.duracionMinutos ? <span>{ct.duracionMinutos} min</span> : null}
                      </p>
                    </div>
                  </div>
                </li>
              ))}
              {m.evaluaciones.map((e) => (
                <li key={`e${e.id}`} className="flex flex-wrap items-center justify-between gap-3 rounded-opt border border-dashed border-primary-300 bg-primary-100/40 p-3">
                  <div className="flex items-center gap-3"><ClipboardCheck size={22} aria-hidden="true" className="text-primary-600" />
                    <div><p className="font-bold text-strong">{e.titulo}</p><p className="text-sm">{TIPO_EVAL_TEXTO[e.tipo]}{e.fechaLimite ? ` · Límite: ${fmtFecha(e.fechaLimite)}` : ""}{e.aviso ? ` · ${e.aviso}` : ""}</p></div></div>
                  <Button variante="secondary" to="/evaluaciones">Ver evaluaciones</Button>
                </li>
              ))}
            </ul>
            <TareasDelModulo moduloId={m.id} />
          </Card>
        ))}
      </div>
    </>
  );
}
