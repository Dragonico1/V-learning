-- ================================================================
-- V-Learning - Tareas calificables por módulo.
-- Para bases YA creadas con 01_schema.sql: ejecuta SOLO este script (no borra datos).
-- En instalaciones nuevas, 01_schema.sql ya incluye estas tablas.
-- ================================================================
USE vlearning;
GO

IF OBJECT_ID(N'dbo.tareas', N'U') IS NULL
BEGIN
    CREATE TABLE tareas (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        modulo_id BIGINT NOT NULL,
        titulo VARCHAR(200) NOT NULL,
        descripcion VARCHAR(MAX),
        puntaje_maximo DECIMAL(8,2) NOT NULL,
        fecha_limite DATETIME2(6) NULL,
        fecha_creacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT ck_tarea_puntaje CHECK (puntaje_maximo > 0),
        CONSTRAINT fk_tarea_modulo FOREIGN KEY (modulo_id)
            REFERENCES modulos(id)
            ON UPDATE NO ACTION
            ON DELETE CASCADE
    );
    CREATE INDEX idx_tareas_modulo ON tareas (modulo_id);
END
GO

IF OBJECT_ID(N'dbo.entregas_tarea', N'U') IS NULL
BEGIN
    CREATE TABLE entregas_tarea (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        tarea_id BIGINT NOT NULL,
        estudiante_id BIGINT NOT NULL,
        texto VARCHAR(MAX),
        enlace VARCHAR(500),
        fecha_entrega DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
        puntaje DECIMAL(8,2) NULL,
        retroalimentacion VARCHAR(MAX),
        fecha_calificacion DATETIME2(6) NULL,
        estado VARCHAR(50) NOT NULL DEFAULT 'ENTREGADA',
        CONSTRAINT uq_entrega_tarea_estudiante UNIQUE (tarea_id, estudiante_id),
        CONSTRAINT ck_entrega_puntaje CHECK (puntaje IS NULL OR puntaje >= 0),
        CONSTRAINT ck_entrega_estado CHECK (estado IN ('ENTREGADA', 'CALIFICADA')),
        CONSTRAINT fk_entrega_tarea FOREIGN KEY (tarea_id)
            REFERENCES tareas(id)
            ON UPDATE NO ACTION
            ON DELETE CASCADE,
        CONSTRAINT fk_entrega_estudiante FOREIGN KEY (estudiante_id)
            REFERENCES estudiantes(id)
            ON UPDATE NO ACTION
            ON DELETE NO ACTION
    );
    CREATE INDEX idx_entregas_tarea ON entregas_tarea (tarea_id);
    CREATE INDEX idx_entregas_estudiante ON entregas_tarea (estudiante_id);
END
GO
