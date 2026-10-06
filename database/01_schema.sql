-- ================================================================
-- V-Learning - Esquema relacional para SQL Server / SSMS
-- Basado en el esquema original de V-Learning.
-- Dialecto: Microsoft SQL Server (T-SQL)
--
-- Convención de cambios:
--   Todo lo que NO lleva marca es idéntico al esquema oficial.
--   Toda ampliación está marcada con  -- [EXT]  y justificada con la
--   historia de usuario (HU) / caso de uso (CU) que la exige.
-- ================================================================

USE master;
GO

IF DB_ID(N'vlearning') IS NOT NULL
BEGIN
    ALTER DATABASE vlearning SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE vlearning;
END
GO

CREATE DATABASE vlearning;
GO

USE vlearning;
GO
-- ================================================================
CREATE TABLE usuarios (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    correo_institucional VARCHAR(180) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE_PRIMER_ACCESO',
    fecha_creacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    ultimo_acceso DATETIME2(6) NULL,
    intentos_fallidos INT NOT NULL DEFAULT 0,
    -- [EXT] HU-002 / CU-002: bloqueo temporal de 15 min tras 5 intentos fallidos.
    bloqueado_hasta DATETIME2(6) NULL,
    -- [EXT] HU-013 / CU-011: el usuario puede deshabilitar notificaciones.
    notificaciones_habilitadas BIT NOT NULL DEFAULT 1,
    -- [EXT] HU-013 / CU-011: canal preferido (plataforma, correo).
    canal_preferido VARCHAR(50) NOT NULL DEFAULT 'PLATAFORMA',
    CONSTRAINT uq_usuarios_correo UNIQUE (correo_institucional),
    CONSTRAINT ck_usuarios_intentos CHECK (intentos_fallidos >= 0),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ESTUDIANTE', 'INSTRUCTOR', 'ADMINISTRADOR')),
    CONSTRAINT ck_usuarios_estado CHECK (estado IN ('PENDIENTE_PRIMER_ACCESO', 'ACTIVO', 'BLOQUEADO', 'INACTIVO')),
    CONSTRAINT ck_usuarios_canal_preferido CHECK (canal_preferido IN ('PLATAFORMA', 'CORREO')) -- [EXT]
);
GO
-- ================================================================
CREATE TABLE estudiantes (
    id BIGINT PRIMARY KEY,
    codigo_estudiante VARCHAR(50) NOT NULL,
    programa_academico VARCHAR(150) NOT NULL,
    CONSTRAINT uq_estudiantes_codigo UNIQUE (codigo_estudiante),
    CONSTRAINT fk_estudiante_usuario FOREIGN KEY (id)
        REFERENCES usuarios(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
GO
-- ================================================================
CREATE TABLE instructores (
    id BIGINT PRIMARY KEY,
    codigo_instructor VARCHAR(50) NOT NULL,
    especialidad VARCHAR(150),
    CONSTRAINT uq_instructores_codigo UNIQUE (codigo_instructor),
    CONSTRAINT fk_instructor_usuario FOREIGN KEY (id)
        REFERENCES usuarios(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
GO
-- ================================================================
CREATE TABLE administradores (
    id BIGINT PRIMARY KEY,
    CONSTRAINT fk_administrador_usuario FOREIGN KEY (id)
        REFERENCES usuarios(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
GO
-- ================================================================
CREATE TABLE cursos (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    instructor_id BIGINT NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    descripcion VARCHAR(MAX),
    estado VARCHAR(50) NOT NULL DEFAULT 'BORRADOR',
    fecha_creacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    fecha_actualizacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME()
        ,
    CONSTRAINT fk_curso_instructor FOREIGN KEY (instructor_id)
        REFERENCES instructores(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT ck_cursos_estado CHECK (estado IN ('BORRADOR', 'PUBLICADO', 'ARCHIVADO'))
);
GO
-- ================================================================
CREATE TABLE modulos (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    curso_id BIGINT NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    descripcion VARCHAR(MAX),
    orden INT NOT NULL,
    CONSTRAINT uq_modulo_orden UNIQUE (curso_id, orden),
    CONSTRAINT ck_modulo_orden CHECK (orden > 0),
    CONSTRAINT fk_modulo_curso FOREIGN KEY (curso_id)
        REFERENCES cursos(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);
GO
-- ================================================================
CREATE TABLE contenidos (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    modulo_id BIGINT NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    descripcion VARCHAR(MAX),
    duracion_minutos INT,
    publicado BIT NOT NULL DEFAULT 0,
    fecha_publicacion DATETIME2(6) NULL,
    formato VARCHAR(50) NOT NULL,
    -- [EXT] HU-006 / CU-005: enlace al video o audio del contenido.
    url_recurso VARCHAR(500) NULL,
    -- [EXT] HU-006 / HU-023: texto estructurado por defecto; incluye la sección
    --       "Glosario" (líneas "Término: definición") usada por el apoyo cognitivo.
    cuerpo VARCHAR(MAX) NULL,
    CONSTRAINT ck_contenido_duracion CHECK (duracion_minutos IS NULL OR duracion_minutos >= 0),
    CONSTRAINT fk_contenido_modulo FOREIGN KEY (modulo_id)
        REFERENCES modulos(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT ck_contenidos_formato CHECK (formato IN ('VIDEO', 'PODCAST', 'SIMULACION', 'LECTURA'))
);
GO
-- ================================================================
CREATE TABLE inscripciones (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    estudiante_id BIGINT NOT NULL,
    curso_id BIGINT NOT NULL,
    fecha_inscripcion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    estado VARCHAR(50) NOT NULL DEFAULT 'ACTIVA',
    porcentaje_completado DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT uq_inscripcion_estudiante_curso UNIQUE (estudiante_id, curso_id),
    CONSTRAINT ck_inscripcion_porcentaje CHECK (porcentaje_completado BETWEEN 0 AND 100),
    CONSTRAINT fk_inscripcion_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_inscripcion_curso FOREIGN KEY (curso_id)
        REFERENCES cursos(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT ck_inscripciones_estado CHECK (estado IN ('ACTIVA', 'CANCELADA', 'FINALIZADA'))
);
GO
-- ================================================================
CREATE TABLE progresos (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    estudiante_id BIGINT NOT NULL,
    contenido_id BIGINT NOT NULL,
    porcentaje DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    tiempo_consumido BIGINT NOT NULL DEFAULT 0,
    estado VARCHAR(50) NOT NULL DEFAULT 'NO_INICIADO',
    ultima_actividad DATETIME2(6) NULL,
    CONSTRAINT uq_progreso_estudiante_contenido UNIQUE (estudiante_id, contenido_id),
    CONSTRAINT ck_progreso_porcentaje CHECK (porcentaje BETWEEN 0 AND 100),
    CONSTRAINT ck_progreso_tiempo CHECK (tiempo_consumido >= 0),
    CONSTRAINT fk_progreso_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_progreso_contenido FOREIGN KEY (contenido_id)
        REFERENCES contenidos(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT ck_progresos_estado CHECK (estado IN ('NO_INICIADO', 'EN_PROGRESO', 'COMPLETADO'))
);
GO
-- ================================================================
CREATE TABLE evaluaciones (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    modulo_id BIGINT NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    descripcion VARCHAR(MAX),
    tipo VARCHAR(50) NOT NULL,
    puntaje_maximo DECIMAL(8,2) NOT NULL,
    tiempo_limite INT NULL,
    -- [EXT] RF-021 / HU-022: 0 = la actividad (p. ej. arrastrar y soltar) aún no tiene
    --       alternativa accesible; se excluye de la calificación para perfil motor.
    alternativa_accesible BIT NOT NULL DEFAULT 1,
    CONSTRAINT ck_evaluacion_puntaje CHECK (puntaje_maximo >= 0),
    CONSTRAINT ck_evaluacion_tiempo CHECK (tiempo_limite IS NULL OR tiempo_limite > 0),
    CONSTRAINT fk_evaluacion_modulo FOREIGN KEY (modulo_id)
        REFERENCES modulos(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT ck_evaluaciones_tipo CHECK (tipo IN ('QUIZ', 'SIMULACION', 'PRACTICA'))
);
GO
-- ================================================================
CREATE TABLE preguntas (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    evaluacion_id BIGINT NOT NULL,
    enunciado VARCHAR(MAX) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    puntaje DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    orden INT NOT NULL,
    CONSTRAINT uq_pregunta_orden UNIQUE (evaluacion_id, orden),
    CONSTRAINT ck_pregunta_puntaje CHECK (puntaje >= 0),
    CONSTRAINT ck_pregunta_orden CHECK (orden > 0),
    CONSTRAINT fk_pregunta_evaluacion FOREIGN KEY (evaluacion_id)
        REFERENCES evaluaciones(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT ck_preguntas_tipo CHECK (tipo IN ('SELECCION_UNICA', 'SELECCION_MULTIPLE', 'VERDADERO_FALSO', 'ABIERTA'))
);
GO
-- ================================================================
CREATE TABLE opciones_respuesta (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    pregunta_id BIGINT NOT NULL,
    texto VARCHAR(MAX) NOT NULL,
    correcta BIT NOT NULL DEFAULT 0,
    CONSTRAINT fk_opcion_pregunta FOREIGN KEY (pregunta_id)
        REFERENCES preguntas(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);
GO
-- ================================================================
CREATE TABLE intentos_evaluacion (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    evaluacion_id BIGINT NOT NULL,
    estudiante_id BIGINT NOT NULL,
    fecha_inicio DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    fecha_finalizacion DATETIME2(6) NULL,
    puntaje DECIMAL(8,2) NULL,
    estado VARCHAR(50) NOT NULL DEFAULT 'EN_PROGRESO',
    CONSTRAINT ck_intento_puntaje CHECK (puntaje IS NULL OR puntaje >= 0),
    CONSTRAINT fk_intento_evaluacion FOREIGN KEY (evaluacion_id)
        REFERENCES evaluaciones(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_intento_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT ck_intentos_evaluacion_estado CHECK (estado IN ('EN_PROGRESO', 'FINALIZADO', 'ABANDONADO'))
);
GO
-- ================================================================
CREATE TABLE respuestas (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    intento_id BIGINT NOT NULL,
    pregunta_id BIGINT NOT NULL,
    valor VARCHAR(MAX),
    es_correcta BIT NULL,
    puntaje_obtenido DECIMAL(8,2) NULL,
    CONSTRAINT uq_respuesta_intento_pregunta UNIQUE (intento_id, pregunta_id),
    CONSTRAINT ck_respuesta_puntaje CHECK (puntaje_obtenido IS NULL OR puntaje_obtenido >= 0),
    CONSTRAINT fk_respuesta_intento FOREIGN KEY (intento_id)
        REFERENCES intentos_evaluacion(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT fk_respuesta_pregunta FOREIGN KEY (pregunta_id)
        REFERENCES preguntas(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
GO
-- ================================================================
CREATE TABLE perfiles_aprendizaje (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    estudiante_id BIGINT NOT NULL,
    estilo_predominante VARCHAR(50) NULL,
    fecha_evaluacion DATETIME2(6) NULL,
    version_test VARCHAR(50),
    -- [EXT] Onboarding: segundo método opcional (máximo uno, distinto del principal).
    metodo_secundario VARCHAR(50) NULL,
    CONSTRAINT uq_perfil_aprendizaje_estudiante UNIQUE (estudiante_id),
    CONSTRAINT fk_perfil_aprendizaje_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT ck_perfiles_aprendizaje_estilo_predominante CHECK (estilo_predominante IN ('VISUAL', 'AUDITIVO', 'LECTURA_ESCRITURA', 'KINESTESICO')),
    CONSTRAINT ck_perfiles_aprendizaje_metodo_secundario CHECK (metodo_secundario IN ('VISUAL', 'AUDITIVO', 'LECTURA_ESCRITURA', 'KINESTESICO')), -- [EXT]
    CONSTRAINT ck_perfiles_aprendizaje_secundario_distinto CHECK (metodo_secundario IS NULL OR metodo_secundario <> estilo_predominante) -- [EXT]
);
GO
-- ================================================================
CREATE TABLE resultados_vark (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    perfil_aprendizaje_id BIGINT NOT NULL,
    visual INT NOT NULL DEFAULT 0,
    auditivo INT NOT NULL DEFAULT 0,
    lectura_escritura INT NOT NULL DEFAULT 0,
    kinestesico INT NOT NULL DEFAULT 0,
    fecha_realizacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT ck_vark_visual CHECK (visual >= 0),
    CONSTRAINT ck_vark_auditivo CHECK (auditivo >= 0),
    CONSTRAINT ck_vark_lectura CHECK (lectura_escritura >= 0),
    CONSTRAINT ck_vark_kinestesico CHECK (kinestesico >= 0),
    CONSTRAINT fk_resultado_vark_perfil FOREIGN KEY (perfil_aprendizaje_id)
        REFERENCES perfiles_aprendizaje(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);
GO
-- ================================================================
-- [EXT] HU-005 / CU-004 (excepción 1): guardado parcial del cuestionario VARK.
--       Una fila por pregunta respondida; se borra al enviar el cuestionario.
CREATE TABLE respuestas_vark_parciales (
    perfil_aprendizaje_id BIGINT NOT NULL,
    numero_pregunta INT NOT NULL,
    opcion VARCHAR(50) NOT NULL,
    fecha_actualizacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    PRIMARY KEY (perfil_aprendizaje_id, numero_pregunta),
    CONSTRAINT ck_vark_parcial_numero CHECK (numero_pregunta > 0),
    CONSTRAINT ck_vark_parcial_opcion CHECK (opcion IN ('VISUAL', 'AUDITIVO', 'LECTURA_ESCRITURA', 'KINESTESICO')),
    CONSTRAINT fk_vark_parcial_perfil FOREIGN KEY (perfil_aprendizaje_id)
        REFERENCES perfiles_aprendizaje(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);
GO
-- ================================================================
CREATE TABLE perfiles_accesibilidad (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    estudiante_id BIGINT NOT NULL,
    fecha_actualizacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME()
        ,
    CONSTRAINT uq_perfil_accesibilidad_estudiante UNIQUE (estudiante_id),
    CONSTRAINT fk_perfil_accesibilidad_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
GO
-- ================================================================
CREATE TABLE perfil_accesibilidad_categoria (
    perfil_accesibilidad_id BIGINT NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    PRIMARY KEY (perfil_accesibilidad_id, categoria),
    CONSTRAINT fk_categoria_perfil_accesibilidad FOREIGN KEY (perfil_accesibilidad_id)
        REFERENCES perfiles_accesibilidad(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT ck_perfil_accesibilidad_categoria_categoria CHECK (categoria IN ('VISUAL', 'AUDITIVA', 'MOTORA', 'COGNITIVA'))
);
GO
-- ================================================================
CREATE TABLE configuraciones_accesibilidad (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    perfil_accesibilidad_id BIGINT NOT NULL,
    alto_contraste BIT NOT NULL DEFAULT 0,
    tamano_fuente INT NOT NULL DEFAULT 16,
    tipografia VARCHAR(100),
    espaciado_linea DECIMAL(5,2),
    navegacion_teclado BIT NOT NULL DEFAULT 0,
    lector_pantalla BIT NOT NULL DEFAULT 0,
    subtitulos BIT NOT NULL DEFAULT 0,
    transcripcion BIT NOT NULL DEFAULT 0,
    texto_a_voz BIT NOT NULL DEFAULT 0,
    tiempo_adicional INT NOT NULL DEFAULT 0,
    fecha_configuracion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT ck_config_fuente CHECK (tamano_fuente > 0),
    CONSTRAINT ck_config_espaciado CHECK (espaciado_linea IS NULL OR espaciado_linea > 0),
    CONSTRAINT ck_config_tiempo CHECK (tiempo_adicional >= 0),
    CONSTRAINT fk_config_accesibilidad_perfil FOREIGN KEY (perfil_accesibilidad_id)
        REFERENCES perfiles_accesibilidad(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);
GO
-- ================================================================
CREATE TABLE recursos_accesibles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    contenido_id BIGINT NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    url VARCHAR(500) NOT NULL,
    descripcion VARCHAR(MAX),
    disponible BIT NOT NULL DEFAULT 1,
    CONSTRAINT fk_recurso_accesible_contenido FOREIGN KEY (contenido_id)
        REFERENCES contenidos(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT ck_recursos_accesibles_tipo CHECK (tipo IN ('TEXTO_ALTERNATIVO', 'SUBTITULO', 'TRANSCRIPCION', 'AUDIO', 'LENGUA_SENAS', 'VERSION_SIMPLIFICADA'))
);
GO
-- ================================================================
CREATE TABLE tutores_ia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    modelo VARCHAR(150) NOT NULL,
    activo BIT NOT NULL DEFAULT 1
);
GO
-- ================================================================
CREATE TABLE consultas_tutor (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    estudiante_id BIGINT NOT NULL,
    tutor_id BIGINT NOT NULL,
    contenido_id BIGINT NULL,
    pregunta VARCHAR(MAX) NOT NULL,
    fecha DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_consulta_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_consulta_tutor FOREIGN KEY (tutor_id)
        REFERENCES tutores_ia(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_consulta_contenido FOREIGN KEY (contenido_id)
        REFERENCES contenidos(id)
        ON UPDATE NO ACTION
        ON DELETE SET NULL
);
GO
-- ================================================================
CREATE TABLE respuestas_tutor (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    consulta_id BIGINT NOT NULL,
    contenido VARCHAR(MAX) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    fecha DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    util BIT NOT NULL DEFAULT 0,
    CONSTRAINT uq_respuesta_tutor_consulta UNIQUE (consulta_id),
    CONSTRAINT fk_respuesta_tutor_consulta FOREIGN KEY (consulta_id)
        REFERENCES consultas_tutor(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT ck_respuestas_tutor_tipo CHECK (tipo IN ('EXPLICACION', 'EJEMPLO', 'ACTIVIDAD', 'RESUMEN'))
);
GO
-- ================================================================
CREATE TABLE perfiles_gamificacion (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    estudiante_id BIGINT NOT NULL,
    puntos INT NOT NULL DEFAULT 0,
    nivel INT NOT NULL DEFAULT 1,
    -- [EXT] RF-009: modo privado; 0 = el estudiante no aparece en la tabla pública.
    visible_ranking BIT NOT NULL DEFAULT 1,
    CONSTRAINT uq_perfil_gamificacion_estudiante UNIQUE (estudiante_id),
    CONSTRAINT ck_gamificacion_puntos CHECK (puntos >= 0),
    CONSTRAINT ck_gamificacion_nivel CHECK (nivel >= 1),
    CONSTRAINT fk_perfil_gamificacion_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
GO
-- ================================================================
CREATE TABLE insignias (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(MAX),
    puntos_requeridos INT NOT NULL,
    CONSTRAINT ck_insignia_puntos CHECK (puntos_requeridos >= 0),
    CONSTRAINT uq_insignia_nombre UNIQUE (nombre)
);
GO
-- ================================================================
CREATE TABLE insignias_obtenidas (
    perfil_gamificacion_id BIGINT NOT NULL,
    insignia_id BIGINT NOT NULL,
    fecha_obtencion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    PRIMARY KEY (perfil_gamificacion_id, insignia_id),
    CONSTRAINT fk_insignia_obtenida_perfil FOREIGN KEY (perfil_gamificacion_id)
        REFERENCES perfiles_gamificacion(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT fk_insignia_obtenida_insignia FOREIGN KEY (insignia_id)
        REFERENCES insignias(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
GO
-- ================================================================
CREATE TABLE rankings (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    curso_id BIGINT NOT NULL,
    periodo VARCHAR(50) NOT NULL,
    fecha_actualizacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT uq_ranking_curso_periodo UNIQUE (curso_id, periodo),
    CONSTRAINT fk_ranking_curso FOREIGN KEY (curso_id)
        REFERENCES cursos(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);
GO
-- ================================================================
CREATE TABLE posiciones_ranking (
    ranking_id BIGINT NOT NULL,
    estudiante_id BIGINT NOT NULL,
    posicion INT NOT NULL,
    puntos INT NOT NULL DEFAULT 0,
    PRIMARY KEY (ranking_id, estudiante_id),
    CONSTRAINT uq_ranking_posicion UNIQUE (ranking_id, posicion),
    CONSTRAINT ck_posicion_ranking CHECK (posicion > 0),
    CONSTRAINT ck_puntos_ranking CHECK (puntos >= 0),
    CONSTRAINT fk_posicion_ranking FOREIGN KEY (ranking_id)
        REFERENCES rankings(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT fk_posicion_estudiante FOREIGN KEY (estudiante_id)
        REFERENCES estudiantes(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
GO
-- ================================================================
CREATE TABLE reglas_gamificacion (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(MAX),
    evento VARCHAR(100) NOT NULL,
    puntos INT NOT NULL,
    activo BIT NOT NULL DEFAULT 1,
    administrador_id BIGINT NULL,
    fecha_creacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    fecha_actualizacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME()
        ,
    CONSTRAINT ck_regla_puntos CHECK (puntos >= 0),
    CONSTRAINT fk_regla_administrador FOREIGN KEY (administrador_id)
        REFERENCES administradores(id)
        ON UPDATE NO ACTION
        ON DELETE SET NULL
);
GO
-- ================================================================
CREATE TABLE mensajes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    curso_id BIGINT NOT NULL,
    contenido VARCHAR(MAX) NOT NULL,
    fecha_envio DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    estado VARCHAR(50) NOT NULL,
    -- [EXT] RF-010: estados válidos; REPORTADO = reporte de contenido inapropiado.
    CONSTRAINT ck_mensajes_estado CHECK (estado IN ('ENVIADO', 'REPORTADO', 'BLOQUEADO', 'OCULTO')),
    CONSTRAINT fk_mensaje_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_mensaje_curso FOREIGN KEY (curso_id)
        REFERENCES cursos(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);
GO
-- ================================================================
CREATE TABLE notificaciones (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    mensaje VARCHAR(MAX) NOT NULL,
    fecha_creacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    fecha_lectura DATETIME2(6) NULL,
    canal VARCHAR(50) NOT NULL,
    leida BIT NOT NULL DEFAULT 0,
    -- [EXT] RF-011: "Posponer el recordatorio"; no se muestra hasta esta fecha.
    pospuesta_hasta DATETIME2(6) NULL,
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT ck_notificaciones_canal CHECK (canal IN ('PLATAFORMA', 'CORREO'))
);
GO
-- ================================================================
CREATE TABLE informes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    fecha_generacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    formato VARCHAR(50) NOT NULL,
    filtros VARCHAR(MAX),
    CONSTRAINT fk_informe_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT ck_informes_formato CHECK (formato IN ('PDF', 'EXCEL'))
);
GO
-- ================================================================
-- NOTA: la aplicación guarda en token_jwt el HASH SHA-256 (hex) del token,
-- nunca el token en claro. El filtro de seguridad desliza fecha_expiracion
-- (inactividad de 30 min) en cada solicitud válida.
CREATE TABLE sesiones (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_jwt VARCHAR(MAX) NOT NULL,
    fecha_inicio DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    fecha_expiracion DATETIME2(6) NOT NULL,
    activa BIT NOT NULL DEFAULT 1,
    ip_origen VARCHAR(45),
    CONSTRAINT fk_sesion_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
GO
-- ================================================================
-- NOTA (HU-015 / CU-014): registros_auditoria es de SOLO INSERCIÓN desde
-- la aplicación. No se usa un trigger INSTEAD OF UPDATE/DELETE porque
-- choca con la FK ON DELETE SET NULL hacia usuarios. En su lugar, al
-- final de este script se documenta el REVOKE para el usuario de la app.
CREATE TABLE registros_auditoria (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id BIGINT NULL,
    accion VARCHAR(150) NOT NULL,
    fecha DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    ip_origen VARCHAR(45),
    resultado VARCHAR(50) NOT NULL,
    recurso VARCHAR(200),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON UPDATE NO ACTION
        ON DELETE SET NULL,
    CONSTRAINT ck_registros_auditoria_resultado CHECK (resultado IN ('PERMITIDO', 'DENEGADO', 'ERROR'))
);
GO
-- ================================================================
CREATE TABLE tokens_recuperacion_password (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    fecha_creacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    fecha_expiracion DATETIME2(6) NOT NULL,
    usado BIT NOT NULL DEFAULT 0,
    ip_origen VARCHAR(45),
    CONSTRAINT uq_token_recuperacion_hash UNIQUE (token_hash),
    CONSTRAINT fk_token_recuperacion_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);
GO
-- ================================================================
-- [EXT] HU-002 / CU-002: segundo factor (OTP de 6 dígitos, válido 5 min).
--       El código se guarda hasheado, nunca en claro.
CREATE TABLE codigos_otp (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    codigo_hash VARCHAR(255) NOT NULL,
    fecha_creacion DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    fecha_expiracion DATETIME2(6) NOT NULL,
    usado BIT NOT NULL DEFAULT 0,
    ip_origen VARCHAR(45),
    CONSTRAINT fk_otp_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);
GO
-- ================================================================
-- Tareas calificables por módulo (idéntico a 02_tareas.sql)
-- ================================================================

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
-- ================================================================
-- ÍNDICES SECUNDARIOS
-- ================================================================
CREATE INDEX idx_cursos_instructor ON cursos (instructor_id);
GO
CREATE INDEX idx_cursos_estado ON cursos (estado);
GO
CREATE INDEX idx_modulos_curso ON modulos (curso_id);
GO
CREATE INDEX idx_contenidos_modulo ON contenidos (modulo_id);
GO
CREATE INDEX idx_contenidos_formato ON contenidos (formato);
GO
CREATE INDEX idx_contenidos_publicado ON contenidos (publicado);
GO
CREATE INDEX idx_inscripciones_estudiante ON inscripciones (estudiante_id);
GO
CREATE INDEX idx_inscripciones_curso ON inscripciones (curso_id);
GO
CREATE INDEX idx_inscripciones_estado ON inscripciones (estado);
GO
CREATE INDEX idx_progresos_estudiante ON progresos (estudiante_id);
GO
CREATE INDEX idx_progresos_contenido ON progresos (contenido_id);
GO
CREATE INDEX idx_progresos_estado ON progresos (estado);
GO
CREATE INDEX idx_evaluaciones_modulo ON evaluaciones (modulo_id);
GO
CREATE INDEX idx_preguntas_evaluacion ON preguntas (evaluacion_id);
GO
CREATE INDEX idx_opciones_pregunta ON opciones_respuesta (pregunta_id);
GO
CREATE INDEX idx_intentos_evaluacion ON intentos_evaluacion (evaluacion_id);
GO
CREATE INDEX idx_intentos_estudiante ON intentos_evaluacion (estudiante_id);
GO
CREATE INDEX idx_intentos_estado ON intentos_evaluacion (estado);
GO
CREATE INDEX idx_respuestas_intento ON respuestas (intento_id);
GO
CREATE INDEX idx_respuestas_pregunta ON respuestas (pregunta_id);
GO
CREATE INDEX idx_resultados_vark_perfil ON resultados_vark (perfil_aprendizaje_id);
GO
CREATE INDEX idx_config_accesibilidad_perfil ON configuraciones_accesibilidad (perfil_accesibilidad_id);
GO
CREATE INDEX idx_config_accesibilidad_fecha ON configuraciones_accesibilidad (fecha_configuracion);
GO
CREATE INDEX idx_recursos_accesibles_contenido ON recursos_accesibles (contenido_id);
GO
CREATE INDEX idx_recursos_accesibles_tipo ON recursos_accesibles (tipo);
GO
CREATE INDEX idx_consultas_estudiante ON consultas_tutor (estudiante_id);
GO
CREATE INDEX idx_consultas_tutor ON consultas_tutor (tutor_id);
GO
CREATE INDEX idx_consultas_contenido ON consultas_tutor (contenido_id);
GO
CREATE INDEX idx_consultas_fecha ON consultas_tutor (fecha);
GO
CREATE INDEX idx_rankings_curso ON rankings (curso_id);
GO
CREATE INDEX idx_posiciones_estudiante ON posiciones_ranking (estudiante_id);
GO
CREATE INDEX idx_reglas_evento ON reglas_gamificacion (evento);
GO
CREATE INDEX idx_reglas_activo ON reglas_gamificacion (activo);
GO
CREATE INDEX idx_mensajes_usuario ON mensajes (usuario_id);
GO
CREATE INDEX idx_mensajes_curso_fecha ON mensajes (curso_id, fecha_envio);
GO
CREATE INDEX idx_notificaciones_usuario ON notificaciones (usuario_id);
GO
CREATE INDEX idx_notificaciones_leida ON notificaciones (usuario_id, leida);
GO
CREATE INDEX idx_notificaciones_fecha ON notificaciones (fecha_creacion);
GO
CREATE INDEX idx_informes_usuario ON informes (usuario_id);
GO
CREATE INDEX idx_informes_fecha ON informes (fecha_generacion);
GO
CREATE INDEX idx_sesiones_usuario ON sesiones (usuario_id);
GO
CREATE INDEX idx_sesiones_activa ON sesiones (usuario_id, activa);
GO
CREATE INDEX idx_sesiones_expiracion ON sesiones (fecha_expiracion);
GO
CREATE INDEX idx_auditoria_usuario_fecha ON registros_auditoria (usuario_id, fecha);
GO
CREATE INDEX idx_auditoria_resultado ON registros_auditoria (resultado);
GO
CREATE INDEX idx_auditoria_recurso ON registros_auditoria (recurso);
GO
CREATE INDEX idx_token_recuperacion_usuario ON tokens_recuperacion_password (usuario_id);
GO
CREATE INDEX idx_token_recuperacion_expiracion ON tokens_recuperacion_password (fecha_expiracion);
GO
CREATE INDEX idx_token_recuperacion_estado ON tokens_recuperacion_password (usado);
GO
CREATE INDEX idx_otp_usuario ON codigos_otp (usuario_id, usado); -- [EXT]
GO
CREATE INDEX idx_otp_expiracion ON codigos_otp (fecha_expiracion); -- [EXT]
GO
-- Actualiza automáticamente cursos.fecha_actualizacion cuando se modifica la fila.
CREATE TRIGGER trg_cursos_fecha_actualizacion_update
ON cursos
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE t
    SET fecha_actualizacion = SYSDATETIME()
    FROM cursos AS t
    INNER JOIN inserted AS i ON t.id = i.id;
END
GO

-- Actualiza automáticamente perfiles_accesibilidad.fecha_actualizacion cuando se modifica la fila.
CREATE TRIGGER trg_perfiles_accesibilidad_fecha_actualizacion_update
ON perfiles_accesibilidad
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE t
    SET fecha_actualizacion = SYSDATETIME()
    FROM perfiles_accesibilidad AS t
    INNER JOIN inserted AS i ON t.id = i.id;
END
GO

-- Actualiza automáticamente reglas_gamificacion.fecha_actualizacion cuando se modifica la fila.
CREATE TRIGGER trg_reglas_gamificacion_fecha_actualizacion_update
ON reglas_gamificacion
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE t
    SET fecha_actualizacion = SYSDATETIME()
    FROM reglas_gamificacion AS t
    INNER JOIN inserted AS i ON t.id = i.id;
END
GO

-- ================================================================
-- [EXT] HU-015 / CU-014: auditoría inmutable (solo inserción).
-- Ejecutar DESPUÉS de crear el usuario de la aplicación en SQL Server.
-- Ejemplo (ajusta el nombre del usuario a tu entorno):
--
--   CREATE LOGIN vlearning_app WITH PASSWORD = '<contraseña-segura>';
--   CREATE USER  vlearning_app FOR LOGIN vlearning_app;
--   ALTER ROLE db_datareader ADD MEMBER vlearning_app;
--   ALTER ROLE db_datawriter ADD MEMBER vlearning_app;
--   DENY UPDATE, DELETE ON dbo.registros_auditoria TO vlearning_app;
--
-- (DENY tiene prioridad sobre el rol db_datawriter; REVOKE solo quita un
--  permiso concedido explícitamente y no lo bloquearía.)
-- ================================================================

-- ================================================================
-- [EXT] RF-013 / HU-015: cifrado en reposo y respaldo cifrado (documentación).
-- Las contraseñas y los códigos se guardan con hash desde la aplicación.
-- Para cifrar el resto de los datos en reposo, activa TDE en SQL Server
-- (Enterprise / Developer / Standard 2019+). Ejecutar con permisos de sysadmin:
--
--   USE master;
--   CREATE MASTER KEY ENCRYPTION BY PASSWORD = '<contraseña-fuerte>';
--   CREATE CERTIFICATE vlearning_tde_cert WITH SUBJECT = 'V-Learning TDE';
--   USE vlearning;
--   CREATE DATABASE ENCRYPTION KEY WITH ALGORITHM = AES_256
--       ENCRYPTION BY SERVER CERTIFICATE vlearning_tde_cert;
--   ALTER DATABASE vlearning SET ENCRYPTION ON;
--
-- Respaldo cifrado periódico (programar con SQL Server Agent):
--
--   BACKUP DATABASE vlearning TO DISK = N'D:\respaldos\vlearning.bak'
--   WITH COMPRESSION, ENCRYPTION (ALGORITHM = AES_256,
--        SERVER CERTIFICATE = vlearning_tde_cert);
--
-- Guarda una copia del certificado y de su clave privada fuera del servidor.
-- ================================================================

-- ================================================================
-- FIN DEL ESQUEMA
-- ================================================================
