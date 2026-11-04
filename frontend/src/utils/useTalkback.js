import { useEffect, useRef } from "react";

/**
 * Narración de la interfaz (talkback).
 * Cuando la preferencia está activa, lee en voz alta el elemento que recibe
 * el foco al navegar con Tab: dónde está, qué es y en qué estado quedó.
 */

const RETARDO = 90;
const REPETIR_TRAS = 900;

const ROLES = {
  button: "botón",
  link: "enlace",
  checkbox: "casilla de verificación",
  switch: "interruptor",
  radio: "opción",
  tab: "pestaña",
  tablist: "pestañas",
  menuitem: "opción de menú",
  menuitemcheckbox: "opción de menú",
  menuitemradio: "opción de menú",
  menu: "menú",
  dialog: "diálogo",
  alertdialog: "diálogo de aviso",
  navigation: "navegación",
  main: "contenido principal",
  search: "búsqueda",
  progressbar: "barra de progreso",
  heading: "título",
  img: "imagen",
  combobox: "lista desplegable",
  listbox: "lista",
  option: "opción",
  table: "tabla",
  row: "fila",
  columnheader: "encabezado de columna",
  rowheader: "encabezado de fila",
  cell: "celda",
  group: "grupo",
  region: "sección",
  status: "estado",
  alert: "aviso",
  tooltip: "ayuda",
};

const REGIONES = {
  MAIN: "Contenido principal",
  NAV: "Navegación",
  ASIDE: "Panel lateral",
  HEADER: "Encabezado",
  FOOTER: "Pie de página",
  DIALOG: "Diálogo",
  FORM: "Formulario",
  SECTION: "Sección",
};

const CAMPOS = {
  checkbox: "casilla de verificación",
  radio: "opción",
  range: "control deslizante",
  search: "buscador",
  submit: "botón",
  button: "botón",
  reset: "botón",
  password: "campo de contraseña",
  file: "selector de archivo",
  email: "campo de correo",
  tel: "campo de teléfono",
  number: "campo numérico",
  date: "campo de fecha",
  time: "campo de hora",
  url: "campo de dirección web",
};

function limpiar(texto) {
  return String(texto ?? "").replace(/\s+/g, " ").trim();
}

function esRegion(el) {
  return ["MAIN", "NAV", "ASIDE", "HEADER", "FOOTER", "DIALOG", "FORM", "SECTION"].includes(el.tagName);
}

function textoVisible(el) {
  return limpiar(el.innerText || el.textContent);
}

function etiquetaDe(el) {
  const aria = limpiar(el.getAttribute("aria-label"));
  if (aria) return aria;
  const ids = limpiar(el.getAttribute("aria-labelledby")).split(" ").filter(Boolean);
  if (ids.length) {
    const texto = ids
      .map((id) => document.getElementById(id))
      .filter(Boolean)
      .map(textoVisible)
      .filter(Boolean)
      .join(" ");
    if (texto) return texto;
  }
  return "";
}

function nombreDe(el) {
  const etiqueta = etiquetaDe(el);
  if (etiqueta) return etiqueta;

  if (el.labels?.length) {
    const texto = Array.from(el.labels).map(textoVisible).filter(Boolean).join(" ");
    if (texto) return texto;
  }

  if (el.tagName === "IMG") {
    const alt = limpiar(el.getAttribute("alt"));
    if (alt) return alt;
  }

  if (el.tagName === "INPUT") {
    const atajos = {
      password: "Contraseña",
      search: "Buscar",
      email: "Correo electrónico",
      tel: "Teléfono",
      number: "Número",
      range: "Valor",
      file: "Archivo",
    };
    return atajos[el.type] ?? limpiar(el.placeholder || el.name);
  }

  if (el.tagName === "SELECT" || el.tagName === "TEXTAREA") {
    return limpiar(el.getAttribute("title") || el.name);
  }

  if (esRegion(el)) return "";

  const texto = textoVisible(el);
  if (texto) return texto;
  return limpiar(el.getAttribute("title"));
}

