import axios from "axios";

const CLAVE_TOKEN = "vl.token";

export const token = {
  get() {
    try { return sessionStorage.getItem(CLAVE_TOKEN); } catch { return null; }
  },
  set(valor) {
    try { sessionStorage.setItem(CLAVE_TOKEN, valor); } catch { /* sin almacenamiento */ }
  },
  clear() {
    try { sessionStorage.removeItem(CLAVE_TOKEN); } catch { /* sin almacenamiento */ }
  },
};

export const client = axios.create({ baseURL: "/api", timeout: 130000 });

client.interceptors.request.use((config) => {
  const t = token.get();
  if (t) config.headers.Authorization = `Bearer ${t}`;
  return config;
});

/** Si la sesión caduca, se limpia y se avisa a la app para que lleve al login. */
client.interceptors.response.use(
  (r) => r,
  (error) => {
    const status = error.response?.status;
    const ruta = error.config?.url ?? "";
    if (status === 401 && !ruta.startsWith("/auth/login") && !ruta.startsWith("/auth/otp")) {
      token.clear();
      window.dispatchEvent(new CustomEvent("vl:sesion-expirada", { detail: error.response?.data?.mensaje }));
    }
    return Promise.reject(error);
  },
);

/** Mensaje en lenguaje simple: lo que dijo el servidor o, si no hay respuesta, qué hacer. */
export function mensajeError(error, porDefecto = "Algo salió mal. Intenta de nuevo en un momento.") {
  const data = error?.response?.data;
  if (data?.mensaje) return data.mensaje;
  if (error?.code === "ERR_NETWORK" || !error?.response) {
    return "No pudimos conectar con el servidor. Revisa tu conexión e intenta de nuevo.";
  }
  return porDefecto;
}

export const codigoError = (error) => error?.response?.data?.codigo ?? null;
export const detallesError = (error) => error?.response?.data?.detalles ?? null;

const d = (p) => p.then((r) => r.data);

/** Descarga un archivo binario (informes y exportaciones). */
async function descargar(promesa, nombreAlterno) {
  const r = await promesa;
  const cd = r.headers["content-disposition"] ?? "";
  const m = /filename\*=UTF-8''([^;]+)/i.exec(cd) ?? /filename="?([^";]+)"?/i.exec(cd);
  const nombre = m ? decodeURIComponent(m[1]) : nombreAlterno;
  const url = URL.createObjectURL(r.data);
  const a = document.createElement("a");
  a.href = url;
  a.download = nombre;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
  return nombre;
}

/** Si el servidor devolvió un JSON de error dentro de un blob, lo convierte para mostrarlo. */
export async function errorDeBlob(error) {
  const blob = error?.response?.data;
  if (blob instanceof Blob) {
    try {
      const texto = await blob.text();
      error.response.data = JSON.parse(texto);
    } catch { /* se deja como está */ }
  }
  return error;
}

