import { useState } from "react";
import { Link } from "react-router-dom";
import {
  Archive,
  BookOpen,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  ChevronUp,
  FileText,
  MessageSquare,
  Pencil,
  Plus,
  Sparkle,
} from "lucide-react";
import { api } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { fmtNumero, ESTADO_CURSO_TEXTO } from "../../utils/format.js";
import {
  Badge,
  Button,
  Cargando,
  EstadoError,
  Vacio,
} from "../../components/ui/index.jsx";

const PANEL = "rounded-card border border-line bg-surface";
const LIMITE_LISTA = 3;
/* Un solo color para todos los íconos, igual que en el resto de la plataforma. */
const TONO_ICONO = "bg-primary-100 text-primary-700";

/* Los títulos llevan "!" en el tamaño porque index.css fija h1–h4 sin capa
   y ganaría sobre las utilidades de Tailwind. */

/** Estado → [singular, plural, ícono]. Evita armar plurales con "s" ("Borradors"). */
const ESTADOS = [
  ["PUBLICADO", "Publicado", "Publicados", CheckCircle2],
  ["BORRADOR", "Borrador", "Borradores", Pencil],
  ["ARCHIVADO", "Archivado", "Archivados", Archive],
];

const ACCESOS = [
  {
    to: "/cursos",
    Icono: Plus,
    titulo: "Crear curso",
    meta: "Empieza como borrador",
  },
  {
    to: "/informes",
    Icono: FileText,
    titulo: "Informes",
    meta: "Resultados de tus estudiantes",
  },
  {
    to: "/comunidad",
    Icono: MessageSquare,
    titulo: "Comunidad",
    meta: "Preguntas y conversaciones",
  },
];

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

