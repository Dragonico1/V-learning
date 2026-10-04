export const PREFS_BASE = {
  altoContraste: false,
  tamanoFuente: 16,
  tipografia: "Atkinson Hyperlegible Next",
  espaciadoLinea: 1.5,
  navegacionTeclado: false,
  lectorPantalla: false,
  subtitulos: false,
  transcripcion: false,
  textoAVoz: false,
  tiempoAdicional: 0,
};

const FUENTES = {
  "Atkinson Hyperlegible Next": "atkinson",
  Lexend: "lexend",
  Arial: "arial",
  Verdana: "verdana",
  Sistema: "sistema",
};


/** Aplica las preferencias al <html> de inmediato (RF-016: menos de 1 segundo). */
export function aplicarAlDocumento(cfg, categorias = []) {
  const el = document.documentElement;
  el.style.fontSize = `${Number(cfg.tamanoFuente) || 16}px`;
  el.style.setProperty("--lh", String(Number(cfg.espaciadoLinea) || 1.5));
  el.dataset.font = FUENTES[cfg.tipografia] ?? "atkinson";
  el.dataset.contrast = cfg.altoContraste ? "high" : "normal";
  el.dataset.targets = categorias.includes("MOTORA") ? "large" : "normal";
  el.dataset.keyboard = cfg.navegacionTeclado ? "on" : "off";
  el.dataset.reader = cfg.lectorPantalla ? "on" : "off";
}

