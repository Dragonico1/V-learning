import { Link } from "react-router-dom";
import { BookOpen, GraduationCap, Lock, Users } from "lucide-react";
import { api } from "../../api/client.js";
import { useAsync } from "../../utils/useAsync.js";
import { fmtNumero } from "../../utils/format.js";
import { Card, Cargando, EstadoError, PageHeader, ProgressBar } from "../../components/ui/index.jsx";

function Metrica({ icono: Icono, valor, texto }) {
  return (
    <Card as="li" className="list-none">
      <Icono size={22} aria-hidden="true" className="mb-2 text-primary-600" />
      <p className="text-3xl font-extrabold text-strong">{valor}</p>
      <p className="text-sm">{texto}</p>
    </Card>
  );
}

export default function AdminInicio() {
  const { datos: d, cargando, error, recargar } = useAsync(() => api.admin.dashboard(), []);
  if (cargando && !d) return <Cargando texto="Cargando el resumen…" />;
  if (error && !d) return <EstadoError mensaje={error} onReintentar={recargar} />;
  const avance = Number(d.avancePromedio ?? 0);
  return (
    <>
      <PageHeader eyebrow="Administración" titulo="Resumen de la plataforma" subtitulo="Estado general de usuarios, cursos e inscripciones." />
      <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Métricas">
        <Metrica icono={Users} valor={fmtNumero(d.estudiantesActivos)} texto="Estudiantes activos" />
        <Metrica icono={GraduationCap} valor={fmtNumero(d.instructoresActivos)} texto="Instructores activos" />
        <Metrica icono={BookOpen} valor={fmtNumero(d.cursosPublicados)} texto={`Cursos publicados (${d.cursosBorrador} en borrador, ${d.cursosArchivados} archivados)`} />
        <Metrica icono={Lock} valor={fmtNumero(d.usuariosBloqueados)} texto="Usuarios bloqueados" />
      </ul>
      <Card className="mt-6 max-w-2xl">
        <h2 className="mb-1 text-xl">Avance promedio en inscripciones activas</h2>
        <p className="mb-3 text-sm">{fmtNumero(d.inscripcionesActivas)} inscripciones activas · {fmtNumero(d.inscripcionesFinalizadas)} finalizadas</p>
        <ProgressBar valor={avance} etiqueta="Avance promedio" />
      </Card>
      <nav aria-label="Accesos rápidos" className="mt-6 flex flex-wrap gap-3">
        <Link className="font-bold text-primary-600 underline" to="/usuarios">Gestionar usuarios</Link>
        <Link className="font-bold text-primary-600 underline" to="/cursos">Ver cursos</Link>
        <Link className="font-bold text-primary-600 underline" to="/auditoria">Consultar auditoría</Link>
      </nav>
    </>
  );
}