export const api = {
  auth: {
    login: (correo, password) => d(client.post("/auth/login", { correo, password })),
    verificarOtp: (correo, codigo) => d(client.post("/auth/otp/verificar", { correo, codigo })),
    recuperar: (correo) => d(client.post("/auth/recuperar", { correo })),
    restablecer: (tokenRec, nuevaPassword) => d(client.post("/auth/restablecer", { token: tokenRec, nuevaPassword })),
    me: () => d(client.get("/auth/me")),
    primerAcceso: (passwordActual, nuevaPassword) => d(client.post("/auth/primer-acceso", { passwordActual, nuevaPassword })),
    logout: () => d(client.post("/auth/logout")),
  },
  vark: {
    preguntas: () => d(client.get("/vark/preguntas")),
    parcial: () => d(client.get("/vark/parcial")),
    responder: (numero, opcion) => d(client.put(`/vark/respuestas/${numero}`, { opcion })),
    enviar: () => d(client.post("/vark/enviar")),
    metodos: () => d(client.get("/vark/metodos")),
    guardarMetodos: (principal, secundario) => d(client.put("/vark/metodos", { principal, secundario })),
  },
  accesibilidad: {
    perfil: () => d(client.get("/accesibilidad/perfil")),
    guardarPerfil: (categorias) => d(client.put("/accesibilidad/perfil", { categorias })),
    previsualizar: (categorias) => d(client.post("/accesibilidad/previsualizar", { categorias })),
    configuracion: () => d(client.get("/accesibilidad/configuracion")),
    guardarConfiguracion: (cfg) => d(client.put("/accesibilidad/configuracion", cfg)),
    restablecer: () => d(client.post("/accesibilidad/restablecer")),
    temas: () => d(client.get("/accesibilidad/temas")),
  },
  cursos: {
    catalogo: () => d(client.get("/cursos")),
    mios: () => d(client.get("/mis-cursos")),
    detalle: (id) => d(client.get(`/cursos/${id}`)),
    inscribirme: (id) => d(client.post(`/cursos/${id}/inscribirme`)),
  },
  contenidos: {
    ver: (id) => d(client.get(`/contenidos/${id}`)),
    apoyo: (id) => d(client.get(`/contenidos/${id}/apoyo-cognitivo`)),
    progreso: (id, datos) => d(client.post(`/contenidos/${id}/progreso`, datos)),
  },
  evaluaciones: {
    delModulo: (moduloId) => d(client.get(`/modulos/${moduloId}/evaluaciones`)),
    iniciar: (id) => d(client.post(`/evaluaciones/${id}/intentos`)),
    historial: (id) => d(client.get(`/evaluaciones/${id}/historial`)),
    guardar: (intentoId, preguntaId, valor) => d(client.put(`/intentos/${intentoId}/respuestas/${preguntaId}`, { valor })),
    finalizar: (intentoId) => d(client.post(`/intentos/${intentoId}/finalizar`)),
    resultado: (intentoId) => d(client.get(`/intentos/${intentoId}/resultado`)),
  },
  progreso: {
    dashboard: () => d(client.get("/progreso")),
    exportar: (formato) => descargar(client.get("/progreso/exportar", { params: { formato }, responseType: "blob" }),
      formato === "PDF" ? "mi-progreso.pdf" : "mi-progreso.xlsx"),
  },
  logros: {
    ver: () => d(client.get("/logros")),
    privacidad: (mostrarEnRanking) => d(client.put("/logros/privacidad", { mostrarEnRanking })),
    ranking: (cursoId) => d(client.get(`/cursos/${cursoId}/ranking`)),
  },
  chat: {
    historial: (cursoId, limite = 50) => d(client.get(`/cursos/${cursoId}/mensajes`, { params: { limite } })),
    enviar: (cursoId, contenido) => d(client.post(`/cursos/${cursoId}/mensajes`, { contenido })),
    reportar: (id) => d(client.post(`/mensajes/${id}/reportar`)),
    ocultar: (id) => d(client.post(`/mensajes/${id}/ocultar`)),
  },
  notificaciones: {
    listar: (limite = 30) => d(client.get("/notificaciones", { params: { limite } })),
    leida: (id) => d(client.patch(`/notificaciones/${id}/leida`)),
    posponer: (id, opcion) => d(client.post(`/notificaciones/${id}/posponer`, { opcion })),
    preferencias: () => d(client.get("/notificaciones/preferencias")),
    guardarPreferencias: (habilitadas, canal) => d(client.put("/notificaciones/preferencias", { habilitadas, canal })),
  },
  tutor: {
    consultar: (pregunta, contenidoId) => d(client.post("/tutor/consultas", { pregunta, contenidoId })),
    util: (id, util) => d(client.post(`/tutor/respuestas/${id}/util`, { util })),
    historial: (limite = 30) => d(client.get("/tutor/historial", { params: { limite } })),
  },
  instructor: {
    cursos: () => d(client.get("/instructor/cursos")),
    crearCurso: (datos) => d(client.post("/instructor/cursos", datos)),
    curso: (id) => d(client.get(`/instructor/cursos/${id}`)),
    editarCurso: (id, datos) => d(client.put(`/instructor/cursos/${id}`, datos)),
    publicarCurso: (id) => d(client.post(`/instructor/cursos/${id}/publicar`)),
    archivarCurso: (id) => d(client.post(`/instructor/cursos/${id}/archivar`)),
    inscribir: (id, correos) => d(client.post(`/instructor/cursos/${id}/inscripciones`, { correos })),
    crearModulo: (cursoId, datos) => d(client.post(`/instructor/cursos/${cursoId}/modulos`, datos)),
    editarModulo: (id, datos) => d(client.put(`/modulos/${id}`, datos)),
    crearContenido: (moduloId, datos) => d(client.post(`/modulos/${moduloId}/contenidos`, datos)),
    editarContenido: (id, datos) => d(client.put(`/contenidos/${id}`, datos)),
    recursos: (contenidoId) => d(client.get(`/contenidos/${contenidoId}/recursos`)),
    crearRecurso: (contenidoId, datos) => d(client.post(`/contenidos/${contenidoId}/recursos`, datos)),
    borrarRecurso: (id) => d(client.delete(`/recursos/${id}`)),
    conformidad: (contenidoId) => d(client.get(`/contenidos/${contenidoId}/conformidad`)),
    publicarContenido: (id) => d(client.post(`/contenidos/${id}/publicar`)),
    despublicarContenido: (id) => d(client.post(`/contenidos/${id}/despublicar`)),
    crearEvaluacion: (moduloId, datos) => d(client.post(`/modulos/${moduloId}/evaluaciones`, datos)),
    editarEvaluacion: (id, datos) => d(client.put(`/evaluaciones/${id}`, datos)),
    borrarEvaluacion: (id) => d(client.delete(`/evaluaciones/${id}`)),
    agregarPregunta: (evaluacionId, datos) => d(client.post(`/evaluaciones/${evaluacionId}/preguntas`, datos)),
  },
  tareas: {
    delModulo: (moduloId) => d(client.get(`/modulos/${moduloId}/tareas`)),
    entregar: (tareaId, datos) => d(client.put(`/tareas/${tareaId}/entrega`, datos)),
    // Instructor
    delModuloInstructor: (moduloId) => d(client.get(`/instructor/modulos/${moduloId}/tareas`)),
    crear: (moduloId, datos) => d(client.post(`/modulos/${moduloId}/tareas`, datos)),
    editar: (id, datos) => d(client.put(`/tareas/${id}`, datos)),
    borrar: (id) => d(client.delete(`/tareas/${id}`)),
    entregas: (id) => d(client.get(`/tareas/${id}/entregas`)),
    calificar: (entregaId, datos) => d(client.put(`/entregas/${entregaId}/calificacion`, datos)),
  },
  informes: {
    vista: (filtros) => d(client.post("/informes/vista", filtros)),
    generar: (filtros) => descargar(client.post("/informes", filtros, { responseType: "blob" }),
      filtros.formato === "PDF" ? "informe.pdf" : "informe.xlsx"),
    historial: () => d(client.get("/informes")),
  },
  admin: {
    dashboard: () => d(client.get("/admin/dashboard")),
    auditoria: (params) => d(client.get("/admin/auditoria", { params })),
    usuarios: (params) => d(client.get("/admin/usuarios", { params })),
    crearUsuario: (datos) => d(client.post("/admin/usuarios", datos)),
    estadoUsuario: (id, estado) => d(client.patch(`/admin/usuarios/${id}/estado`, { estado })),
    reenviar: (id) => d(client.post(`/admin/usuarios/${id}/reenviar-credenciales`)),
    cursos: () => d(client.get("/admin/cursos")),
    inscribir: (id, correos) => d(client.post(`/admin/cursos/${id}/inscripciones`, { correos })),
    reglas: () => d(client.get("/admin/gamificacion/reglas")),
    crearRegla: (datos) => d(client.post("/admin/gamificacion/reglas", datos)),
    editarRegla: (id, datos) => d(client.put(`/admin/gamificacion/reglas/${id}`, datos)),
    insignias: () => d(client.get("/admin/gamificacion/insignias")),
    crearInsignia: (datos) => d(client.post("/admin/gamificacion/insignias", datos)),
    editarInsignia: (id, datos) => d(client.put(`/admin/gamificacion/insignias/${id}`, datos)),
  },
};
