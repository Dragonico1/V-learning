import { useState } from "react";

import { Link } from "react-router-dom";

import {
  ArrowRight,
  BookOpen,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  ChevronUp,
  ClipboardCheck,
  FileText,
  Sparkle,
} from "lucide-react";

import { api } from "../../api/client.js";

import { useAsync } from "../../utils/useAsync.js";

import { fmtNumero } from "../../utils/format.js";

import { useTexto } from "../../utils/useTexto.js";

import { portadaCurso } from "../../utils/portada.js";

import {
  Button,
  Cargando,
  EstadoError,
  ProgressBar,
  Vacio,
} from "../../components/ui/index.jsx";

const PANEL = "rounded-card border border-line bg-surface";

const LIMITE_LISTA = 3;

/* Los títulos llevan "!" en el tamaño porque index.css fija h1–h4 sin capa

   y ganaría sobre las utilidades de Tailwind. */

/** Tema de la ilustración según las palabras del título del curso. */

function temaCurso(titulo = "") {
  const t = titulo

    .normalize("NFD")

    .replace(/[\u0300-\u036f]/g, "")

    .toLowerCase();

  if (/\bjava\b/.test(t)) return "java";

  if (/datos|sql/.test(t)) return "datos";

  if (/\bweb\b|html|css|javascript|react/.test(t)) return "web";

  if (/program|codigo|software|algoritm|python/.test(t)) return "codigo";

  return null;
}

function Ilustracion({ tema }) {
  const base = "absolute inset-0 h-full w-full";

  const props = {
    viewBox: "0 0 260 96",

    preserveAspectRatio: "xMidYMid slice",

    className: base,

    "aria-hidden": "true",
  };

  if (tema === "java") {
    return (
      <svg {...props}>
        <text
          x="46"
          y="64"
          fontSize="44"
          fontWeight="700"
          className="fill-primary-300"
        >
          {"{"}
        </text>

        <text
          x="190"
          y="64"
          fontSize="44"
          fontWeight="700"
          className="fill-primary-300"
        >
          {"}"}
        </text>

        <path
          d="M112 30q-6-8 0-14M128 30q-6-8 0-14M144 30q-6-8 0-14"
          fill="none"
          strokeWidth="4"
          strokeLinecap="round"
          className="stroke-white/70"
        />

        <path
          d="M100 38h56v22a22 22 0 0 1-22 22h-12a22 22 0 0 1-22-22z"
          className="fill-white/90"
        />

        <path
          d="M156 44h8a10 10 0 0 1 0 20h-10"
          fill="none"
          strokeWidth="5"
          className="stroke-white/80"
        />

        <ellipse cx="128" cy="86" rx="40" ry="5" className="fill-white/30" />
      </svg>
    );
  }

  if (tema === "codigo") {
    return (
      <svg {...props}>
        <rect
          x="64"
          y="12"
          width="124"
          height="72"
          rx="8"
          className="fill-white/15"
        />

        <circle cx="76" cy="24" r="3" className="fill-white/60" />

        <circle cx="86" cy="24" r="3" className="fill-white/60" />

        <circle cx="96" cy="24" r="3" className="fill-white/60" />

        <rect
          x="76"
          y="38"
          width="48"
          height="5"
          rx="2.5"
          className="fill-primary-300"
        />

        <rect
          x="86"
          y="50"
          width="70"
          height="5"
          rx="2.5"
          className="fill-white/70"
        />

        <rect
          x="86"
          y="61"
          width="42"
          height="5"
          rx="2.5"
          className="fill-white/70"
        />

        <rect
          x="76"
          y="72"
          width="30"
          height="5"
          rx="2.5"
          className="fill-primary-300"
        />

        <text
          x="196"
          y="58"
          fontSize="30"
          fontWeight="700"
          className="fill-white/40"
        >
          &lt;/&gt;
        </text>
      </svg>
    );
  }

  if (tema === "datos") {
    return (
      <svg {...props}>
        <path
          d="M94 24v50c0 8 15 14 34 14s34-6 34-14V24"
          className="fill-white/20"
        />

        <path
          d="M94 42c0 8 15 14 34 14s34-6 34-14M94 58c0 8 15 14 34 14s34-6 34-14"
          fill="none"
          strokeWidth="3"
          className="stroke-white/60"
        />

        <ellipse cx="128" cy="24" rx="34" ry="10" className="fill-white/85" />
      </svg>
    );
  }

  if (tema === "web") {
    return (
      <svg {...props}>
        <rect
          x="64"
          y="12"
          width="132"
          height="72"
          rx="8"
          className="fill-white/15"
        />

        <rect
          x="64"
          y="12"
          width="132"
          height="16"
          rx="8"
          className="fill-white/25"
        />

        <circle cx="76" cy="20" r="3" className="fill-white/70" />

        <circle cx="86" cy="20" r="3" className="fill-white/70" />

        <rect
          x="102"
          y="17"
          width="76"
          height="6"
          rx="3"
          className="fill-white/40"
        />

        <rect
          x="76"
          y="38"
          width="48"
          height="34"
          rx="4"
          className="fill-primary-300"
        />

        <rect
          x="132"
          y="38"
          width="52"
          height="8"
          rx="4"
          className="fill-white/70"
        />

        <rect
          x="132"
          y="52"
          width="40"
          height="6"
          rx="3"
          className="fill-white/50"
        />

        <rect
          x="132"
          y="64"
          width="46"
          height="6"
          rx="3"
          className="fill-white/50"
        />
      </svg>
    );
  }

  return null;
}

