export const FORMATOS = [
  { valor: "VIDEO", texto: "Video" },
  { valor: "PODCAST", texto: "Podcast" },
  { valor: "SIMULACION", texto: "Simulación" },
  { valor: "LECTURA", texto: "Lectura" },
];

export const TIPOS_RECURSO = {
  TEXTO_ALTERNATIVO: { texto: "Texto alternativo de imágenes", usaUrl: false, ayuda: "Describe lo que muestran las imágenes para quien usa lector de pantalla." },
  SUBTITULO: { texto: "Subtítulos", usaUrl: true, ayuda: "Enlace al archivo de subtítulos del video (por ejemplo .vtt)." },
  TRANSCRIPCION: { texto: "Transcripción", usaUrl: false, ayuda: "Escribe aquí el texto completo de lo que se dice en el video o el podcast." },
  AUDIO: { texto: "Audio", usaUrl: true, ayuda: "Enlace a una versión en audio del contenido." },
  LENGUA_SENAS: { texto: "Lengua de señas", usaUrl: true, ayuda: "Enlace a un video con interpretación en lengua de señas colombiana." },
  VERSION_SIMPLIFICADA: { texto: "Versión simplificada", usaUrl: false, ayuda: "Escribe el contenido con frases cortas y palabras sencillas." },
};

export const TIPOS_EVALUACION = [
  { valor: "QUIZ", texto: "Quiz" },
  { valor: "SIMULACION", texto: "Simulación" },
  { valor: "PRACTICA", texto: "Práctica" },
];

export const TIPOS_PREGUNTA = [
  { valor: "SELECCION_UNICA", texto: "Selección única (una respuesta correcta)" },
  { valor: "SELECCION_MULTIPLE", texto: "Selección múltiple (varias correctas)" },
  { valor: "VERDADERO_FALSO", texto: "Verdadero o falso" },
  { valor: "ABIERTA", texto: "Abierta (respuesta escrita)" },
];

export const EVENTOS_GAMIFICACION = [
  { valor: "CONTENIDO_COMPLETADO", texto: "Completar un contenido" },
  { valor: "EVALUACION_COMPLETADA", texto: "Completar una evaluación" },
  { valor: "TEST_VARK", texto: "Hacer el test VARK" },
  { valor: "CURSO_FINALIZADO", texto: "Finalizar un curso" },
];

export const tonoEstadoCurso = (estado) => ({ PUBLICADO: "ok", BORRADOR: "warn", ARCHIVADO: "gris" }[estado] ?? "gris");
