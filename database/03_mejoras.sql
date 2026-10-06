-- ================================================================
-- V-Learning - Mejoras: reportes de mensajes moderados por el instructor,
-- fecha limite en evaluaciones y destino (enlace) en notificaciones.
-- Para bases YA creadas: ejecuta SOLO este script (idempotente, no borra datos).
-- En instalaciones nuevas, 01_schema.sql ya incluye todo esto.
-- ================================================================
USE vlearning;
GO

-- 1) Evaluaciones: fecha limite (igual que las tareas)
IF COL_LENGTH('dbo.evaluaciones', 'fecha_limite') IS NULL
    ALTER TABLE evaluaciones ADD fecha_limite DATETIME2(6) NULL;
GO

-- 2) Notificaciones: a donde lleva el boton "Ir" (ruta de la aplicacion, p. ej. /logros)
IF COL_LENGTH('dbo.notificaciones', 'enlace') IS NULL
    ALTER TABLE notificaciones ADD enlace VARCHAR(300) NULL;
GO

-- 3) Mensajes: trazabilidad de la moderacion que hace el instructor
IF COL_LENGTH('dbo.mensajes', 'fecha_reporte') IS NULL
    ALTER TABLE mensajes ADD fecha_reporte DATETIME2(6) NULL;
GO
IF COL_LENGTH('dbo.mensajes', 'moderador_id') IS NULL
    ALTER TABLE mensajes ADD moderador_id BIGINT NULL;
GO
IF COL_LENGTH('dbo.mensajes', 'fecha_moderacion') IS NULL
    ALTER TABLE mensajes ADD fecha_moderacion DATETIME2(6) NULL;
GO
IF COL_LENGTH('dbo.mensajes', 'resolucion') IS NULL
    ALTER TABLE mensajes ADD resolucion VARCHAR(30) NULL;
GO
IF OBJECT_ID(N'dbo.fk_mensaje_moderador', N'F') IS NULL
    ALTER TABLE mensajes ADD CONSTRAINT fk_mensaje_moderador FOREIGN KEY (moderador_id)
        REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE NO ACTION;
GO
IF OBJECT_ID(N'dbo.ck_mensajes_resolucion', N'C') IS NULL
    ALTER TABLE mensajes ADD CONSTRAINT ck_mensajes_resolucion
        CHECK (resolucion IS NULL OR resolucion IN ('ELIMINADO', 'MANTENIDO'));
GO

-- 4) Quien reporto cada mensaje (un reporte por persona y mensaje)
IF OBJECT_ID(N'dbo.reportes_mensaje', N'U') IS NULL
BEGIN
    CREATE TABLE reportes_mensaje (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        mensaje_id BIGINT NOT NULL,
        reportante_id BIGINT NOT NULL,
        fecha DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT uq_reporte_mensaje_reportante UNIQUE (mensaje_id, reportante_id),
        CONSTRAINT fk_reporte_mensaje FOREIGN KEY (mensaje_id)
            REFERENCES mensajes(id) ON UPDATE NO ACTION ON DELETE CASCADE,
        CONSTRAINT fk_reporte_reportante FOREIGN KEY (reportante_id)
            REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE NO ACTION
    );
END
GO

-- 5) Indices para las consultas nuevas
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_mensajes_curso_estado' AND object_id = OBJECT_ID('dbo.mensajes'))
    CREATE INDEX idx_mensajes_curso_estado ON mensajes (curso_id, estado, fecha_envio);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_reportes_mensaje' AND object_id = OBJECT_ID('dbo.reportes_mensaje'))
    CREATE INDEX idx_reportes_mensaje ON reportes_mensaje (mensaje_id);
GO
IF OBJECT_ID(N'dbo.entregas_tarea', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_entregas_estudiante' AND object_id = OBJECT_ID('dbo.entregas_tarea'))
    CREATE INDEX idx_entregas_estudiante ON entregas_tarea (estudiante_id);
GO
PRINT 'Mejoras aplicadas correctamente.';
GO
