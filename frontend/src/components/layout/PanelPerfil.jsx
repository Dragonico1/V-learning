import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Accessibility, Trophy, X } from "lucide-react";
import { NotificationBell } from "./NotificationBell.jsx";
import { fmtNumero } from "../../utils/format.js";
import { cx } from "../../utils/cx.js";

const RADIO = 36;
const CIRCUNFERENCIA = 2 * Math.PI * RADIO;

const DIAS_LETRA = ["L", "M", "M", "J", "V", "S", "D"];
const DIAS_NOMBRE = [
  "Lunes",
  "Martes",
  "Miércoles",
  "Jueves",
  "Viernes",
  "Sábado",
  "Domingo",
];

const PUNTOS_DEMO = [40, 0, 65, 30, 0, 20, 45];

/** Actividad de la semana actual, de lunes a domingo (los días futuros quedan en 0). */
function actividadDemo() {
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);

  const lunes = new Date(hoy);
  lunes.setDate(hoy.getDate() - ((hoy.getDay() + 6) % 7));

  return PUNTOS_DEMO.map((puntos, i) => {
    const fecha = new Date(lunes);
    fecha.setDate(lunes.getDate() + i);

    return { fecha, puntos: fecha > hoy ? 0 : puntos };
  });
}

const BOTON_REDONDO =
  "flex h-10 w-10 items-center justify-center rounded-full bg-primary-100 text-primary-700! no-underline hover:bg-primary-200 2xl:h-11 2xl:w-11";

const limitar = (n) => Math.max(0, Math.min(100, Number(n) || 0));

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
      <Icono size={18} aria-hidden="true" className="2xl:size-5" />
    </Link>
  );
}

function AnilloPerfil({ porcentaje, inicial, listo }) {
  const v = limitar(porcentaje);

  return (
    <div className="relative mx-auto mb-3 h-28 w-28 2xl:h-32 2xl:w-32">
      <svg
        viewBox="0 0 80 80"
        className="h-28 w-28 -rotate-90 2xl:h-32 2xl:w-32"
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
        className="absolute inset-3.5 flex items-center justify-center rounded-full bg-primary-100 text-3xl font-extrabold uppercase text-primary-700 2xl:inset-4 2xl:text-4xl"
      >
        {inicial}
      </span>

      <span
        aria-hidden="true"
        className="absolute -bottom-2 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full bg-primary-600 px-2.5 py-0.5 text-xs font-extrabold text-white ring-4 ring-surface 2xl:text-sm"
      >
        {Math.round(v)}%
      </span>
    </div>
  );
}

function TituloSeccion({ id, children, derecha }) {
  return (
    <div className="mb-3 flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
      <h3 id={id} className="text-base!">
        {children}
      </h3>

      {derecha && (
        <>
          <span aria-hidden="true" className="text-muted">
            ·
          </span>
          {derecha}
        </>
      )}
    </div>
  );
}

/**
 * Barras de la semana actual (lunes a domingo) en una caja lavanda con eje Y.
 * Morado = día con más actividad, lila = menos, gris = sin actividad.
 */
