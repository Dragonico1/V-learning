/** Sonido corto de aviso (sin archivos de audio: se genera con Web Audio). El aviso visual siempre se muestra. */
const CLAVE = "vl.sonido";

export function sonidoActivado() {
  try { return localStorage.getItem(CLAVE) !== "off"; } catch { return true; }
}

export function fijarSonido(activado) {
  try { localStorage.setItem(CLAVE, activado ? "on" : "off"); } catch { /* sin almacenamiento */ }
}

let contexto = null;

export function reproducirAviso() {
  try {
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return;
    contexto = contexto ?? new AC();
    if (contexto.state === "suspended") contexto.resume();
    const t0 = contexto.currentTime;
    [[880, 0], [1175, 0.14]].forEach(([frecuencia, inicio]) => {
      const osc = contexto.createOscillator();
      const gain = contexto.createGain();
      osc.type = "sine";
      osc.frequency.value = frecuencia;
      gain.gain.setValueAtTime(0.0001, t0 + inicio);
      gain.gain.exponentialRampToValueAtTime(0.18, t0 + inicio + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.0001, t0 + inicio + 0.18);
      osc.connect(gain).connect(contexto.destination);
      osc.start(t0 + inicio);
      osc.stop(t0 + inicio + 0.2);
    });
  } catch { /* el navegador puede bloquear el audio hasta que haya interacción */ }
}