function rolDe(el) {
  const explicito = limpiar(el.getAttribute("role"));
  if (explicito) return ROLES[explicito] ?? explicito;
  if (el.tagName === "A") return "enlace";
  if (el.tagName === "BUTTON") return "botón";
  if (el.tagName === "SELECT") return "lista desplegable";
  if (el.tagName === "TEXTAREA") return "área de texto";
  if (el.tagName === "IMG") return "imagen";
  if (/^H[1-6]$/.test(el.tagName)) return "título";
  if (el.tagName === "INPUT") return CAMPOS[el.type] ?? "campo de texto";
  return REGIONES[el.tagName] ?? "";
}

function valorDe(el) {
  if (el.tagName === "SELECT") {
    const opcion = el.selectedOptions?.[0];
    const texto = opcion ? textoVisible(opcion) : "";
    return texto ? `seleccionado ${texto}` : "";
  }
  if (el.tagName === "TEXTAREA") {
    const texto = limpiar(el.value).slice(0, 80);
    return texto ? `con el texto ${texto}` : "";
  }
  if (el.tagName !== "INPUT") return "";
  if (el.type === "password") return "";
  if (el.type === "range") return `valor ${el.value} de ${el.max || 100}`;
  if (["checkbox", "radio", "button", "submit", "reset", "file", "hidden"].includes(el.type)) return "";
  const texto = limpiar(el.value).slice(0, 80);
  return texto ? `con el texto ${texto}` : "";
}

function estadoDe(el) {
  const estados = [];

  const marcado = el.getAttribute("aria-checked");
  if (marcado === "true") estados.push("marcado");
  else if (marcado === "mixed") estados.push("marcado parcialmente");
  else if (marcado === "false") estados.push("no marcado");

  if (el.matches?.('input[type="checkbox"], input[type="radio"]')) {
    estados.push(el.checked ? "marcado" : "no marcado");
  }

  const seleccionado = el.getAttribute("aria-selected");
  if (seleccionado === "true") estados.push("seleccionado");
  else if (seleccionado === "false") estados.push("no seleccionado");

  const expandido = el.getAttribute("aria-expanded");
  if (expandido === "true") estados.push("expandido");
  else if (expandido === "false") estados.push("contraído");

  const presionado = el.getAttribute("aria-pressed");
  if (presionado === "true") estados.push("presionado");
  else if (presionado === "false") estados.push("no presionado");

  const actual = el.getAttribute("aria-current");
  if (actual) estados.push(actual === "page" ? "página actual" : actual === "step" ? "paso actual" : "actual");

  if (el.disabled || el.getAttribute("aria-disabled") === "true") estados.push("deshabilitado");
  if (el.required || el.getAttribute("aria-required") === "true") estados.push("obligatorio");
  if (el.getAttribute("aria-invalid") === "true") estados.push("con error");

  return [...new Set(estados)].join(", ");
}

function contextoDe(el) {
  const region = el.closest(
    'main, nav, aside, header, footer, [role="dialog"], [role="alertdialog"], [role="menu"], [role="tablist"], [role="search"], form[role="search"]',
  );
  if (!region || region === el) return "";

  const etiqueta = etiquetaDe(region);
  if (etiqueta) return etiqueta;

  const rol = limpiar(region.getAttribute("role"));
  if (rol === "search") return "Búsqueda";
  if (rol === "dialog" || rol === "alertdialog") return "Diálogo";
  if (rol === "menu") return "Menú";
  if (rol === "tablist") return "Pestañas";

  return REGIONES[region.tagName] ?? "";
}

function describir(el, incluirContexto) {
  if (!el || el === document.body || el.getAttribute?.("aria-hidden") === "true") return "";

  const principal = el.tagName === "MAIN" || el.getAttribute("role") === "main";
  if (principal) {
    const titulo = el.querySelector("h1, h2");
    const texto = titulo ? nombreDe(titulo) : "";
    return texto ? `Página ${texto}` : "Contenido principal";
  }

  const nombre = nombreDe(el);
  const rol = rolDe(el);
  if (!nombre && !rol) return "";

  const partes = [];
  const contexto = incluirContexto ? contextoDe(el) : "";
  if (contexto) partes.push(`En ${contexto}`);
  if (nombre) partes.push(nombre);
  if (rol) partes.push(rol);
  const valor = valorDe(el);
  if (valor) partes.push(valor);
  const estado = estadoDe(el);
  if (estado) partes.push(estado);
  return `${partes.join(", ")}.`;
}

