import { forwardRef, useEffect, useId, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, CheckCircle2, CircleAlert, Eye, EyeOff, Info, Loader2, X } from "lucide-react";

import { cx } from "../../utils/cx.js";

/* ------------------------------ Botones ------------------------------ */
const VARIANTES = {
  primary: "bg-primary-600 text-white hover:bg-primary-700 border-transparent",
  secondary: "bg-surface text-primary-600 border-primary-600 hover:bg-primary-100",
  tutor: "bg-tutor-700 text-white hover:bg-[#08463c] border-transparent",
  link: "bg-transparent text-primary-600 underline border-transparent px-1 min-h-0 hover:text-primary-700",
  ghost: "bg-transparent text-strong border-line hover:bg-subtle",
};

/** Botón. Si está deshabilitado y se indica `motivo`, el texto explica por qué (nunca solo color). */
export const Button = forwardRef(function Button(
  { variante = "primary", cargando = false, disabled = false, motivo, icono: Icono, bloque = false, to, className, children, type = "button", ...resto },
  ref,
) {
  const idMotivo = useId();
  const inactivo = disabled || cargando;
  const base = cx(
    "vl-btn inline-flex items-center justify-center gap-2 rounded-ctl border-[1.5px] px-5 font-bold transition-colors",
    variante === "link" ? "" : "min-h-12",
    inactivo ? "cursor-not-allowed border-transparent bg-[#E2E8F0] text-[#475569] hover:bg-[#E2E8F0]" : VARIANTES[variante],
    bloque && "w-full",
    className,
  );
  const contenido = (
    <>
      {cargando ? <Loader2 size={18} aria-hidden="true" className="vl-spin" /> : Icono && <Icono size={18} aria-hidden="true" />}
      <span>{children}</span>
    </>
  );
  const aviso = inactivo && motivo ? <span id={idMotivo} className="mt-1 block text-sm text-muted">{motivo}</span> : null;
  if (to && !inactivo) {
    return <Link ref={ref} to={to} className={base} {...resto}>{contenido}</Link>;
  }
  return (
    <span className={bloque ? "block" : "inline-block"}>
      <button ref={ref} type={type} disabled={inactivo} aria-busy={cargando || undefined}
        aria-describedby={aviso ? idMotivo : undefined} className={base} {...resto}>{contenido}</button>
      {aviso}
    </span>
  );
});

/* ------------------------------ Formularios ------------------------------ */
export const TextField = forwardRef(function TextField(
  { label, error, ayuda, id, className, type = "text", multilinea = false, ...resto }, ref,
) {
  const auto = useId();
  const fid = id ?? auto;
  const Tag = multilinea ? "textarea" : "input";
  return (
    <div className={className}>
      <label htmlFor={fid} className="mb-1.5 block font-bold text-strong">{label}</label>
      <Tag ref={ref} id={fid} type={multilinea ? undefined : type} aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${fid}-e` : ayuda ? `${fid}-a` : undefined}
        className={cx("vl-input w-full rounded-ctl border bg-surface px-3.5 text-strong placeholder:text-muted",
          multilinea ? "min-h-28 py-3" : "min-h-12",
          error ? "border-2 border-err-fg" : "border-line")} {...resto} />
      {ayuda && !error && <p id={`${fid}-a`} className="mt-1 text-sm text-muted">{ayuda}</p>}
      {error && (
        <p id={`${fid}-e`} role="alert" className="mt-1 flex items-start gap-1.5 text-sm font-semibold text-err-fg">
          <CircleAlert size={16} aria-hidden="true" className="mt-0.5 shrink-0" />{error}
        </p>
      )}
    </div>
  );
});

export const PasswordField = forwardRef(function PasswordField({ label, error, ayuda, id, className, ...resto }, ref) {
  const auto = useId();
  const fid = id ?? auto;
  const [visible, setVisible] = useState(false);
  return (
    <div className={className}>
      <label htmlFor={fid} className="mb-1.5 block font-bold text-strong">{label}</label>
      <div className="relative">
        <input ref={ref} id={fid} type={visible ? "text" : "password"} aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${fid}-e` : ayuda ? `${fid}-a` : undefined}
          className={cx("vl-input min-h-12 w-full rounded-ctl border bg-surface pl-3.5 pr-14 text-strong",
            error ? "border-2 border-err-fg" : "border-line")} {...resto} />
        <button type="button" onClick={() => setVisible((v) => !v)} aria-pressed={visible}
          aria-label={visible ? "Ocultar contraseña" : "Mostrar contraseña"}
          className="absolute right-1 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-lg text-body hover:bg-subtle">
          {visible ? <EyeOff size={20} aria-hidden="true" /> : <Eye size={20} aria-hidden="true" />}
        </button>
      </div>
      {ayuda && !error && <p id={`${fid}-a`} className="mt-1 text-sm text-muted">{ayuda}</p>}
      {error && (
        <p id={`${fid}-e`} role="alert" className="mt-1 flex items-start gap-1.5 text-sm font-semibold text-err-fg">
          <CircleAlert size={16} aria-hidden="true" className="mt-0.5 shrink-0" />{error}
        </p>
      )}
    </div>
  );
});

