import { usePrefs } from "../../context/PrefsContext.jsx";
import { useTalkback } from "../../utils/useTalkback.js";

/** Activa la narración de la interfaz cuando la persona la tiene habilitada. */
export function Narrador() {
  const { config } = usePrefs();
  useTalkback(config.narracionNavegacion);
  return null;
}
