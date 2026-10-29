import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Accessibility, Award, Lock, Trophy } from "lucide-react";
import { NotificationBell } from "./NotificationBell.jsx";
import { fmtNumero } from "../../utils/format.js";
import { cx } from "../../utils/cx.js";

const RADIO = 36;
const CIRCUNFERENCIA = 2 * Math.PI * RADIO;
const MAX_NODOS = 5;

const DIAS_LETRA = ["D", "L", "M", "X", "J", "V", "S"];
const DIAS_NOMBRE = [
  "Domingo",
  "Lunes",
  "Martes",
  "Miércoles",
  "Jueves",
  "Viernes",
  "Sábado",
];

/*
 * TODO: reemplazar por datos reales del endpoint.
 * Forma esperada: [{ fecha: Date | string ISO, puntos: number }] con los últimos 7 días,
 * del más antiguo al más reciente (el último es hoy). Se recibe por la prop `actividad`.
 */
const PUNTOS_DEMO = [40, 0, 65, 30, 0, 20, 45];

function actividadDemo() {
  const hoy = new Date();
  return PUNTOS_DEMO.map((puntos, i) => {
    const fecha = new Date(hoy);
    fecha.setDate(hoy.getDate() - (PUNTOS_DEMO.length - 1 - i));
    return { fecha, puntos };
  });
}

const BOTON_REDONDO =
  "flex h-10 w-10 items-center justify-center rounded-full bg-primary-100 text-primary-700! no-underline hover:bg-primary-200";

const limitar = (n) => Math.max(0, Math.min(100, Number(n) || 0));

function progresoDe(p) {
  const meta = Number(p?.puntosRequeridos) || 0;
  const falta = Math.max(0, Number(p?.faltan) || 0);
  const pct = meta > 0 ? limitar(((meta - falta) / meta) * 100) : 0;
  return { meta, falta, pct };
}

function useMontado() {
  const [listo, setListo] = useState(false);
  useEffect(() => {
    const id = requestAnimationFrame(() => setListo(true));
    return () => cancelAnimationFrame(id);
  }, []);
  return listo;
}

function BotonIcono({ to, Icono, etiqueta }) {
  return (
    <Link
      to={to}
      aria-label={etiqueta}
      title={etiqueta}
      className={BOTON_REDONDO}
    >
      <Icono size={18} aria-hidden="true" />
    </Link>
  );
}

function AnilloPerfil({ porcentaje, inicial, listo }) {
  const v = limitar(porcentaje);
  return (
    <div className="relative mx-auto mb-3 h-28 w-28 xl:short:h-20 xl:short:w-20">
      <svg
        viewBox="0 0 80 80"
        className="h-28 w-28 -rotate-90 xl:short:h-20 xl:short:w-20"
        role="img"
        aria-label={`Avance promedio ${Math.round(v)} %`}
      >
        <circle
          cx="40"
          cy="40"
          r={RADIO}
          fill="none"
          strokeWidth="5"
          className="stroke-track"
        />
        <circle
          cx="40"
          cy="40"
          r={RADIO}
          fill="none"
          strokeWidth="5"
          strokeLinecap="round"
          className="stroke-primary-600 motion-safe:transition-[stroke-dashoffset] motion-safe:duration-1000 motion-safe:ease-out"
          strokeDasharray={CIRCUNFERENCIA}
          strokeDashoffset={CIRCUNFERENCIA * (1 - (listo ? v : 0) / 100)}
        />
      </svg>
      <span
        aria-hidden="true"
        className="absolute inset-3.5 flex items-center justify-center rounded-full bg-primary-100 text-3xl font-extrabold uppercase text-primary-700 xl:short:inset-2.5 xl:short:text-2xl"
      >
        {inicial}
      </span>
      <span
        aria-hidden="true"
        className="absolute -bottom-2 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full bg-primary-600 px-2.5 py-0.5 text-xs font-extrabold text-white ring-4 ring-surface"
      >
        {Math.round(v)}%
      </span>
    </div>
  );
}

function TituloSeccion({ id, children, derecha }) {
  return (
    <div className="mb-4 flex items-baseline justify-between gap-3">
      <h3 id={id} className="text-base">
        {children}
      </h3>
      {derecha}
    </div>
  );
}

