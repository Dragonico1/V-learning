import { Link } from "react-router-dom";
import {
  ArrowRight,
  BookOpen,
  ChevronRight,
  FileText,
  Gamepad2,
  GraduationCap,
  Lock,
  ScrollText,
  Sparkle,
  UserPlus,
  Users,
} from "lucide-react";
import { api } from "../../api/client.js";
import { useAsync } from "../../utils/useAsync.js";
import { fmtNumero } from "../../utils/format.js";
import { Cargando, EstadoError } from "../../components/ui/index.jsx";

const PANEL = "rounded-card border border-line bg-surface";
/* Un solo color para todos los íconos, igual que en el resto de la plataforma. */
const TONO_ICONO = "bg-primary-100 text-primary-700";

/* Los títulos llevan "!" en el tamaño porque index.css fija h1–h4 sin capa
   y ganaría sobre las utilidades de Tailwind. */

const ACCESOS = [
  {
    to: "/usuarios",
    Icono: UserPlus,
    titulo: "Gestionar usuarios",
    meta: "Altas, roles y bloqueos",
  },
  {
    to: "/cursos",
    Icono: BookOpen,
    titulo: "Ver cursos",
    meta: "Publicados, borrador y archivados",
  },
  {
    to: "/gamificacion",
    Icono: Gamepad2,
    titulo: "Gamificación",
    meta: "Reglas de puntos e insignias",
  },
  {
    to: "/informes",
    Icono: FileText,
    titulo: "Informes",
    meta: "Genera y descarga reportes",
  },
  {
    to: "/auditoria",
    Icono: ScrollText,
    titulo: "Consultar auditoría",
    meta: "Registro de acciones",
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

/** Anillo con el avance promedio (0–100). */
function Anillo({ valor }) {
  const v = Math.max(0, Math.min(100, Number(valor) || 0));
  const radio = 54;
  const circunferencia = 2 * Math.PI * radio;
  return (
    <div className="relative size-[132px] shrink-0">
      <svg
        width="132"
        height="132"
        viewBox="0 0 132 132"
        role="img"
        aria-label={`Avance promedio: ${Math.round(v)} por ciento`}
      >
        <circle
          cx="66"
          cy="66"
          r={radio}
          fill="none"
          strokeWidth="12"
          className="stroke-track"
        />
        <circle
          cx="66"
          cy="66"
          r={radio}
          fill="none"
          strokeWidth="12"
          strokeLinecap="round"
          strokeDasharray={`${(circunferencia * v) / 100} ${circunferencia}`}
          transform="rotate(-90 66 66)"
          className="stroke-primary-600"
        />
      </svg>
      <span
        aria-hidden="true"
        className="absolute inset-[18px] flex items-center justify-center whitespace-nowrap text-2xl leading-none font-extrabold tabular-nums text-strong"
      >
        {Math.round(v)} %
      </span>
    </div>
  );
}

/** Barra dividida en segmentos con su leyenda. Solo usa tonos del color principal. */
function BarraApilada({ titulo, segmentos }) {
  const total = segmentos.reduce((a, s) => a + s.valor, 0);
  const resumen = segmentos
    .map((s) => `${fmtNumero(s.valor)} ${s.etiqueta}`)
    .join(", ");
  return (
    <div className="grid gap-2">
      <div className="flex items-baseline justify-between gap-3">
        <span className="text-base font-bold text-strong">{titulo}</span>
        <span className="text-sm text-muted">{fmtNumero(total)} en total</span>
      </div>
      <div
        role="img"
        aria-label={`${titulo}: ${resumen}`}
        className="flex h-3 overflow-hidden rounded-full bg-track"
      >
        {segmentos.map(
          (s) =>
            s.valor > 0 && (
              <span
                key={s.etiqueta}
                className={s.clase}
                style={{ width: `${(s.valor / total) * 100}%` }}
              />
            ),
        )}
      </div>
      <ul className="flex flex-wrap gap-x-5 gap-y-1 text-sm text-body">
        {segmentos.map((s) => (
          <li key={s.etiqueta} className="flex list-none items-center gap-1.5">
            <span
              aria-hidden="true"
              className={`size-2.5 rounded-[3px] ring-1 ring-primary-300 ${s.clase}`}
            />
            {fmtNumero(s.valor)} {s.etiqueta}
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function AdminInicio() {
  const {
    datos: d,
    cargando,
    error,
    recargar,
  } = useAsync(() => api.admin.dashboard(), []);

  if (cargando && !d) return <Cargando texto="Preparando el resumen…" />;
  if (error && !d)
    return <EstadoError mensaje={error} onReintentar={recargar} />;

  const avance = Number(d.avancePromedio ?? 0);

  const metricas = [
    [Users, "Estudiantes activos", d.estudiantesActivos, "/usuarios"],
    [GraduationCap, "Instructores activos", d.instructoresActivos, "/usuarios"],
    [BookOpen, "Cursos publicados", d.cursosPublicados, "/cursos"],
    [Lock, "Usuarios bloqueados", d.usuariosBloqueados, "/usuarios"],
  ];

  return (
    <>
      <h1 className="sr-only">Inicio</h1>
      <div className="min-w-0">
        <section
          aria-labelledby="hero"
          className="relative mb-5 flex min-h-[240px] items-center overflow-hidden rounded-card bg-[linear-gradient(120deg,var(--color-primary-700),var(--color-primary-600)_55%,#7c3aed)] px-12 py-10 text-white max-sm:min-h-[196px] max-sm:px-[22px] max-sm:py-7 xl:min-h-[256px] xl:py-11"
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
              Resumen de la plataforma
            </h2>
            <p className="max-w-[46ch] text-base text-primary-100!">
              Usuarios, cursos e inscripciones de un vistazo.
            </p>
            <Link
              to="/usuarios"
              className="mt-1.5 inline-flex min-h-[48px] items-center gap-2.5 rounded-full bg-sidebar py-1.5 pl-6 pr-2 text-base font-bold text-white hover:bg-sidebar-surface"
            >
              Gestionar usuarios
              <span className="flex size-[34px] items-center justify-center rounded-full bg-white text-sidebar">
                <ArrowRight size={18} aria-hidden="true" />
              </span>
            </Link>
          </div>
        </section>

        <ul
          aria-label="Métricas"
          className="mb-8 grid gap-3 sm:grid-cols-[repeat(auto-fit,minmax(190px,1fr))]"
        >
          {metricas.map(([Icono, etiqueta, valor, ruta]) => (
            <li key={etiqueta} className="flex list-none">
              <Link
                to={ruta}
                className={`${PANEL} group flex flex-1 items-center gap-3 rounded-xl px-4 py-3 transition-shadow hover:border-primary-300 hover:shadow-md`}
              >
                <span
                  className={`flex size-10 shrink-0 items-center justify-center rounded-lg ${TONO_ICONO}`}
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

        <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
          <section
            aria-labelledby="salud"
            className="grid min-w-0 grid-cols-[minmax(0,1fr)] content-start gap-3"
          >
            <h2 id="salud" className="text-xl!">
              Salud de la plataforma
            </h2>
            <div className={`${PANEL} flex flex-wrap items-center gap-6 p-5`}>
              <div className="flex items-center gap-4">
                <Anillo valor={avance} />
                <div className="grid max-w-[150px] gap-1">
                  <span className="text-base font-bold text-strong">
                    Avance promedio
                  </span>
                  <span className="text-sm text-muted">
                    en inscripciones activas
                  </span>
                </div>
              </div>
              <div className="grid min-w-[260px] flex-1 gap-5">
                <BarraApilada
                  titulo="Inscripciones"
                  segmentos={[
                    {
                      etiqueta: "activas",
                      valor: Number(d.inscripcionesActivas ?? 0),
                      clase: "bg-primary-600",
                    },
                    {
                      etiqueta: "finalizadas",
                      valor: Number(d.inscripcionesFinalizadas ?? 0),
                      clase: "bg-primary-300",
                    },
                  ]}
                />
                <BarraApilada
                  titulo="Cursos por estado"
                  segmentos={[
                    {
                      etiqueta: "publicados",
                      valor: Number(d.cursosPublicados ?? 0),
                      clase: "bg-primary-700",
                    },
                    {
                      etiqueta: "en borrador",
                      valor: Number(d.cursosBorrador ?? 0),
                      clase: "bg-primary-300",
                    },
                    {
                      etiqueta: "archivados",
                      valor: Number(d.cursosArchivados ?? 0),
                      clase: "bg-primary-200",
                    },
                  ]}
                />
              </div>
            </div>
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
