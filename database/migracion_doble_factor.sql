-- ================================================================
-- Migración: segundo factor opcional para estudiantes
-- Para bases de datos YA creadas (el esquema completo ya incluye la columna).
-- Idempotente: se puede ejecutar más de una vez.
--
-- Regla de negocio:
--   * ADMINISTRADOR e INSTRUCTOR: el OTP siempre es obligatorio (lo fuerza la aplicación).
--   * ESTUDIANTE: puede desactivarlo; por defecto queda activado.
-- Nota: estudiantes.programa_academico ya existía; no requiere cambios.
-- ================================================================
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS doble_factor_habilitado BOOLEAN NOT NULL DEFAULT TRUE;
