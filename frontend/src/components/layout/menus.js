import {
  BookOpen,
  FileText,
  Gamepad2,
  House,
  ScrollText,
  Settings,
  Trophy,
  TrendingUp,
  Users,
  UserPlus,
  MessageSquare,
} from "lucide-react";

/** Menú lateral por rol (sección 4.1). Configuración y Cerrar sesión siempre al final. */
export const MENU = {
  ESTUDIANTE: [
    { a: "/inicio", t: "Inicio", i: House },
    { a: "/cursos", t: "Mis cursos", i: BookOpen },
    { a: "/progreso", t: "Mi progreso", i: TrendingUp },
    { a: "/logros", t: "Logros", i: Trophy },
    { a: "/comunidad", t: "Comunidad", i: Users },
  ],
  INSTRUCTOR: [
    { a: "/inicio", t: "Inicio", i: House },
    { a: "/cursos", t: "Mis cursos", i: BookOpen },
    { a: "/informes", t: "Informes", i: FileText },
    { a: "/comunidad", t: "Comunidad", i: MessageSquare },
  ],
  ADMINISTRADOR: [
    { a: "/inicio", t: "Inicio", i: House },
    { a: "/usuarios", t: "Usuarios", i: UserPlus },
    { a: "/cursos", t: "Cursos", i: BookOpen },
    { a: "/gamificacion", t: "Gamificación", i: Gamepad2 },
    { a: "/informes", t: "Informes", i: FileText },
    { a: "/auditoria", t: "Auditoría", i: ScrollText },
    { a: "/comunidad", t: "Comunidad", i: Users },
  ],
};
export const ITEM_CONFIG = {
  a: "/configuracion",
  t: "Configuración",
  i: Settings,
};

/** Nombres para las migas de pan. */
export const TITULOS = {
  inicio: "Inicio",
  cursos: "Cursos",
  contenidos: "Lección",
  evaluaciones: "Evaluaciones",
  intentos: "Evaluación",
  progreso: "Mi progreso",
  logros: "Logros",
  comunidad: "Comunidad",
  accesibilidad: "Accesibilidad",
  configuracion: "Configuración",
  informes: "Informes",
  usuarios: "Usuarios",
  gamificacion: "Gamificación",
  auditoria: "Auditoría",
  editar: "Editor",
  resultado: "Resultado",
};
