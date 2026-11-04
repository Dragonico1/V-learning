/** Portadas de curso por temática. Se usan cuando el curso no trae una imagen propia. */

const BASE = "https://images.unsplash.com/photo-";

const PARAMS = "?auto=format&fit=crop&w=900&q=75";

const IMAGEN_POR_TEMA = {
  python: "1526379095098-d400fd0bf935",

  react: "1633356122544-f134324a6cee",

  web: "1547658719-da2b51169166",

  datos: "1558494949-ef010cbdcc31",

  scrum: "1552664730-d307ca884978",

  seguridad: "1614064641938-3bbee52942c7",

  java: "1515879218367-8466d910aaa4",

  codigo: "1461749280684-dccba630e2f6",

  general: "1531297484001-80022131f5a1",
};

/** Tema del curso según las palabras de su título. */

export function temaCurso(titulo = "") {
  const t = titulo

    .normalize("NFD")

    .replace(/[\u0300-\u036f]/g, "")

    .toLowerCase();

  if (/python/.test(t)) return "python";

  if (/react/.test(t)) return "react";

  if (/seguridad|ciber/.test(t)) return "seguridad";

  if (/scrum|agil|ingenieria de software/.test(t)) return "scrum";

  if (/datos|sql|base de datos/.test(t)) return "datos";

  if (/\bweb\b|html|css|javascript/.test(t)) return "web";

  if (/\bjava\b/.test(t)) return "java";

  if (/program|codigo|algoritm|software/.test(t)) return "codigo";

  return "general";
}

/** URL de la portada: la del curso si existe o una foto acorde a su temática. */

export function portadaCurso(curso) {
  return (
    curso?.imagen ||
    `${BASE}${IMAGEN_POR_TEMA[temaCurso(curso?.titulo)]}${PARAMS}`
  );
}