function SeccionActividad({ datos, listo }) {
  const dias = useMemo(() => {
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);

    return datos.map((d) => {
      const fecha = new Date(d.fecha);
      fecha.setHours(0, 0, 0, 0);
      const indice = (fecha.getDay() + 6) % 7;

      return {
        puntos: Math.max(0, Number(d.puntos) || 0),
        letra: DIAS_LETRA[indice],
        nombre: DIAS_NOMBRE[indice],
        esHoy: fecha.getTime() === hoy.getTime(),
      };
    });
  }, [datos]);

  const max = Math.max(0, ...dias.map((d) => d.puntos));
  const techo = Math.max(20, Math.ceil(max / 20) * 20);
  const total = dias.reduce((a, d) => a + d.puntos, 0);
  const resumen = dias.map((d) => `${d.nombre} ${d.puntos}`).join(", ");

  return (
    <section aria-labelledby="perfil-actividad" className="text-left">
      <TituloSeccion
        id="perfil-actividad"
        derecha={
          <span className="text-sm text-muted">
            <span className="font-bold text-strong">
              {fmtNumero(total)} pts
            </span>
          </span>
        }
      >
        Actividad semanal
      </TituloSeccion>

      <div
        role="img"
        aria-label={`Puntos de esta semana: ${resumen}`}
        className="grid grid-cols-[auto_1fr] grid-rows-[88px_auto] gap-x-2 rounded-[14px] bg-primary-100/60 px-3.5 pb-2.5 pt-3.5 2xl:grid-rows-[104px_auto] 2xl:px-4"
      >
        <div
          aria-hidden="true"
          className="flex flex-col justify-between text-right text-xs leading-none text-muted 2xl:text-sm"
        >
          <span>{fmtNumero(techo)}</span>
          <span>{fmtNumero(techo / 2)}</span>
          <span>0</span>
        </div>

        <div
          aria-hidden="true"
          className="flex items-end gap-1.5 border-b border-primary-200"
        >
          {dias.map((d, i) => {
            const esHoy = d.esHoy;
            const alto = (d.puntos / techo) * 100;

            return (
              <div
                key={i}
                className="flex h-full flex-1 flex-col items-center justify-end"
              >
                {esHoy && d.puntos > 0 && (
                  <span className="mb-0.5 text-xs leading-none font-bold text-primary-700">
                    {fmtNumero(d.puntos)}
                  </span>
                )}

                <div
                  title={`${esHoy ? "Hoy" : d.nombre} · ${fmtNumero(d.puntos)} pts`}
                  className={cx(
                    "w-[22px] max-w-full rounded-t-[5px] rounded-b-[2px] motion-safe:transition-[height] motion-safe:duration-1000 motion-safe:ease-out 2xl:w-[26px]",
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

        <div aria-hidden="true" />

        <div
          aria-hidden="true"
          className="flex gap-1.5 pt-1 text-xs text-muted 2xl:text-sm"
        >
          {dias.map((d, i) => {
            return (
              <span
                key={i}
                className={cx(
                  "flex-1 text-center",
                  d.esHoy && "font-bold text-primary-700",
                )}
              >
                {d.letra}
              </span>
            );
          })}
        </div>
      </div>
    </section>
  );
}

/**
 * Panel de perfil del estudiante.
 * - nombre: primer nombre del usuario
 * - programa: programa académico del estudiante (opcional)
 * - promedio: avance promedio (0-100) de los cursos activos
 * - actividad: puntos por día de los últimos 7 días [{ fecha, puntos }].
 * - onCerrar: cierra el panel cuando vive en el slot lateral del layout.
 *
 * La parte superior no hace scroll (así el menú de notificaciones no se recorta);
 * solo la zona de actividad se desplaza si la pantalla es baja.
 */
export default function PanelPerfil({
  nombre,
  programa,
  promedio,
  actividad,
  onCerrar,
}) {
  const listo = useMontado();
  const demo = useMemo(() => actividadDemo(), []);

  return (
    <section
      aria-labelledby="perfil"
      className="relative flex h-full min-h-0 flex-col text-center"
    >
      <h2 id="perfil" className="sr-only">
        Tu perfil
      </h2>

      {onCerrar && (
        <button
          type="button"
          onClick={onCerrar}
          aria-label="Cerrar panel de perfil"
          title="Cerrar panel de perfil"
          className="absolute right-0 top-1 hidden h-9 w-9 items-center justify-center rounded-full text-muted hover:bg-subtle hover:text-strong xl:flex"
        >
          <X size={18} aria-hidden="true" />
        </button>
      )}

      <div className="flex shrink-0 flex-col items-center gap-4 pt-4 2xl:gap-5">
        <AnilloPerfil
          porcentaje={promedio}
          inicial={nombre.charAt(0)}
          listo={listo}
        />

        <div className="grid gap-0.5">
          <p className="text-lg font-bold text-strong 2xl:text-xl">
            Qué bueno verte, {nombre}
          </p>

          {programa && (
            <p className="text-sm text-muted 2xl:text-base">
              <span className="sr-only">Programa académico: </span>
              {programa}
            </p>
          )}

          <p className="text-sm text-muted 2xl:text-base">
            Sigue aprendiendo para alcanzar tu meta
          </p>
        </div>

        <div className="flex items-center justify-center gap-2">
          <div className="max-xl:hidden">
            <NotificationBell redonda />
          </div>

          <BotonIcono
            to="/accesibilidad"
            Icono={Accessibility}
            etiqueta="Accesibilidad"
          />

          <BotonIcono to="/logros" Icono={Trophy} etiqueta="Ranking" />
        </div>
      </div>

      <div className="mt-6 min-h-0 flex-1 overflow-y-auto pb-6 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden xl:[mask-image:linear-gradient(to_bottom,black_calc(100%-24px),transparent)]">
        <SeccionActividad datos={actividad ?? demo} listo={listo} />
      </div>
    </section>
  );
}