/** Barras de los últimos 7 días: morado = día con más actividad, lila = menos, gris = sin actividad. */
function SeccionActividad({ datos, listo }) {
  const dias = useMemo(
    () =>
      datos.map((d) => {
        const fecha = new Date(d.fecha);
        return {
          puntos: Math.max(0, Number(d.puntos) || 0),
          letra: DIAS_LETRA[fecha.getDay()],
          nombre: DIAS_NOMBRE[fecha.getDay()],
        };
      }),
    [datos],
  );
  const max = Math.max(0, ...dias.map((d) => d.puntos));
  const total = dias.reduce((a, d) => a + d.puntos, 0);
  const resumen = dias.map((d) => `${d.nombre} ${d.puntos}`).join(", ");

  return (
    <section aria-labelledby="perfil-actividad" className="text-left">
      <TituloSeccion
        id="perfil-actividad"
        derecha={
          <span className="text-xs text-muted">
            Últimos 7 días ·{" "}
            <span className="font-bold text-strong">
              {fmtNumero(total)} pts
            </span>
          </span>
        }
      >
        Actividad
      </TituloSeccion>

      <div
        role="img"
        aria-label={`Puntos de los últimos 7 días: ${resumen}`}
        className="flex h-24 items-end gap-1.5 border-b border-line px-0.5 xl:short:h-16"
      >
        {dias.map((d, i) => {
          const esHoy = i === dias.length - 1;
          const alto = max > 0 ? (d.puntos / max) * 80 : 0;
          return (
            <div
              key={i}
              aria-hidden="true"
              className="flex h-full flex-1 flex-col items-center justify-end"
            >
              {esHoy && d.puntos > 0 && (
                <span className="mb-0.5 text-[11px] font-bold text-primary-700">
                  {fmtNumero(d.puntos)}
                </span>
              )}
              <div
                title={`${esHoy ? "Hoy" : d.nombre} · ${fmtNumero(d.puntos)} pts`}
                className={cx(
                  "w-[22px] rounded-t-[5px] rounded-b-[2px] motion-safe:transition-[height] motion-safe:duration-1000 motion-safe:ease-out",
                  d.puntos === 0
                    ? "bg-track"
                    : d.puntos >= max * 0.5
                      ? "bg-primary-600"
                      : "bg-primary-300",
                )}
                style={{
                  height: listo ? `${alto}%` : "0%",
                  minHeight: listo ? 3 : 0,
                }}
              />
            </div>
          );
        })}
      </div>
      <div
        aria-hidden="true"
        className="flex gap-1.5 px-0.5 pt-1 text-[11px] text-muted"
      >
        {dias.map((d, i) => {
          const esHoy = i === dias.length - 1;
          return (
            <span
              key={i}
              className={cx(
                "flex-1 text-center",
                esHoy && "font-bold text-primary-700",
              )}
            >
              {esHoy ? "Hoy" : d.letra}
            </span>
          );
        })}
      </div>
    </section>
  );
}

function SeccionInsignias({ obtenidas, pendientes, listo }) {
  const total = obtenidas.length + pendientes.length;
  if (total === 0) return null;

  const todas = [
    ...obtenidas.map((i) => ({ ...i, ganada: true })),
    ...pendientes.map((i) => ({ ...i, ganada: false })),
  ];
  const inicio = Math.max(0, Math.min(obtenidas.length - 1, total - MAX_NODOS));
  const visibles = todas.slice(inicio, inicio + MAX_NODOS);
  const n = visibles.length;
  const k = obtenidas.length - inicio;
  const siguiente = pendientes[0];
  const { falta, pct } = progresoDe(siguiente);

  let avance = 0;
  if (n > 1 && k >= 1) {
    avance = (k - 1 + (siguiente ? pct / 100 : 1)) / (n - 1);
  }
  avance = Math.max(0, Math.min(1, avance));

  return (
    <section aria-labelledby="perfil-insignias" className="text-left">
      <TituloSeccion id="perfil-insignias">Camino de insignias</TituloSeccion>

      <div
        role="img"
        aria-label={`${obtenidas.length} de ${total} insignias obtenidas${siguiente ? `. Próxima: ${siguiente.nombre}, faltan ${fmtNumero(falta)} puntos` : ""}`}
        className="relative flex justify-between px-1"
      >
        <div
          aria-hidden="true"
          className="absolute left-[18px] right-[18px] top-3 h-1 rounded-full bg-track"
        >
          <div
            className="h-full rounded-full bg-sidebar motion-safe:transition-[width] motion-safe:duration-1000 motion-safe:ease-out"
            style={{ width: `${listo ? avance * 100 : 0}%` }}
          />
        </div>
        {visibles.map((i, idx) => {
          const base =
            "relative z-10 flex h-7 w-7 items-center justify-center rounded-full";
          if (i.ganada) {
            return (
              <span
                key={i.id}
                title={i.nombre}
                aria-hidden="true"
                className={cx(base, "bg-primary-600 text-white")}
              >
                <Award size={14} />
              </span>
            );
          }
          if (idx === k) {
            return (
              <span
                key={i.id}
                title={
                  falta > 0
                    ? `${i.nombre} · faltan ${fmtNumero(falta)} pts`
                    : i.nombre
                }
                aria-hidden="true"
                className={cx(
                  base,
                  "border-2 border-primary-600 bg-surface text-primary-600",
                )}
              >
                <Award size={14} />
              </span>
            );
          }
          return (
            <span
              key={i.id}
              title={`${i.nombre} · ${fmtNumero(i.puntosRequeridos)} pts`}
              aria-hidden="true"
              className={cx(
                base,
                "border border-dashed border-primary-300 bg-surface text-primary-300",
              )}
            >
              <Lock size={12} />
            </span>
          );
        })}
      </div>
    </section>
  );
}

