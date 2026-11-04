import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { BookOpen, Plus } from "lucide-react";
import { api, mensajeError } from "../../api/client.js";
import { useNotif } from "../../context/NotifContext.jsx";
import { useAsync } from "../../utils/useAsync.js";
import { portadaCurso } from "../../utils/portada.js";
import { useTexto } from "../../utils/useTexto.js";
import { Badge, Button, Cargando, EstadoError, PageHeader, ProgressBar, Vacio } from "../../components/ui/index.jsx";

const TARJETA =
  "group relative flex flex-col overflow-hidden rounded-card border border-line bg-surface transition duration-200 hover:-translate-y-0.5 hover:border-primary-300 hover:shadow-lg has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-primary-600";
const PANEL = "rounded-card border border-line bg-surface p-4";

/** Portada de la tarjeta, igual que la del inicio del estudiante. */
function PortadaCurso({ curso, etiqueta }) {
  const [fallo, setFallo] = useState(false);

  return (
    <div className="relative h-28 w-full overflow-hidden bg-gradient-to-br from-primary-700 to-primary-300 text-white sm:h-32">
      {!fallo ? (
        <img
          src={portadaCurso(curso)}
          alt=""
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={() => setFallo(true)}
          className="absolute inset-0 h-full w-full object-cover"
        />
      ) : (
        <span className="absolute inset-0 flex items-center justify-center">
          <BookOpen size={28} strokeWidth={1.6} aria-hidden="true" />
        </span>
      )}
      {etiqueta && (
        <span className="absolute left-2 top-2 z-10 rounded-full bg-white/95 px-2 py-0.5 text-[11px] font-extrabold uppercase tracking-wide text-primary-700">
          {etiqueta}
        </span>
      )}
    </div>
  );
}

/** Cabecera de sección igual que en el resto de pestañas del estudiante. */
function TituloSeccion({ id, titulo, total }) {
  return (
    <div className="mb-3 flex items-center justify-between gap-3">
      <h2 id={id} className="text-xl">{titulo}</h2>
      <Badge tono="gris">{total} {total === 1 ? "curso" : "cursos"}</Badge>
    </div>
  );
}

export default function EstudianteCursos() {
  const t = useTexto();
  const [params] = useSearchParams();
  const q = (params.get("q") ?? "").trim().toLowerCase();
  const { avisar } = useNotif();
  const { datos, cargando, error, recargar } = useAsync(() => api.cursos.catalogo(), []);
  const [inscribiendo, setInscribiendo] = useState(null);

  if (cargando && !datos) return <Cargando texto={t("Cargando cursos…")} />;
  if (error && !datos) return <EstadoError mensaje={error} onReintentar={recargar} />;

  const lista = datos ?? [];
  const filtrar = (l) => l.filter((c) => !q || `${c.titulo} ${c.instructor ?? ""} ${c.descripcion ?? ""}`.toLowerCase().includes(q));
  const mios = filtrar(lista.filter((c) => c.inscrito));
  const otros = filtrar(lista.filter((c) => !c.inscrito));

  const inscribirme = async (c) => {
    setInscribiendo(c.id);
    try {
      await api.cursos.inscribirme(c.id);
      avisar(t(`Te inscribiste en «${c.titulo}».`, `Ya estás en «${c.titulo}».`), "ok");
      recargar();
    } catch (e) {
      avisar(mensajeError(e), "error");
    } finally {
      setInscribiendo(null);
    }
  };

  return (
    <>
      <PageHeader titulo={t("Mis cursos")} subtitulo={t("Tus cursos y el catálogo disponible.")} />

      {q && (
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-opt border border-line bg-subtle px-4 py-3" role="status">
          <p className="text-sm font-semibold">
            {t("Resultados para", "Resultados de")} «{params.get("q")}»: <strong>{mios.length + otros.length}</strong>
          </p>
          <Link to="/cursos" className="font-bold text-primary-600 underline">{t("Limpiar búsqueda", "Ver todos")}</Link>
        </div>
      )}

      <section aria-labelledby="cursos-en-curso" className="mb-9">
        <TituloSeccion id="cursos-en-curso" titulo={t("En curso", "Tus cursos")} total={mios.length} />
        {mios.length === 0 ? (
          <Vacio icono={BookOpen} titulo={t("Aún no estás inscrito en cursos")}>
            {t("Explora el catálogo y elige el curso que quieras.", "Mira el catálogo y elige un curso.")}
          </Vacio>
        ) : (
          <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {mios.map((c) => {
              const pct = Math.round(Number(c.porcentaje ?? 0));
              return (
                <li key={c.id} className={TARJETA}>
                  <PortadaCurso curso={c} etiqueta={pct >= 100 ? t("Completado", "Terminado") : t("En curso", "Seguir")} />
                  <div className="flex min-w-0 flex-1 flex-col gap-1 p-2.5 sm:p-3">
                    <h3 className="line-clamp-2 text-base! leading-snug!">
                      <Link
                        to={`/cursos/${c.id}`}
                        className="text-inherit no-underline after:absolute after:inset-0 hover:underline"
                      >
                        {c.titulo}
                      </Link>
                    </h3>
                    <div className="mt-auto pt-1">
                      <p className="mb-1 text-xs font-semibold text-muted">{t("Tu avance", "Tu progreso")}</p>
                      <ProgressBar valor={pct} etiqueta={`${t("Avance en", "Progreso en")} ${c.titulo}`} />
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      <section aria-labelledby="catalogo-titulo">
        <TituloSeccion id="catalogo-titulo" titulo={t("Catálogo")} total={otros.length} />
        {otros.length === 0 ? (
          <Vacio icono={BookOpen} titulo={t("No hay más cursos disponibles por ahora")}>
            {t("Vuelve más tarde para ver nuevas opciones.", "Más adelante puede haber cursos nuevos.")}
          </Vacio>
        ) : (
          <div className={PANEL}>
            <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {otros.map((c) => (
                <li key={c.id} className={TARJETA}>
                  <PortadaCurso curso={c} etiqueta={t("Disponible")} />
                  <div className="flex min-w-0 flex-1 flex-col gap-1 p-2.5 sm:p-3">
                    <h3 className="line-clamp-2 text-base! leading-snug!">{c.titulo}</h3>
                    <div className="mt-auto pt-2">
                      <Button
                        variante="secondary"
                        bloque
                        cargando={inscribiendo === c.id}
                        icono={Plus}
                        onClick={() => inscribirme(c)}
                        className="px-3 text-sm"
                      >
                        {t("Inscribirme")}<span className="sr-only"> en {c.titulo}</span>
                      </Button>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}
      </section>
    </>
  );
}