export function SelectField({ label, id, error, className, children, ...resto }) {
  const auto = useId();
  const fid = id ?? auto;
  return (
    <div className={className}>
      <label htmlFor={fid} className="mb-1.5 block font-bold text-strong">{label}</label>
      <select id={fid} aria-invalid={error ? true : undefined}
        className={cx("vl-input min-h-12 w-full rounded-ctl border bg-surface px-3 text-strong", error ? "border-2 border-err-fg" : "border-line")} {...resto}>
        {children}
      </select>
      {error && <p role="alert" className="mt-1 text-sm font-semibold text-err-fg">{error}</p>}
    </div>
  );
}

export function Checkbox({ checked, onChange, children, id, ...resto }) {
  const auto = useId();
  const fid = id ?? auto;
  return (
    <label htmlFor={fid} className="flex min-h-11 cursor-pointer items-center gap-3 font-semibold text-strong">
      <input id={fid} type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)}
        className="h-5 w-5 shrink-0 accent-primary-600" {...resto} />
      <span>{children}</span>
    </label>
  );
}

/** Interruptor con la palabra «Activado» / «Desactivado» siempre visible. */
export function Toggle({ checked, onChange, label, descripcion, disabled }) {
  const id = useId();
  return (
    <div className="flex items-center justify-between gap-4 py-3">
      <div className="min-w-0">
        <p id={`${id}-l`} className="font-bold text-strong">{label}</p>
        {descripcion && <p id={`${id}-d`} className="text-sm text-muted">{descripcion}</p>}
      </div>
      <div className="flex shrink-0 items-center gap-3">
        <span aria-hidden="true" className="w-24 text-right text-sm font-bold text-strong">{checked ? "Activado" : "Desactivado"}</span>
        <button type="button" role="switch" aria-checked={checked} aria-labelledby={`${id}-l`}
          aria-describedby={descripcion ? `${id}-d` : undefined} disabled={disabled} onClick={() => onChange(!checked)}
          className={cx("relative h-8 w-14 shrink-0 rounded-full border-2 transition-colors",
            checked ? "border-primary-600 bg-primary-600" : "border-[#64748B] bg-[#94A3B8]", disabled && "opacity-50")}>
          <span className={cx("absolute top-0.5 h-6 w-6 rounded-full bg-white transition-all", checked ? "left-[1.625rem]" : "left-0.5")} />
        </button>
      </div>
    </div>
  );
}

/** Control segmentado (radiogrupo) con flechas del teclado. */
export function Segmented({ label, opciones, valor, onChange, nombre }) {
  const id = useId();
  const refs = useRef([]);
  const idx = Math.max(0, opciones.findIndex((o) => o.valor === valor));
  const mover = (e, i) => {
    let n = null;
    if (e.key === "ArrowRight" || e.key === "ArrowDown") n = (i + 1) % opciones.length;
    if (e.key === "ArrowLeft" || e.key === "ArrowUp") n = (i - 1 + opciones.length) % opciones.length;
    if (n !== null) { e.preventDefault(); onChange(opciones[n].valor); refs.current[n]?.focus(); }
  };
  return (
    <div role="radiogroup" aria-labelledby={`${id}-l`} data-nombre={nombre}>
      <p id={`${id}-l`} className="mb-1.5 font-bold text-strong">{label}</p>
      <div className="inline-flex flex-wrap gap-1 rounded-xl bg-subtle p-1">
        {opciones.map((o, i) => {
          const activo = o.valor === valor;
          return (
            <button key={String(o.valor)} ref={(el) => { refs.current[i] = el; }} type="button" role="radio" aria-checked={activo}
              tabIndex={i === idx ? 0 : -1} onClick={() => onChange(o.valor)} onKeyDown={(e) => mover(e, i)}
              className={cx("flex min-h-11 items-center gap-1.5 rounded-lg border-2 px-4 font-bold",
                activo ? "border-primary-600 bg-surface text-primary-700" : "border-transparent text-body hover:bg-surface")}>
              {activo && <CheckCircle2 size={16} aria-hidden="true" />}{o.texto}
            </button>
          );
        })}
      </div>
    </div>
  );
}