/** Imagen de la tarjeta de curso, arriba y con el avance encima. */

function ImagenCurso({ curso, etiqueta }) {
  const [fallo, setFallo] = useState(false);

  const tema = temaCurso(curso.titulo);

  const src = portadaCurso(curso);

  return (
    <div className="relative h-28 w-full overflow-hidden bg-gradient-to-br from-primary-700 to-primary-300 text-white sm:h-32">
      {src && !fallo ? (
        <img
          src={src}
          alt=""
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={() => setFallo(true)}
          className="absolute inset-0 h-full w-full object-cover"
        />
      ) : tema ? (
        <Ilustracion tema={tema} />
      ) : (
        <span className="absolute inset-0 flex items-center justify-center">
          <BookOpen size={28} strokeWidth={1.6} aria-hidden="true" />
        </span>
      )}

      {etiqueta && (
        <span className="absolute left-2 top-2 z-10 rounded-full bg-white/95 px-2 py-0.5 text-[11px] font-extrabold uppercase tracking-wide text-primary-700">
          {etiqueta}
        </span>
      )}
    </div>
  );
}

function BotonVerMas({ total, abierto, onAlternar, controla, className = "" }) {
  if (total <= LIMITE_LISTA) return null;

  const Icono = abierto ? ChevronUp : ChevronDown;

  return (
    <div className={`text-center ${className}`}>
      <button
        type="button"
        onClick={onAlternar}
        aria-expanded={abierto}
        aria-controls={controla}
        className="inline-flex min-h-9 items-center gap-1.5 rounded-full bg-primary-100 px-4 text-sm font-bold text-primary-700 hover:bg-primary-200"
      >
        {abierto ? "Ver menos" : `Ver más (${total - LIMITE_LISTA})`}

        <Icono size={16} aria-hidden="true" />
      </button>
    </div>
  );
}

function enlacePendiente(p) {
  if (p.tipo === "EVALUACION") return "/evaluaciones";

  if (p.tipo === "TAREA") return `/cursos/${p.cursoId}#m-${p.moduloId}`;

  return `/contenidos/${p.id}`;
}

function textoPendiente(p, t) {
  if (p.tipo === "EVALUACION") return t("Evaluación", "Prueba");

  if (p.tipo === "TAREA") return t("Tarea");

  return t("Contenido", "Lección");
}

const ICONO_PENDIENTE = { EVALUACION: ClipboardCheck, TAREA: FileText };

/** Fila de lista: ícono, título, línea de detalle y flecha. */

function FilaLista({ to, Icono, titulo, meta }) {
  return (
    <li className="list-none">
      <Link
        to={to}
        className="flex items-center gap-3 rounded-opt bg-subtle p-3 hover:bg-primary-100"
      >
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-100 text-primary-700">
          <Icono size={18} aria-hidden="true" />
        </span>

        <span className="flex min-w-0 flex-1 flex-col gap-0.5">
          <span className="block truncate text-base font-bold text-strong">
            {titulo}
          </span>

          <span className="block truncate text-sm text-muted">{meta}</span>
        </span>

        <ChevronRight
          size={16}
          aria-hidden="true"
          className="shrink-0 text-muted"
        />
      </Link>
    </li>
  );
}

/** Sección con título, filas y "Ver más". */