function controlConEstado(el) {
  return el.matches(
    'input[type="checkbox"], input[type="radio"], select, [role="switch"], [role="checkbox"], [role="radio"], [role="tab"], [role="option"], [role="menuitemcheckbox"], [role="menuitemradio"]',
  );
}

function controlConEstadoCambiante(el) {
  return el.matches(
    'button, [role="button"], [role="switch"], [role="checkbox"], [role="radio"], [role="tab"], [role="menuitem"], [role="option"], [role="menuitemcheckbox"], [role="menuitemradio"]',
  );
}

function tieneEstado(el) {
  return (
    el.hasAttribute("aria-checked") ||
    el.hasAttribute("aria-expanded") ||
    el.hasAttribute("aria-selected") ||
    el.hasAttribute("aria-pressed") ||
    el.hasAttribute("aria-current") ||
    el.matches?.('input[type="checkbox"], input[type="radio"]')
  );
}

export function useTalkback(activo) {
  const temporizador = useRef(null);
  const ultimo = useRef({ el: null, texto: "", contexto: null, momento: 0 });

  useEffect(() => {
    if (!activo) return undefined;
    const sintesis = window.speechSynthesis;
    if (!sintesis) return undefined;
    let vigente = true;

    const hablar = (texto) => {
      if (!texto) return;
      sintesis.cancel();
      if (typeof SpeechSynthesisUtterance === "undefined") return;
      const voz = new SpeechSynthesisUtterance(texto);
      voz.lang = "es-CO";
      voz.rate = 1;
      const voces = sintesis.getVoices?.() ?? [];
      const escogida =
        voces.find((v) => /^es[-_]CO/i.test(v.lang)) ||
        voces.find((v) => /^es/i.test(v.lang));
      if (escogida) voz.voice = escogida;
      sintesis.speak(voz);
    };

    const anunciar = (el, { inmediato = false, conContexto = true } = {}) => {
      if (!(el instanceof HTMLElement)) return;
      clearTimeout(temporizador.current);
      const contexto = contextoDe(el);
      const incluir = conContexto && contexto && contexto !== ultimo.current.contexto;
      const texto = describir(el, incluir);
      if (!texto) return;

      const ahora = Date.now();
      const previo = ultimo.current;
      const repetido =
        previo.el === el && previo.texto === texto && ahora - previo.momento < REPETIR_TRAS;
      if (repetido) return;

      ultimo.current = { el, texto, contexto, momento: ahora };
      if (inmediato) hablar(texto);
      else temporizador.current = setTimeout(() => hablar(texto), RETARDO);
    };

    const alEnfocar = (e) => {
      if (e.target instanceof HTMLElement) anunciar(e.target);
    };

    const alCambiar = (e) => {
      const el = e.target;
      if (!(el instanceof HTMLElement) || !controlConEstado(el)) return;
      setTimeout(() => {
        if (vigente) anunciar(el, { inmediato: true, conContexto: false });
      }, 30);
    };

    const alHacerClic = (e) => {
      const el = e.target instanceof HTMLElement ? e.target.closest("button, [role]") : null;
      if (!el || !controlConEstadoCambiante(el) || !tieneEstado(el)) return;
      setTimeout(() => {
        if (vigente) anunciar(el, { inmediato: true, conContexto: false });
      }, 60);
    };

    document.addEventListener("focusin", alEnfocar);
    document.addEventListener("change", alCambiar, true);
    document.addEventListener("click", alHacerClic, true);

    return () => {
      vigente = false;
      clearTimeout(temporizador.current);
      document.removeEventListener("focusin", alEnfocar);
      document.removeEventListener("change", alCambiar, true);
      document.removeEventListener("click", alHacerClic, true);
      sintesis.cancel();
    };
  }, [activo]);
}
