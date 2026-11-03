import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Accessibility, Trophy } from "lucide-react";
import { NotificationBell } from "./NotificationBell.jsx";
import { fmtNumero } from "../../utils/format.js";
import { cx } from "../../utils/cx.js";

const RADIO = 36;
const CIRCUNFERENCIA = 2 * Math.PI * RADIO;

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
    <div className="relative mx-auto mb-3 h-28 w-28">
      <svg
        viewBox="0 0 80 80"
        className="h-28 w-28 -rotate-90"
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
        className="absolute inset-3.5 flex items-center justify-center rounded-full bg-primary-100 text-3xl font-extrabold uppercase text-primary-700"
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
    <div className="mb-3 flex items-baseline justify-between gap-3">
      <h3 id={id} className="text-base!">
        {children}
      </h3>

      {derecha}
    </div>
  );
}

/**
 * Barras de los últimos 7 días en una caja lavanda con eje Y.
 * Morado = día con más actividad, lila = menos, gris = sin actividad.
 */
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
  const techo = Math.max(20, Math.ceil(max / 20) * 20);
  const total = dias.reduce((a, d) => a + d.puntos, 0);
  const resumen = dias.map((d) => `${d.nombre} ${d.puntos}`).join(", ");

  return (
    <section aria-labelledby="perfil-actividad" className="text-left">
      <TituloSeccion
        id="perfil-actividad"
        derecha={
          <span className="text-sm text-muted">
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
        className="grid grid-cols-[auto_1fr] grid-rows-[88px_auto] gap-x-2 rounded-[14px] bg-primary-100/60 px-3.5 pb-2.5 pt-3.5"
      >
        <div
          aria-hidden="true"
          className="flex flex-col justify-between text-right text-xs leading-none text-muted"
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
            const esHoy = i === dias.length - 1;
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
                    "w-[22px] max-w-full rounded-t-[5px] rounded-b-[2px] motion-safe:transition-[height] motion-safe:duration-1000 motion-safe:ease-out",
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
          className="flex gap-1.5 pt-1 text-xs text-muted"
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
      </div>
    </section>
  );
}

export default function PanelPerfil({
  nombre,
  promedio,
  actividad,
  conCampana = false,
}) {
  const listo = useMontado();
  const demo = useMemo(() => actividadDemo(), []);

  return (
    <section
      aria-labelledby="perfil"
      className="flex h-full min-h-0 flex-col text-center"
    >
      <h2 id="perfil" className="sr-only">
        Tu perfil
      </h2>

      <div className="flex shrink-0 flex-col items-center gap-4 pt-4">
        <AnilloPerfil
          porcentaje={promedio}
          inicial={nombre.charAt(0)}
          listo={listo}
        />

        <div className="grid gap-0.5">
          <p className="text-lg font-bold text-strong">
            Qué bueno verte, {nombre}
          </p>

          <p className="text-sm text-muted">
            Sigue aprendiendo para alcanzar tu meta
          </p>
        </div>

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

      <div className="mt-6 min-h-0 flex-1 overflow-y-auto pb-6 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden xl:[mask-image:linear-gradient(to_bottom,black_calc(100%-24px),transparent)]">
        <SeccionActividad datos={actividad ?? demo} listo={listo} />
      </div>
    </section>
  );
}