/** Tarjeta seleccionable (VARK, categorías). `multiple` usa casilla; si no, radio. */
export function OpcionCard({ seleccionada, onSelect, icono: Icono, titulo, descripcion, etiqueta, multiple = false, disabled = false }) {
  return (
    <button type="button" role={multiple ? "checkbox" : "radio"} aria-checked={seleccionada} onClick={onSelect} disabled={disabled}
      className={cx("vl-opt flex w-full items-start gap-4 rounded-opt border p-4 text-left transition-colors",
        seleccionada ? "border-2 border-primary-600 bg-primary-100" : "border-line bg-surface hover:border-primary-600",
        disabled && "opacity-60")}>
      {Icono && (
        <span className={cx("flex h-11 w-11 shrink-0 items-center justify-center rounded-lg",
          seleccionada ? "bg-primary-600 text-white" : "bg-subtle text-body")}>
          <Icono size={24} aria-hidden="true" />
        </span>
      )}
      <span className="min-w-0 flex-1">
        <span className="flex flex-wrap items-center gap-2">
          <span className="font-bold text-strong">{titulo}</span>
          {etiqueta && <Badge tono="primario">{etiqueta}</Badge>}
        </span>
        {descripcion && <span className="mt-0.5 block text-sm text-body">{descripcion}</span>}
      </span>
      {seleccionada && (
        <span className="flex shrink-0 items-center gap-1 text-sm font-bold text-primary-700">
          <CheckCircle2 size={16} aria-hidden="true" />Seleccionada
        </span>
      )}
    </button>
  );
}

/* ------------------------------ Insignias y estados ------------------------------ */
const TONOS = {
  primario: "bg-primary-100 text-primary-700 border-primary-300",
  ok: "bg-ok-bg text-ok-fg border-transparent",
  warn: "bg-warn-bg text-warn-fg border-transparent",
  err: "bg-err-bg text-err-fg border-transparent",
  gris: "bg-subtle text-[#334155] border-transparent",
  tutor: "bg-tutor-100 text-tutor-700 border-tutor-700",
};

export function Badge({ tono = "gris", icono: Icono, children, className }) {
  return (
    <span className={cx("inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-bold uppercase tracking-[0.04em]", TONOS[tono], className)}>
      {Icono && <Icono size={13} aria-hidden="true" />}{children}
    </span>
  );
}

export function ProgressBar({ valor, etiqueta, className }) {
  const v = Math.max(0, Math.min(100, Number(valor ?? 0)));
  return (
    <div className={cx("flex items-center gap-3", className)}>
      <div role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(v)} aria-label={etiqueta ?? "Progreso"}
        className="h-2 flex-1 overflow-hidden rounded-full bg-track">
        <div className="h-full rounded-full bg-primary-600 transition-all" style={{ width: `${v}%` }} />
      </div>
      <span className="w-12 text-right text-sm font-bold tabular-nums text-strong">{Math.round(v)} %</span>
    </div>
  );
}

/* ------------------------------ Contenedores ------------------------------ */
export function Card({ children, className, as: Tag = "div", ...resto }) {
  return <Tag className={cx("vl-card rounded-card border border-line bg-surface p-6", className)} {...resto}>{children}</Tag>;
}

