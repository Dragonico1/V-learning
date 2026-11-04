export const TIPOGRAFIA_BASE = "Poppins + Inter";

export const PREFS_BASE = {
  altoContraste: false,
  tamanoFuente: 16,
  tipografia: TIPOGRAFIA_BASE,
  espaciadoLinea: 1.5,
  navegacionTeclado: false,
  lectorPantalla: false,
  narracionNavegacion: false,
  subtitulos: false,
  transcripcion: false,
  textoAVoz: false,
  tiempoAdicional: 0,
};

const FUENTES = {
  "Poppins + Inter": "inter",
  Inter: "inter",
  "Atkinson Hyperlegible Next": "atkinson",
  Lexend: "lexend",
  Arial: "arial",
  Verdana: "verdana",
  // Valor antiguo de la configuración por defecto: ahora mostramos Poppins + Inter.
  Sistema: "inter",
};

/** La configuración guardada con "Sistema" pasa a la tipografía base actual. */
export function normalizarTipografia(cfg) {
  if (!cfg || cfg.tipografia !== "Sistema") return cfg;
  return { ...cfg, tipografia: TIPOGRAFIA_BASE };
}

/** Aplica las preferencias al <html> de inmediato (RF-016: menos de 1 segundo). */
export function aplicarAlDocumento(cfg, categorias = []) {
  const el = document.documentElement;
  el.style.fontSize = `calc(${Number(cfg.tamanoFuente) || 16}px * var(--vl-escala, 1))`;
  el.style.setProperty("--lh", String(Number(cfg.espaciadoLinea) || 1.5));
  el.dataset.font = FUENTES[cfg.tipografia] ?? "inter";
  el.dataset.contrast = cfg.altoContraste ? "high" : "normal";
  el.dataset.targets = categorias.includes("MOTORA") ? "large" : "normal";
  el.dataset.keyboard = cfg.navegacionTeclado ? "on" : "off";
  el.dataset.reader = cfg.lectorPantalla ? "on" : "off";
  el.dataset.talkback = cfg.narracionNavegacion ? "on" : "off";
}
