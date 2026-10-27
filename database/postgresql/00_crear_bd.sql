-- ================================================================
-- V-Learning - Crear rol y base de datos en PostgreSQL (14 o superior)
--
-- Ejecutar UNA vez, como superusuario (postgres), pasando la clave por variable
-- para no dejarla escrita en el repositorio:
--
--   psql -U postgres -v admin_password="TuClaveSegura" -f database/postgresql/00_crear_bd.sql
--
-- vlearning_admin es el DUEÑO de la base de datos (control total sobre vlearning), pero NO es
-- superusuario del servidor: es el equivalente prudente a "sysadmin" de SQL Server.
-- ================================================================

-- Rol (si ya existe, solo se actualiza la clave)
SELECT format('CREATE ROLE vlearning_admin LOGIN PASSWORD %L', :'admin_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'vlearning_admin')
\gexec

SELECT format('ALTER ROLE vlearning_admin LOGIN PASSWORD %L', :'admin_password')
WHERE EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'vlearning_admin')
\gexec

-- Base de datos UTF-8 (tildes y ñ). template0 permite fijar la codificación.
SELECT 'CREATE DATABASE vlearning OWNER vlearning_admin ENCODING ''UTF8'' TEMPLATE template0'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'vlearning')
\gexec
