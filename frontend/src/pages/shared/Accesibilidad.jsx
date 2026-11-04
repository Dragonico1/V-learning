import { Fragment, useState } from "react";
import { Brain, Ear, Eye, Hand, RotateCcw } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useAuth } from "../../context/AuthContext.jsx";
import { usePrefs } from "../../context/PrefsContext.jsx";
import { useNotif } from "../../context/NotifContext.jsx";
import { useTexto } from "../../utils/useTexto.js";
import { CATEGORIAS } from "../../utils/format.js";
import {
  Banner,
  Button,
  Card,
  OpcionCard,
  PageHeader,
  SelectField,
  Toggle,
} from "../../components/ui/index.jsx";
import ConfirmarDialogo from "../../components/ConfirmarDialogo.jsx";
import VolverACursos from "../../components/VolverACursos.jsx";

const ICONO = { VISUAL: Eye, AUDITIVA: Ear, MOTORA: Hand, COGNITIVA: Brain };
const FUENTES = [
  "Poppins + Inter",
  "Atkinson Hyperlegible Next",
  "Lexend",
  "Arial",
  "Verdana",
];
const hhmm = (d) =>
  d.toLocaleTimeString("es-CO", { hour: "2-digit", minute: "2-digit" });

/**
 * RF-014 / RF-016: ajustes de accesibilidad con aplicación inmediata.
 * Con `embebido` se muestra dentro de otra página (p. ej. Configuración):
 * sin encabezado ni botón "Volver" propios y en una sola columna.
 */