function SeccionLista({
  id,

  titulo,

  total,

  vacio,

  listaId,

  abierto,

  onAlternar,

  children,
}) {
  return (
    <section
      aria-labelledby={id}
      className="grid min-w-0 grid-cols-[minmax(0,1fr)] content-start gap-3"
    >
      <h2 id={id} className="text-xl!">
        {titulo}
      </h2>

      <div className={`${PANEL} p-4`}>
        {total === 0 ? (
          <p>{vacio}</p>
        ) : (
          <ul id={listaId} className="grid grid-cols-[minmax(0,1fr)] gap-2">
            {children}
          </ul>
        )}

        <BotonVerMas
          total={total}
          abierto={abierto}
          onAlternar={onAlternar}
          controla={listaId}
          className="mt-3"
        />
      </div>
    </section>
  );
}

export default function EstudianteInicio() {
  const [verTodosPend, setVerTodosPend] = useState(false);

  const [verTodosRef, setVerTodosRef] = useState(false);

  const t = useTexto();

  const {
    datos: d,

    cargando,

    error,

    recargar,
  } = useAsync(() => api.progreso.dashboard(), []);

  if (cargando && !d) return <Cargando texto="Preparando tu inicio…" />;

  if (error && !d)
    return <EstadoError mensaje={error} onReintentar={recargar} />;

  const enCurso = d.cursos.filter((c) => c.estado === "ACTIVA");

  const pendientesVisibles = verTodosPend
    ? d.pendientes
    : d.pendientes.slice(0, LIMITE_LISTA);

  const sugerenciasVisibles = verTodosRef
    ? d.sugerencias
    : d.sugerencias.slice(0, LIMITE_LISTA);

  const pendientes = (
    <SeccionLista
      id="pend"
      titulo={t("Pendientes", "Por hacer")}
      total={d.pendientes.length}
      vacio={t(
        "No tienes pendientes. ¡Buen trabajo!",

        "No tienes nada por hacer. ¡Muy bien!",
      )}
      listaId="lista-pend"
      abierto={verTodosPend}
      onAlternar={() => setVerTodosPend((v) => !v)}
    >
      {pendientesVisibles.map((p) => (
        <FilaLista
          key={`${p.tipo}-${p.id}`}
          to={enlacePendiente(p)}
          Icono={ICONO_PENDIENTE[p.tipo] ?? BookOpen}
          titulo={p.titulo}
          meta={`${textoPendiente(p, t)} · ${p.curso}`}
        />
      ))}
    </SeccionLista>
  );

  const reforzar = d.sugerencias.length > 0 && (
    <SeccionLista
      id="reforzar"
      titulo={t("Para reforzar", "Para repasar")}
      total={d.sugerencias.length}
      listaId="lista-ref"
      abierto={verTodosRef}
      onAlternar={() => setVerTodosRef((v) => !v)}
    >
      {sugerenciasVisibles.map((s) => (
        <FilaLista
          key={s.contenidoId}
          to={`/contenidos/${s.contenidoId}`}
          Icono={BookOpen}
          titulo={s.titulo}
          meta={`${s.motivo} · ${s.curso}`}
        />
      ))}
    </SeccionLista>
  );

  const principal = (
    <div className="min-w-0">
      <section
        aria-labelledby="hero"
        className="relative mb-5 flex min-h-[212px] items-center overflow-hidden rounded-card bg-[linear-gradient(120deg,var(--color-primary-700),var(--color-primary-600)_55%,#7c3aed)] px-12 py-8 text-white max-sm:min-h-[174px] max-sm:px-[22px] max-sm:py-6 xl:min-h-[224px] xl:py-9"
      >
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0"
        >
          <span className="absolute -right-20 -top-[120px] size-[300px] rounded-full bg-white/[.07]" />

          <span className="absolute -bottom-[130px] right-[170px] size-[230px] rounded-full bg-white/5" />

          <Sparkle
            size={150}
            strokeWidth={1}
            className="absolute right-14 top-1/2 -translate-y-1/2 text-white/[.16] max-sm:-right-8 max-sm:opacity-60"
          />

          <Sparkle
            size={48}
            strokeWidth={1.2}
            className="absolute right-[230px] top-[26px] text-white/[.12] max-sm:hidden"
          />
        </div>

        <div className="relative flex max-w-[640px] flex-col items-start gap-2.5">
          <h2
            id="hero"
            className="text-[clamp(1.375rem,2.1vw,1.75rem)]! font-extrabold! leading-[1.2]! text-white!"
          >
            {t(
              "Afina tus habilidades con cursos profesionales",

              "Aprende a tu ritmo",
            )}
          </h2>

          <p className="max-w-[46ch] text-base text-primary-100!">
            {t(
              "Retoma tus cursos o empieza uno nuevo, a tu ritmo.",

              "Sigue tus cursos o empieza uno nuevo.",
            )}
          </p>

          <Link
            to="/cursos"
            className="mt-1.5 inline-flex min-h-[48px] items-center gap-2.5 rounded-full bg-sidebar py-1.5 pl-6 pr-2 text-base font-bold text-white hover:bg-sidebar-surface"
          >
            {t("Explorar cursos", "Ver cursos")}

            <span className="flex size-[34px] items-center justify-center rounded-full bg-white text-sidebar">
              <ArrowRight size={18} aria-hidden="true" />
            </span>
          </Link>
        </div>
      </section>

      <ul
        aria-label="Tu progreso"
        className="mb-8 grid gap-3 sm:grid-cols-[repeat(auto-fit,minmax(190px,1fr))]"
      >
        {[
          [
            CheckCircle2,

            t("Contenidos", "Lecciones"),

            d.totales.contenidosCompletados,

            "/cursos",

            "bg-primary-100 text-primary-700",
          ],

          [
            FileText,

            t("Tareas"),

            d.totales.tareasCalificadas,

            "/progreso",

            "bg-primary-100 text-primary-700",
          ],

          [
            ClipboardCheck,

            t("Evaluaciones", "Pruebas"),

            d.totales.evaluacionesRealizadas,

            "/evaluaciones",

            "bg-primary-100 text-primary-700",
          ],
        ].map(([Icono, etiqueta, valor, ruta, tono]) => (
          <li key={etiqueta} className="flex list-none">
            <Link
              to={ruta}
              className={`${PANEL} group flex flex-1 items-center gap-3 rounded-xl px-4 py-3 transition-shadow hover:border-primary-300 hover:shadow-md`}
            >
              <span
                className={`flex size-10 shrink-0 items-center justify-center rounded-lg ${tono}`}
              >
                <Icono size={20} aria-hidden="true" />
              </span>

              <dl className="grid min-w-0 flex-1 gap-0.5">
                <dt className="text-sm text-muted">{etiqueta}</dt>

                <dd className="text-[1.375rem] leading-tight font-extrabold text-strong [font-family:var(--heading-font)]">
                  {fmtNumero(valor ?? 0)}
                </dd>
              </dl>

              <ChevronRight
                size={16}
                aria-hidden="true"
                className="shrink-0 text-muted transition-transform group-hover:translate-x-0.5 group-hover:text-primary-600"
              />
            </Link>
          </li>
        ))}
      </ul>

      <section aria-labelledby="seguir" className="mb-5">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 id="seguir" className="text-xl!">
            {t("Continúa aprendiendo", "Sigue aprendiendo")}
          </h2>
        </div>

        {enCurso.length === 0 ? (
          <Vacio
            titulo={t(
              "No estás inscrito en ningún curso",

              "Todavía no tienes cursos",
            )}
          >
            <Button to="/cursos" icono={ArrowRight}>
              {t("Explorar cursos", "Ver cursos")}
            </Button>
          </Vacio>
        ) : (
          <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {enCurso.map((c) => (
              <li
                key={c.cursoId}
                className={`${PANEL} group relative flex flex-col overflow-hidden transition duration-200 hover:-translate-y-0.5 hover:border-primary-300 hover:shadow-lg has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-primary-600`}
              >
                <ImagenCurso curso={c} etiqueta={`${c.modulosCompletos} de ${c.modulosTotales} ${t("módulos", "partes")}`} />

                <div className="flex min-w-0 flex-1 flex-col gap-1 p-2.5 sm:p-3">
                  <h3 className="line-clamp-2 text-base! leading-snug!">
                    <Link
                      to={`/cursos/${c.cursoId}`}
                      className="text-inherit no-underline after:absolute after:inset-0 hover:underline"
                    >
                      {c.titulo}
                    </Link>
                  </h3>

                  <div className="mt-auto pt-1">
                    <p className="mb-1 text-xs font-semibold text-muted">
                      {t("Tu avance", "Tu progreso")}
                    </p>

                    <ProgressBar
                      valor={Number(c.porcentaje)}
                      etiqueta={`${t("Avance en", "Progreso en")} ${c.titulo}`}
                    />
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <div className="grid gap-5 sm:grid-cols-[repeat(auto-fit,minmax(min(340px,100%),1fr))] xl:gap-6">
        {reforzar}

        {pendientes}
      </div>
    </div>
  );

  return (
    <>
      <h1 className="sr-only">Inicio</h1>

      {principal}
    </>
  );
}