const BANNERS = {
  info: { c: "bg-subtle text-[#334155] border-line", i: Info },
  ok: { c: "bg-tutor-100 text-ok-fg border-ok-fg", i: CheckCircle2 },
  warn: { c: "bg-warn-bg text-warn-fg border-warn-fg", i: AlertTriangle },
  err: { c: "bg-err-bg text-err-fg border-err-fg", i: CircleAlert },
};
export function Banner({ tono = "info", children, className, rol }) {
  const { c, i: Icono } = BANNERS[tono];
  return (
    <div role={rol ?? (tono === "err" ? "alert" : undefined)} className={cx("flex items-start gap-3 rounded-opt border p-3 text-sm font-semibold", c, className)}>
      <Icono size={20} aria-hidden="true" className="mt-0.5 shrink-0" />
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  );
}

export function Cargando({ texto = "Cargando…" }) {
  return (
    <div role="status" className="flex items-center justify-center gap-3 p-10 text-body">
      <Loader2 size={22} aria-hidden="true" className="vl-spin" /><span>{texto}</span>
    </div>
  );
}

export function EstadoError({ mensaje, onReintentar }) {
  return (
    <Banner tono="err" className="my-4">
      <p>{mensaje}</p>
      {onReintentar && <button type="button" onClick={onReintentar} className="mt-2 font-bold underline">Intentar de nuevo</button>}
    </Banner>
  );
}

export function Vacio({ icono: Icono, titulo, children }) {
  return (
    <div className="rounded-card border border-dashed border-line bg-surface p-8 text-center">
      {Icono && <Icono size={32} aria-hidden="true" className="mx-auto mb-3 text-muted" />}
      <h3>{titulo}</h3>
      {children && <div className="prose-vl mx-auto mt-1 text-body">{children}</div>}
    </div>
  );
}

export function PageHeader({ eyebrow, titulo, subtitulo, acciones }) {
  return (
    <header className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        {eyebrow && <Badge tono="primario" className="mb-2">{eyebrow}</Badge>}
        <h1>{titulo}</h1>
        {subtitulo && <p className="prose-vl mt-1.5 text-lg">{subtitulo}</p>}
      </div>
      {acciones && <div className="flex flex-wrap items-center gap-3">{acciones}</div>}
    </header>
  );
}

/* ------------------------------ Diálogo y pestañas ------------------------------ */
/** Diálogo modal nativo: atrapa el foco y se cierra con Esc. */
export function Dialogo({ abierto, onCerrar, titulo, children, ancho = "max-w-lg" }) {
  const ref = useRef(null);
  const id = useId();
  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    if (abierto && !d.open) d.showModal();
    if (!abierto && d.open) d.close();
  }, [abierto]);
  return (
    <dialog ref={ref} aria-labelledby={id} onClose={onCerrar} onCancel={onCerrar}
      className={cx("m-auto w-[calc(100vw-2rem)] rounded-screen border border-line bg-surface p-0 backdrop:bg-black/50", ancho)}>
      {abierto && (
        <div className="p-6">
          <div className="mb-4 flex items-start justify-between gap-4">
            <h2 id={id}>{titulo}</h2>
            <button type="button" onClick={onCerrar} aria-label="Cerrar" className="flex h-11 w-11 items-center justify-center rounded-lg hover:bg-subtle">
              <X size={22} aria-hidden="true" />
            </button>
          </div>
          {children}
        </div>
      )}
    </dialog>
  );
}

export function Pestanas({ etiqueta, pestanas, activa, onCambiar }) {
  const refs = useRef([]);
  const mover = (e, i) => {
    let n = null;
    if (e.key === "ArrowRight") n = (i + 1) % pestanas.length;
    if (e.key === "ArrowLeft") n = (i - 1 + pestanas.length) % pestanas.length;
    if (n !== null) { e.preventDefault(); onCambiar(pestanas[n].id); refs.current[n]?.focus(); }
  };
  return (
    <div role="tablist" aria-label={etiqueta} className="mb-5 flex flex-wrap gap-1 border-b border-line">
      {pestanas.map((p, i) => {
        const a = p.id === activa;
        return (
          <button key={p.id} ref={(el) => { refs.current[i] = el; }} role="tab" id={`tab-${p.id}`} aria-selected={a} aria-controls={`panel-${p.id}`}
            tabIndex={a ? 0 : -1} type="button" onClick={() => onCambiar(p.id)} onKeyDown={(e) => mover(e, i)}
            className={cx("-mb-px min-h-11 border-b-[3px] px-4 font-bold", a ? "border-primary-600 text-primary-700" : "border-transparent text-body hover:text-strong")}>
            {p.texto}
          </button>
        );
      })}
    </div>
  );
}
