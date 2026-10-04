/** A dónde debe ir cada persona según su rol y lo que le falta completar (RF-002, RF-004, RF-014). */
export function destinoInicial(u) {
  if (!u) return "/login";
  if (u.primerAcceso) return "/primer-acceso";
  if (u.rol === "ESTUDIANTE") {
    if (!u.varkCompletado) return "/onboarding/test";
    if (!u.accesibilidadConfigurada) return "/onboarding/accesibilidad";
  }
  return "/inicio";
}
