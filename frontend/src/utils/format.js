const fechaLarga = new Intl.DateTimeFormat("es-CO", { dateStyle: "medium", timeStyle: "short" });
const fechaCorta = new Intl.DateTimeFormat("es-CO", { dateStyle: "medium" });
const hora = new Intl.DateTimeFormat("es-CO", { hour: "2-digit", minute: "2-digit" });

export const fmtFecha = (v) => (v ? fechaLarga.format(new Date(v)) : "—");
export const fmtDia = (v) => (v ? fechaCorta.format(new Date(v)) : "—");
export const fmtHora = (v) => (v ? hora.format(new Date(v)) : "");
export const fmtNumero = (n) => new Intl.NumberFormat("es-CO").format(n ?? 0);
export const fmtPct = (n) => `${Math.round(Number(n ?? 0))} %`;

/** Segundos → "1 h 05 min" / "12 min" / "30 s". */
export function fmtDuracion(segundos) {
  const s = Math.max(0, Math.round(Number(segundos ?? 0)));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  if (h > 0) return `${h} h ${String(m).padStart(2, "0")} min`;
  if (m > 0) return `${m} min`;
  return `${s} s`;
}

/** mm:ss para cronómetros. */
export function fmtCronometro(segundos) {
  const s = Math.max(0, Math.floor(segundos));
  return `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
}

export const ROL_TEXTO = { ESTUDIANTE: "Estudiante", INSTRUCTOR: "Instructor", ADMINISTRADOR: "Administrador" };
export const FORMATO_TEXTO = { VIDEO: "Video", PODCAST: "Podcast", SIMULACION: "Simulación", LECTURA: "Lectura" };
export const ESTADO_CURSO_TEXTO = { BORRADOR: "Borrador", PUBLICADO: "Publicado", ARCHIVADO: "Archivado" };
export const ESTADO_USUARIO_TEXTO = {
  PENDIENTE_PRIMER_ACCESO: "Primer acceso pendiente", ACTIVO: "Activo", BLOQUEADO: "Bloqueado", INACTIVO: "Inactivo",
};
export const TIPO_EVAL_TEXTO = { QUIZ: "Quiz", SIMULACION: "Simulación", PRACTICA: "Práctica" };
export const CLASIFICACION_TEXTO = { EXCELENTE: "Excelente", ACEPTABLE: "Aceptable", INSUFICIENTE: "Insuficiente" };
export const ESTADO_PROGRESO_TEXTO = { NO_INICIADO: "Pendiente", EN_PROGRESO: "En curso", COMPLETADO: "Completado" };

export const VARK = {
  VISUAL: { nombre: "Visual", frase: "Aprendes mejor con imágenes, esquemas y videos.", cambia: "Verás primero videos y simulaciones." },
  AUDITIVO: { nombre: "Auditivo", frase: "Aprendes mejor escuchando y conversando.", cambia: "Verás primero podcasts y explicaciones en audio." },
  LECTURA_ESCRITURA: { nombre: "Lectura y escritura", frase: "Aprendes mejor leyendo y escribiendo.", cambia: "Verás primero lecturas y textos estructurados." },
  KINESTESICO: { nombre: "Kinestésico", frase: "Aprendes mejor practicando y haciendo.", cambia: "Verás primero simulaciones y prácticas." },
};
export const CATEGORIAS = {
  VISUAL: { nombre: "Visual", detalle: "Lector de pantalla, alto contraste, texto grande y descripciones de imágenes." },
  AUDITIVA: { nombre: "Auditiva", detalle: "Subtítulos, transcripciones y alertas visuales en lugar de sonidos." },
  MOTORA: { nombre: "Motora", detalle: "Teclado completo, botones grandes, sin arrastrar y tiempo adicional." },
  COGNITIVA: { nombre: "Cognitiva", detalle: "Lenguaje simple, pasos cortos, resúmenes y glosario." },
};