export default function Accesibilidad({ embebido = false }) {
  const t = useTexto();
  const { usuario } = useAuth();
  const {
    config,
    categorias,
    actualizar,
    reemplazar,
    restablecer,
    guardadoEn,
    guardando,
    error,
    advertencias,
    sugerencia,
    setSugerencia,
    persistenteEnServidor,
  } = usePrefs();
  const { avisar } = useNotif();
  const esEstudiante = usuario.rol === "ESTUDIANTE";
  const [confirmar, setConfirmar] = useState(false);
  const [errorCat, setErrorCat] = useState(null);

  const alternarCategoria = async (k) => {
    const s = new Set(categorias);
    s.has(k) ? s.delete(k) : s.add(k);
    setErrorCat(null);
    try {
      const p = await api.accesibilidad.guardarPerfil([...s]);
      reemplazar(p.configuracion, p.categorias);
      avisar(t("Actualizamos tu perfil de accesibilidad.", "Actualizamos tu perfil."), "ok");
    } catch (e) {
      setErrorCat(mensajeError(e));
    }
  };
  const aplicarSugerencia = () => {
    actualizar(sugerencia);
    setSugerencia(null);
  };

  const Raiz = embebido ? "div" : Fragment;

  return (
    <Raiz>
      {!embebido && <VolverACursos />}
      {!embebido && (
        <PageHeader
          eyebrow={t("Accesibilidad")}
          titulo={t("Ajusta la plataforma a ti", "Ajusta la plataforma a tu medida")}
          subtitulo={t("Los cambios se aplican de inmediato y se guardan solos.", "Los cambios se ven al momento y se guardan solos.")}
        />
      )}
      {!persistenteEnServidor && (
        <Banner tono="info" className="mb-5">
          {t("Como", "Eres")} {usuario.rol === "INSTRUCTOR" ? t("instructor", "profesor") : t("administrador", "administrador")}: {t("tus ajustes se guardan solo en este dispositivo.", "tus ajustes se guardan solo en este equipo.")}
        </Banner>
      )}
      <p className="mb-4 min-h-6 font-semibold" role="status">
        {error ? (
          <span className="text-err-fg">{error}</span>
        ) : guardando ? (
          t("Guardando…")
        ) : guardadoEn ? (
          `${t("Cambios guardados automáticamente a las", "Cambios guardados a las")} ${hhmm(guardadoEn)}`
        ) : (
          ""
        )}
      </p>
      {advertencias.length > 0 && (
        <Banner tono="warn" className="mb-5" rol="status">
          <ul className="list-disc pl-5">
            {advertencias.map((a) => (
              <li key={a}>{a}</li>
            ))}
          </ul>
          {sugerencia && (
            <Button
              className="mt-3"
              variante="secondary"
              onClick={aplicarSugerencia}
            >
              {t("Aplicar la combinación sugerida")}
            </Button>
          )}
        </Banner>
      )}
      <div className={embebido ? "grid gap-6" : "grid gap-6 xl:grid-cols-2"}>
        {esEstudiante && (
          <Card as="section" aria-labelledby="cat">
            <h2 id="cat" className="mb-3 text-xl">
              {t("Tu perfil", "Tu perfil de accesibilidad")}
            </h2>
            {errorCat && (
              <Banner tono="err" className="mb-3" rol="alert">
                {errorCat}
              </Banner>
            )}
            <div role="group" aria-labelledby="cat" className="grid gap-3">
              {Object.entries(CATEGORIAS).map(([k, v]) => (
                <OpcionCard
                  key={k}
                  multiple
                  icono={ICONO[k]}
                  titulo={v.nombre}
                  descripcion={t(v.detalle)}
                  seleccionada={categorias.includes(k)}
                  onSelect={() => alternarCategoria(k)}
                />
              ))}
            </div>
          </Card>
        )}
        <Card as="section" aria-labelledby="aj">
          <h2 id="aj" className="mb-2 text-xl">
            {t("Ajustes manuales")}
          </h2>
          <div className="grid gap-4">
            <div>
              <label
                htmlFor="tam"
                className="mb-1.5 block font-bold text-strong"
              >
                {t("Tamaño del texto:", "Tamaño de la letra:")} {config.tamanoFuente} px
              </label>
              <input
                id="tam"
                type="range"
                min={12}
                max={32}
                step={1}
                value={config.tamanoFuente}
                onChange={(e) =>
                  actualizar({ tamanoFuente: Number(e.target.value) })
                }
                className="h-8 w-full accent-primary-600"
              />
            </div>
            <div>
              <label
                htmlFor="lh"
                className="mb-1.5 block font-bold text-strong"
              >
                {t("Interlineado:", "Espacio entre líneas:")} {Number(config.espaciadoLinea).toFixed(1)}
              </label>
              <input
                id="lh"
                type="range"
                min={1}
                max={3}
                step={0.1}
                value={config.espaciadoLinea}
                onChange={(e) =>
                  actualizar({
                    espaciadoLinea: Number(Number(e.target.value).toFixed(1)),
                  })
                }
                className="h-8 w-full accent-primary-600"
              />
            </div>
            <SelectField
              label={t("Tipografía", "Tipo de letra")}
              value={config.tipografia}
              onChange={(e) => actualizar({ tipografia: e.target.value })}
            >
              {FUENTES.map((f) => (
                <option key={f}>{f}</option>
              ))}
            </SelectField>
            {esEstudiante && (
              <div>
                <label
                  htmlFor="ta"
                  className="mb-1.5 block font-bold text-strong"
                >
                  {t("Tiempo adicional en evaluaciones", "Tiempo extra en las pruebas")}: {config.tiempoAdicional} %
                </label>
                <input
                  id="ta"
                  type="range"
                  min={0}
                  max={200}
                  step={10}
                  value={config.tiempoAdicional}
                  onChange={(e) =>
                    actualizar({ tiempoAdicional: Number(e.target.value) })
                  }
                  className="h-8 w-full accent-primary-600"
                />
              </div>
            )}
            <div className="divide-y divide-line">
              <Toggle
                label={t("Alto contraste")}
                descripcion={t("Colores con más contraste.", "Colores más fuertes para ver mejor.")}
                checked={config.altoContraste}
                onChange={(v) => actualizar({ altoContraste: v })}
              />
              <Toggle
                label={t("Navegación con teclado")}
                descripcion={t("Muestra siempre el foco y los atajos.")}
                checked={config.navegacionTeclado}
                onChange={(v) => actualizar({ navegacionTeclado: v })}
              />
              <Toggle
                label={t("Optimizar para lector de pantalla")}
                descripcion={t("Prioriza contenidos con texto alternativo.")}
                checked={config.lectorPantalla}
                onChange={(v) => actualizar({ lectorPantalla: v })}
              />
              {esEstudiante && (
                <>
                  <Toggle
                    label={t("Subtítulos")}
                    checked={config.subtitulos}
                    onChange={(v) => actualizar({ subtitulos: v })}
                  />
                  <Toggle
                    label={t("Transcripciones")}
                    checked={config.transcripcion}
                    onChange={(v) => actualizar({ transcripcion: v })}
                  />
                  <Toggle
                    label={t("Lectura en voz alta")}
                    descripcion={t("Botón «Leer en voz alta» en las lecciones.")}
                    checked={config.textoAVoz}
                    onChange={(v) => actualizar({ textoAVoz: v })}
                  />
                </>
              )}
            </div>
            <div>
              <Button
                variante="secondary"
                icono={RotateCcw}
                onClick={() => setConfirmar(true)}
              >
                {t("Restablecer valores predeterminados")}
              </Button>
            </div>
          </div>
        </Card>
      </div>
      <Card className="mt-6">
        <h2 className="mb-2 text-xl">{t("Vista previa del texto")}</h2>
        <p className="lesson-text">
          {t("Así se verán las lecciones con tus ajustes. Un buen contraste, un tamaño cómodo y un interlineado amplio ayudan a leer con menos esfuerzo.", "Así se verán las lecciones con tus ajustes. Los colores, el tamaño y el espacio entre líneas te ayudan a leer mejor.")}
        </p>
      </Card>
      <ConfirmarDialogo
        abierto={confirmar}
        onCerrar={() => setConfirmar(false)}
        titulo={t("¿Restablecer los ajustes?", "¿Volver a los ajustes originales?")}
        textoConfirmar={t("Sí, restablecer", "Sí, volver")}
        onConfirmar={async () => {
          await restablecer();
          avisar(t("Restablecimos los valores predeterminados.", "Volvimos a los ajustes originales."), "ok");
        }}
      >
        <p>
          {t("Tus ajustes volverán a los valores originales. Tu perfil de categorías se conserva.", "Tus ajustes volverán a como estaban al principio. Tu perfil de accesibilidad se mantiene.")}
        </p>
      </ConfirmarDialogo>
    </Raiz>
  );
}
