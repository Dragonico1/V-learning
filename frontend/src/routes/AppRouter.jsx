import { lazy, Suspense } from "react";
import { BrowserRouter, Navigate, Outlet, Route, Routes, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { AppShell } from "../components/layout/AppShell.jsx";
import { Cargando } from "../components/ui/index.jsx";

const Login = lazy(() => import("../pages/auth/Login.jsx"));
const Recuperar = lazy(() => import("../pages/auth/Recuperar.jsx"));
const Restablecer = lazy(() => import("../pages/auth/Restablecer.jsx"));
const PrimerAcceso = lazy(() => import("../pages/auth/PrimerAcceso.jsx"));

const VarkTest = lazy(() => import("../pages/onboarding/VarkTest.jsx"));
const VarkResultado = lazy(() => import("../pages/onboarding/VarkResultado.jsx"));
const AccesibilidadOnboarding = lazy(() => import("../pages/onboarding/AccesibilidadOnboarding.jsx"));

const InicioEstudiante = lazy(() => import("../pages/student/Inicio.jsx"));
const CursosEstudiante = lazy(() => import("../pages/student/Cursos.jsx"));
const CursoDetalle = lazy(() => import("../pages/student/CursoDetalle.jsx"));
const Leccion = lazy(() => import("../pages/student/Leccion.jsx"));
const Evaluaciones = lazy(() => import("../pages/student/Evaluaciones.jsx"));
const TomarEvaluacion = lazy(() => import("../pages/student/TomarEvaluacion.jsx"));
const ResultadoEvaluacion = lazy(() => import("../pages/student/ResultadoEvaluacion.jsx"));
const MiProgreso = lazy(() => import("../pages/student/MiProgreso.jsx"));
const Logros = lazy(() => import("../pages/student/Logros.jsx"));

const Comunidad = lazy(() => import("../pages/shared/Comunidad.jsx"));
const Accesibilidad = lazy(() => import("../pages/shared/Accesibilidad.jsx"));
const Configuracion = lazy(() => import("../pages/shared/Configuracion.jsx"));
const Informes = lazy(() => import("../pages/shared/Informes.jsx"));

const InicioInstructor = lazy(() => import("../pages/instructor/Inicio.jsx"));
const CursosInstructor = lazy(() => import("../pages/instructor/Cursos.jsx"));
const EditorCurso = lazy(() => import("../pages/instructor/EditorCurso.jsx"));

const InicioAdmin = lazy(() => import("../pages/admin/Inicio.jsx"));
const Usuarios = lazy(() => import("../pages/admin/Usuarios.jsx"));
const CursosAdmin = lazy(() => import("../pages/admin/Cursos.jsx"));
const Gamificacion = lazy(() => import("../pages/admin/Gamificacion.jsx"));
const Auditoria = lazy(() => import("../pages/admin/Auditoria.jsx"));

/** Elige la pantalla según el rol de la persona. */
function PorRol({ ESTUDIANTE, INSTRUCTOR, ADMINISTRADOR }) {
  const { usuario } = useAuth();
  const Pantalla = { ESTUDIANTE, INSTRUCTOR, ADMINISTRADOR }[usuario.rol];
  return Pantalla ? <Pantalla /> : <Navigate to="/inicio" replace />;
}

function SoloRol({ roles }) {
  const { usuario } = useAuth();
  return roles.includes(usuario.rol) ? <Outlet /> : <Navigate to="/inicio" replace />;
}

/**
 * Guardas: sin sesión → /login; primer acceso → /primer-acceso; estudiante sin VARK → /onboarding/test;
 * sin perfil de accesibilidad → /onboarding/accesibilidad.
 */
function Protegida({ onboarding = false }) {
  const { usuario, cargando } = useAuth();
  const { pathname } = useLocation();
  if (cargando) return <Cargando texto="Cargando tu sesión…" />;
  if (!usuario) return <Navigate to="/login" replace state={{ desde: pathname }} />;
  if (usuario.primerAcceso) return <Navigate to="/primer-acceso" replace />;
  if (usuario.rol === "ESTUDIANTE" && !onboarding) {
    if (!usuario.varkCompletado) return <Navigate to="/onboarding/test" replace />;
    if (!usuario.accesibilidadConfigurada) return <Navigate to="/onboarding/accesibilidad" replace />;
  }
  if (onboarding && usuario.rol !== "ESTUDIANTE") return <Navigate to="/inicio" replace />;
  return <Outlet />;
}

function SoloPrimerAcceso() {
  const { usuario, cargando } = useAuth();
  if (cargando) return <Cargando texto="Cargando tu sesión…" />;
  if (!usuario) return <Navigate to="/login" replace />;
  if (!usuario.primerAcceso) return <Navigate to="/inicio" replace />;
  return <Outlet />;
}

function NoEncontrada() {
  return (
    <div className="py-16 text-center">
      <h1>No encontramos esa página</h1>
      <p className="mt-2 text-lg">Revisa la dirección o vuelve al inicio.</p>
      <a className="mt-4 inline-block font-bold text-primary-600 underline" href="/inicio">Ir al inicio</a>
    </div>
  );
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Suspense fallback={<Cargando />}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/recuperar" element={<Recuperar />} />
          <Route path="/restablecer" element={<Restablecer />} />

          <Route element={<SoloPrimerAcceso />}>
            <Route path="/primer-acceso" element={<PrimerAcceso />} />
          </Route>

          <Route element={<Protegida onboarding />}>
            <Route path="/onboarding/test" element={<VarkTest />} />
            <Route path="/onboarding/resultado" element={<VarkResultado />} />
            <Route path="/onboarding/accesibilidad" element={<AccesibilidadOnboarding />} />
          </Route>

          <Route element={<Protegida />}>
            <Route element={<AppShell />}>
              <Route path="/" element={<Navigate to="/inicio" replace />} />
              <Route path="/inicio" element={<PorRol ESTUDIANTE={InicioEstudiante} INSTRUCTOR={InicioInstructor} ADMINISTRADOR={InicioAdmin} />} />
              <Route path="/cursos" element={<PorRol ESTUDIANTE={CursosEstudiante} INSTRUCTOR={CursosInstructor} ADMINISTRADOR={CursosAdmin} />} />
              <Route path="/contenidos/:id" element={<Leccion />} />
              <Route path="/comunidad" element={<Comunidad />} />
              <Route path="/comunidad/:cursoId" element={<Comunidad />} />
              <Route path="/accesibilidad" element={<Accesibilidad />} />
              <Route path="/configuracion" element={<Configuracion />} />

              <Route element={<SoloRol roles={["ESTUDIANTE"]} />}>
                <Route path="/cursos/:id" element={<CursoDetalle />} />
                <Route path="/evaluaciones" element={<Evaluaciones />} />
                <Route path="/intentos/:id" element={<TomarEvaluacion />} />
                <Route path="/intentos/:id/resultado" element={<ResultadoEvaluacion />} />
                <Route path="/progreso" element={<MiProgreso />} />
                <Route path="/logros" element={<Logros />} />
              </Route>

              <Route element={<SoloRol roles={["INSTRUCTOR"]} />}>
                <Route path="/cursos/:id/editar" element={<EditorCurso />} />
              </Route>

              <Route element={<SoloRol roles={["INSTRUCTOR", "ADMINISTRADOR"]} />}>
                <Route path="/informes" element={<Informes />} />
              </Route>

              <Route element={<SoloRol roles={["ADMINISTRADOR"]} />}>
                <Route path="/usuarios" element={<Usuarios />} />
                <Route path="/gamificacion" element={<Gamificacion />} />
                <Route path="/auditoria" element={<Auditoria />} />
              </Route>

              <Route path="*" element={<NoEncontrada />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
