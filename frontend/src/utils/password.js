export const REGLAS_PASSWORD = [
  ["Al menos 10 caracteres", (p) => p.length >= 10],
  ["Una letra mayúscula", (p) => /[A-Z]/.test(p)],
  ["Una letra minúscula", (p) => /[a-z]/.test(p)],
  ["Un número", (p) => /\d/.test(p)],
  ["Un símbolo (por ejemplo # o !)", (p) => /[^A-Za-z0-9]/.test(p)],
];

export const passwordValida = (p) => REGLAS_PASSWORD.every(([, f]) => f(p));