/**
 * Panel de perfil del estudiante.
 * - nombre: primer nombre del usuario
 * - programa: programa académico del estudiante (opcional)
 * - promedio: avance promedio (0-100) de los cursos activos
 * - logros: respuesta de GET /logros (puntos, nivel, insignias obtenidas,
 *   pendientes[{ id, nombre, puntosRequeridos, faltan }])
 * - actividad: puntos por día de los últimos 7 días [{ fecha, puntos }].
 *   Mientras no exista el endpoint, usa datos de ejemplo (PUNTOS_DEMO).
 * - conCampana: muestra la campana de notificaciones
 *   (solo cuando el panel vive en el slot lateral del layout)
 *
 * `logros` es opcional: sin datos, las insignias no se dibujan.
 * La parte superior no hace scroll (así el menú de notificaciones no se recorta);
 * solo la zona de actividad e insignias se desplaza si la pantalla es baja.
 */
export default function PanelPerfil({
  nombre,
  programa,
  promedio,
  logros,
  actividad,
  conCampana = false,
}) {
  const listo = useMontado();
  const demo = useMemo(() => actividadDemo(), []);
  const obtenidas = logros?.insignias ?? [];
  const pendientes = logros?.pendientes ?? [];

  return (
    <section
      aria-labelledby="perfil"
      className="flex h-full min-h-0 flex-col text-center"
    >
      <h2 id="perfil" className="sr-only">
        Tu perfil
      </h2>

      <div className="flex shrink-0 flex-col items-center gap-4 pt-4 xl:short:gap-3 xl:short:pt-0">
        <AnilloPerfil
          porcentaje={promedio}
          inicial={nombre.charAt(0)}
          listo={listo}
        />
        <p className="text-lg font-bold text-strong xl:short:text-base">
          Qué bueno verte, {nombre}
        </p>
        {programa && (
          <p className="-mt-2 text-sm text-muted">
            <span className="sr-only">Programa académico: </span>
            {programa}
          </p>
        )}

        <div className="flex items-center justify-center gap-2">
          {conCampana && (
            <div className="max-xl:hidden">
              <NotificationBell redonda />
            </div>
          )}
          <BotonIcono
            to="/accesibilidad"
            Icono={Accessibility}
            etiqueta="Accesibilidad"
          />
          <BotonIcono to="/logros" Icono={Trophy} etiqueta="Ranking" />
        </div>
      </div>

      <div className="mt-8 min-h-0 flex-1 space-y-12 overflow-y-auto pb-4 xl:pb-5 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden xl:short:mt-5 xl:short:space-y-8 xl:[mask-image:linear-gradient(to_bottom,black_calc(100%-20px),transparent)]">
        <SeccionActividad datos={actividad ?? demo} listo={listo} />
        <SeccionInsignias
          obtenidas={obtenidas}
          pendientes={pendientes}
          listo={listo}
        />
      </div>
    </section>
  );
}