/** Panel del instructor: resumen de sus cursos y accesos rápidos. */
export default function Inicio() {
  const { usuario } = useAuth();
  const [verTodos, setVerTodos] = useState(false);
  const {
    datos: cursos,
    cargando,
    error,
    recargar,
  } = useAsync(() => api.instructor.cursos(), []);

  if (cargando && !cursos) return <Cargando texto="Preparando tu inicio…" />;
  if (error && !cursos)
    return <EstadoError mensaje={error} onReintentar={recargar} />;

  const nombre = (usuario?.nombre ?? "").split(" ")[0];
  const conteo = (estado) => cursos.filter((c) => c.estado === estado).length;
  /* Curso a retomar: primero un borrador, luego uno publicado. */
  const destacado =
    cursos.find((c) => c.estado === "BORRADOR") ??
    cursos.find((c) => c.estado === "PUBLICADO") ??
    null;
  const visibles = verTodos ? cursos : cursos.slice(0, LIMITE_LISTA);

  return (
    <>
      <h1 className="sr-only">Inicio</h1>
      <div className="min-w-0">
        <section
          aria-labelledby="hero"
          className="relative mb-5 flex min-h-[196px] items-center overflow-hidden rounded-card bg-[linear-gradient(120deg,var(--color-primary-700),var(--color-primary-600)_55%,#7c3aed)] px-12 py-[30px] text-white max-sm:min-h-[180px] max-sm:px-[22px] max-sm:py-[26px]"
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
              {nombre ? `Hola, ${nombre}` : "Hola"}
            </h2>
            <p className="max-w-[46ch] text-base text-primary-100!">
              Aquí ves el estado de tus cursos y puedes seguir creando contenido
              accesible.
            </p>
            <Link
              to="/cursos"
              className="mt-1.5 inline-flex min-h-[46px] items-center gap-2.5 rounded-full bg-sidebar py-1 pl-5 pr-1.5 text-base font-bold text-white hover:bg-sidebar-surface"
            >
              Crear curso
              <span className="flex size-[34px] items-center justify-center rounded-full bg-white text-sidebar">
                <Plus size={18} aria-hidden="true" />
              </span>
            </Link>
          </div>
        </section>

        <ul
          aria-label="Tus cursos por estado"
          className="mb-8 grid gap-3 sm:grid-cols-[repeat(auto-fit,minmax(190px,1fr))]"
        >
          {ESTADOS.map(([estado, singular, plural, Icono]) => {
            const n = conteo(estado);
            return (
              <li key={estado} className="flex list-none">
                <Link
                  to="/cursos"
                  className={`${PANEL} group flex flex-1 items-center gap-3 rounded-xl px-4 py-2.5 transition-shadow hover:border-primary-300 hover:shadow-md`}
                >
                  <span
                    className={`flex size-10 shrink-0 items-center justify-center rounded-lg ${TONO_ICONO}`}
                  >
                    <Icono size={20} aria-hidden="true" />
                  </span>
                  <dl className="grid min-w-0 flex-1 gap-0.5">
                    <dt className="text-sm text-muted">
                      {n === 1 ? singular : plural}
                    </dt>
                    <dd className="text-[1.375rem] leading-tight font-extrabold text-strong [font-family:var(--heading-font)]">
                      {fmtNumero(n)}
                    </dd>
                  </dl>
                  <ChevronRight
                    size={16}
                    aria-hidden="true"
                    className="shrink-0 text-muted transition-transform group-hover:translate-x-0.5 group-hover:text-primary-600"
                  />
                </Link>
              </li>
            );
          })}
        </ul>

        {destacado && (
          <section aria-labelledby="seguir" className="mb-5">
            <h2 id="seguir" className="mb-3 text-xl!">
              Continúa editando
            </h2>
            <article
              className={`${PANEL} relative flex flex-wrap gap-5 p-3.5 transition-shadow hover:shadow-md has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-primary-600`}
            >
              <div
                aria-hidden="true"
                className="flex min-h-[150px] flex-[0_1_240px] items-center justify-center rounded-opt bg-gradient-to-br from-primary-700 to-primary-300 text-white"
              >
                <BookOpen size={48} strokeWidth={1.4} />
              </div>
              <div className="flex min-w-0 flex-[1_1_260px] flex-col justify-center gap-3">
                <Badge
                  tono={destacado.estado === "PUBLICADO" ? "primario" : "gris"}
                  className="self-start"
                >
                  {ESTADO_CURSO_TEXTO[destacado.estado] ?? destacado.estado}
                </Badge>
                <h3 className="line-clamp-2 text-xl!">{destacado.titulo}</h3>
                <div>
                  <Button to={`/cursos/${destacado.id}/editar`} icono={Pencil}>
                    Seguir editando
                    <span className="sr-only">
                      {" "}
                      el curso {destacado.titulo}
                    </span>
                  </Button>
                </div>
              </div>
            </article>
          </section>
        )}

        <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
          <section
            aria-labelledby="mis"
            className="grid min-w-0 grid-cols-[minmax(0,1fr)] content-start gap-3"
          >
            <h2 id="mis" className="text-xl!">
              Mis cursos
            </h2>
            {cursos.length === 0 ? (
              <Vacio icono={BookOpen} titulo="Todavía no tienes cursos">
                <p>
                  Crea tu primer curso desde «Crear curso». Empieza como
                  borrador y lo publicas cuando esté listo.
                </p>
                <Button to="/cursos" icono={Plus}>
                  Crear curso
                </Button>
              </Vacio>
            ) : (
              <div className={`${PANEL} p-4`}>
                <ul
                  id="lista-cursos"
                  className="grid grid-cols-[minmax(0,1fr)] gap-2"
                >
                  {visibles.map((c) => (
                    <FilaLista
                      key={c.id}
                      to={`/cursos/${c.id}/editar`}
                      Icono={BookOpen}
                      titulo={c.titulo}
                      meta={ESTADO_CURSO_TEXTO[c.estado] ?? c.estado}
                    />
                  ))}
                </ul>
                <BotonVerMas
                  total={cursos.length}
                  abierto={verTodos}
                  onAlternar={() => setVerTodos((v) => !v)}
                  controla="lista-cursos"
                  className="mt-3"
                />
              </div>
            )}
          </section>

          <section
            aria-labelledby="accesos"
            className="grid min-w-0 grid-cols-[minmax(0,1fr)] content-start gap-3"
          >
            <h2 id="accesos" className="text-xl!">
              Accesos rápidos
            </h2>
            <div className={`${PANEL} p-4`}>
              <ul className="grid grid-cols-[minmax(0,1fr)] gap-2">
                {ACCESOS.map((a) => (
                  <FilaLista key={a.to} {...a} />
                ))}
              </ul>
            </div>
          </section>
        </div>
      </div>
    </>
  );
}
