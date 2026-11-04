package com.elearning.platform.config;

import com.elearning.platform.dto.ContenidoDtos.ProgresoRequest;
import com.elearning.platform.entity.*;
import com.elearning.platform.enums.*;
import com.elearning.platform.repository.*;
import com.elearning.platform.services.ProgresoService;
import com.elearning.platform.util.EventosGamificacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos iniciales.
 * - Siempre (si las tablas están vacías): reglas de puntos, insignias y tutor de IA por defecto.
 * - Solo con vlearning.dev.sembrar-datos=true: cuentas y curso de DEMOSTRACIÓN (si no hay usuarios)
 *   y cinco cursos de ejemplo con módulos, contenidos y evaluaciones (si no existen).
 *   Las credenciales demo son SOLO para desarrollo: apaga la bandera en producción.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    static final String PASSWORD_DEMO = "Vlearning#2026";
    static final String PASSWORD_TEMPORAL_DEMO = "Temporal#2026";

    private final VlearningProperties props;
    private final OllamaProperties ollama;
    private final TransactionTemplate tx;
    private final PasswordEncoder encoder;
    private final UsuarioRepository usuarios;
    private final CursoRepository cursos;
    private final ModuloRepository modulos;
    private final ContenidoRepository contenidos;
    private final RecursoAccesibleRepository recursos;
    private final EvaluacionRepository evaluaciones;
    private final PreguntaRepository preguntas;
    private final OpcionRespuestaRepository opciones;
    private final InscripcionRepository inscripciones;
    private final ReglaGamificacionRepository reglas;
    private final InsigniaRepository insignias;
    private final TutorIARepository tutores;
    private final CursosDemoSeeder cursosDemo;
    private final ProgresoRepository progresos;
    private final ProgresoService progresoService;
    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        if (props.dev().exponerSecretos()) {
            log.warn("MODO DESARROLLO: la API devuelve OTP, contraseñas temporales y enlaces de recuperación. "
                    + "Define VLEARNING_DEV_EXPONER_SECRETOS=false en producción.");
        }
        tx.executeWithoutResult(s -> sembrarBase());
        if (props.dev().sembrarDatos()) {
            if (usuarios.count() == 0) {
                tx.executeWithoutResult(s -> sembrarDemo());
                log.warn("Se crearon datos de DEMOSTRACIÓN (usuarios @tdea.edu.co). No uses estas credenciales en producción.");
            }
            tx.executeWithoutResult(s -> sembrarEstudiantesAdicionales());
            sembrarAvanceSofia();
            tx.executeWithoutResult(s -> reiniciarAvanceDemo());
            // Cursos de ejemplo: se crean una sola vez y se asignan al primer instructor.
            tx.executeWithoutResult(s -> cursosDemo.sembrar());
        }
    }

    private void sembrarBase() {
        if (reglas.count() == 0) {
            regla("Contenido completado", "Puntos por completar un contenido.", EventosGamificacion.CONTENIDO_COMPLETADO, 50);
            regla("Evaluación completada", "Puntos por mejorar tu mejor resultado en una evaluación.", EventosGamificacion.EVALUACION_COMPLETADA, 100);
            regla("Test VARK completado", "Puntos por descubrir tu estilo de aprendizaje.", EventosGamificacion.TEST_VARK, 30);
            regla("Curso finalizado", "Reconocimiento por terminar un curso.", EventosGamificacion.CURSO_FINALIZADO, 0);
        }
        if (insignias.count() == 0) {
            insignia("Primeros pasos", "Alcanzaste tus primeros 50 puntos.", 50);
            insignia("Constante", "Sumaste 250 puntos aprendiendo.", 250);
            insignia("Explorador", "Llegaste a 500 puntos.", 500);
            insignia("Maestro del aprendizaje", "Superaste los 1000 puntos.", 1000);
        }
        if (tutores.count() == 0) {
            TutorIA t = new TutorIA();
            t.setNombre("Tutor V-Learning");
            t.setModelo(ollama.model());
            tutores.save(t);
        }
    }

    private void regla(String nombre, String descripcion, String evento, int puntos) {
        ReglaGamificacion r = new ReglaGamificacion();
        r.setNombre(nombre);
        r.setDescripcion(descripcion);
        r.setEvento(evento);
        r.setPuntos(puntos);
        reglas.save(r);
    }

    private void insignia(String nombre, String descripcion, int puntos) {
        Insignia i = new Insignia();
        i.setNombre(nombre);
        i.setDescripcion(descripcion);
        i.setPuntosRequeridos(puntos);
        insignias.save(i);
    }

    // ------------------------------------------------------------------ demo

    private void sembrarDemo() {
        String hash = encoder.encode(PASSWORD_DEMO);
        activo(usuarios, new Administrador("Administración V-Learning", "admin@tdea.edu.co", hash));
        Instructor laura = activo(usuarios, new Instructor("Laura Mejía", "laura.mejia@tdea.edu.co", hash, "INS-0001", "Fundamentos de programación"));

        Estudiante camila = new Estudiante("Camila Rojas", "camila.rojas@tdea.edu.co", encoder.encode(PASSWORD_TEMPORAL_DEMO), "EST-0001", "Ingeniería de Software");
        camila.setEstado(EstadoUsuario.PENDIENTE_PRIMER_ACCESO); // demuestra el flujo de primer acceso (RF-001)
        usuarios.save(camila);
        Estudiante andres = activo(usuarios, new Estudiante("Andrés Gómez", "andres.gomez@tdea.edu.co", hash, "EST-0002", "Ingeniería de Software"));
        Estudiante sofia = activo(usuarios, new Estudiante("Sofía Restrepo", "sofia.restrepo@tdea.edu.co", hash, "EST-0003", "Ingeniería de Software"));
        Estudiante mateo = activo(usuarios, new Estudiante("Mateo Álvarez", "mateo.alvarez@tdea.edu.co", hash, "EST-0004", "Tecnología en Sistemas"));

        Curso curso = new Curso();
        curso.setInstructor(laura);
        curso.setTitulo("Introducción a la programación");
        curso.setDescripcion("Curso de demostración: variables, decisiones y ciclos, con material en varios formatos accesibles.");
        curso.publicar();
        curso = cursos.save(curso);

        Modulo m1 = modulo(curso, "Variables y tipos de datos", "Qué es una variable y cómo guardar información.", 1);
        Modulo m2 = modulo(curso, "Decisiones y ciclos", "Cómo hacer que el programa decida y repita tareas.", 2);

        Contenido lectura = contenido(m1, "¿Qué es una variable?", FormatoContenido.LECTURA, 8,
                "Una variable es un espacio con nombre donde el programa guarda un dato. Por ejemplo, edad = 20 guarda el número 20 con el nombre edad. "
                        + "Los datos pueden ser números, texto o valores de verdadero y falso. Puedes cambiar el valor de una variable cuando lo necesites.", null);
        Contenido video = contenido(m1, "Tipos de datos en 5 minutos", FormatoContenido.VIDEO, 5,
                "Video introductorio sobre números enteros, decimales, texto y booleanos.", "https://example.org/demo/tipos-de-datos");
        recurso(video, TipoRecursoAccesible.SUBTITULO, "https://example.org/demo/tipos-de-datos.vtt", "Subtítulos en español.");
        recurso(video, TipoRecursoAccesible.TRANSCRIPCION, "interno:texto",
                "Hay cuatro tipos básicos de datos: enteros para contar, decimales para medir, texto para palabras y booleanos para verdadero o falso.");
        contenido(m2, "Condicionales: si pasa esto, haz aquello", FormatoContenido.LECTURA, 10,
                "Un condicional evalúa una pregunta con respuesta de sí o no. Si la respuesta es sí, ejecuta un bloque de instrucciones; si no, puede ejecutar otro. "
                        + "Por ejemplo: si la edad es mayor o igual a 18, mostrar «adulto»; si no, mostrar «menor».", null);
        contenido(m2, "Ciclos: repetir sin copiar", FormatoContenido.LECTURA, 10,
                "Un ciclo repite instrucciones mientras se cumpla una condición o un número de veces. Así evitas escribir lo mismo muchas veces.", null);

        Evaluacion quiz = new Evaluacion();
        quiz.setModulo(m1);
        quiz.setTitulo("Quiz: variables");
        quiz.setDescripcion("Repasa lo básico sobre variables.");
        quiz.setTipo(TipoEvaluacion.QUIZ);
        quiz.setPuntajeMaximo(new BigDecimal("10"));
        quiz.setTiempoLimite(15);
        quiz = evaluaciones.save(quiz);
        pregunta(quiz, 1, TipoPregunta.SELECCION_UNICA, "¿Para qué sirve una variable?", new BigDecimal("5"),
                new String[]{"Para guardar un dato con un nombre", "Para apagar el computador", "Para dibujar imágenes"}, 0);
        pregunta(quiz, 2, TipoPregunta.VERDADERO_FALSO, "El valor de una variable puede cambiar durante el programa.", new BigDecimal("5"),
                new String[]{"Verdadero", "Falso"}, 0);

        for (Estudiante e : new Estudiante[]{camila, andres, sofia, mateo}) {
            Inscripcion i = new Inscripcion();
            i.setEstudiante(e);
            i.setCurso(curso);
            inscripciones.save(i);
        }
    }

    private void sembrarEstudiantesAdicionales() {
        String hash = encoder.encode(PASSWORD_DEMO);
        String[][] datos = {
                {"Valentina Ospina", "valentina.ospina@tdea.edu.co", "EST-0005", "Ingeniería de Software"},
                {"Santiago Cardona", "santiago.cardona@tdea.edu.co", "EST-0006", "Ingeniería de Software"},
                {"Isabella Marín", "isabella.marin@tdea.edu.co", "EST-0007", "Tecnología en Sistemas"},
                {"Daniel Henao", "daniel.henao@tdea.edu.co", "EST-0008", "Ingeniería de Software"},
                {"Mariana Toro", "mariana.toro@tdea.edu.co", "EST-0009", "Tecnología en Sistemas"},
                {"Sebastián Zapata", "sebastian.zapata@tdea.edu.co", "EST-0010", "Ingeniería de Software"},
                {"Juliana Giraldo", "juliana.giraldo@tdea.edu.co", "EST-0011", "Tecnología en Sistemas"}
        };
        Curso demo = cursos.todosConInstructor().stream()
                .filter(c -> "Introducción a la programación".equals(c.getTitulo()))
                .findFirst().orElse(null);
        for (String[] d : datos) {
            if (usuarios.existsByCorreoInstitucionalIgnoreCase(d[1])) continue;
            Estudiante e = activo(usuarios, new Estudiante(d[0], d[1], hash, d[2], d[3]));
            if (demo != null) {
                Inscripcion i = new Inscripcion();
                i.setEstudiante(e);
                i.setCurso(demo);
                inscripciones.save(i);
            }
        }
    }

    private void sembrarAvanceSofia() {
        Estudiante sofia = usuarios.findByCorreoInstitucionalIgnoreCase("sofia.restrepo@tdea.edu.co")
                .filter(u -> u instanceof Estudiante).map(u -> (Estudiante) u).orElse(null);
        Curso demo = cursos.todosConInstructor().stream()
                .filter(c -> "Introducción a la programación".equals(c.getTitulo()))
                .findFirst().orElse(null);
        if (sofia == null || demo == null) return;
        List<Long> idsModulos = modulos.findByCursoIdOrderByOrden(demo.getId()).stream().map(Modulo::getId).toList();
        List<Contenido> lista = contenidos.findByModuloIdInOrderByIdAsc(idsModulos);
        if (lista.size() < 3 || progresos.findByEstudianteIdAndContenidoId(sofia.getId(), lista.get(0).getId()).isPresent()) return;
        progresoService.registrar(sofia.getId(), lista.get(0).getId(), new ProgresoRequest(480L, new BigDecimal("100"), true));
        progresoService.registrar(sofia.getId(), lista.get(1).getId(), new ProgresoRequest(300L, new BigDecimal("100"), true));
        progresoService.registrar(sofia.getId(), lista.get(2).getId(), new ProgresoRequest(240L, new BigDecimal("40"), false));
    }

    private void reiniciarAvanceDemo() {
        String ids = "(select id from usuarios where lower(correo_institucional) in ("
                + "'camila.rojas@tdea.edu.co','andres.gomez@tdea.edu.co','mateo.alvarez@tdea.edu.co',"
                + "'valentina.ospina@tdea.edu.co','santiago.cardona@tdea.edu.co','isabella.marin@tdea.edu.co',"
                + "'daniel.henao@tdea.edu.co','mariana.toro@tdea.edu.co','sebastian.zapata@tdea.edu.co',"
                + "'juliana.giraldo@tdea.edu.co'))";
        jdbc.update("delete from intentos_evaluacion where estudiante_id in " + ids);
        jdbc.update("delete from progresos where estudiante_id in " + ids);
        jdbc.update("delete from entregas_tarea where estudiante_id in " + ids);
        jdbc.update("delete from consultas_tutor where estudiante_id in " + ids);
        jdbc.update("delete from historial_puntos where estudiante_id in " + ids);
        jdbc.update("delete from perfiles_gamificacion where estudiante_id in " + ids);
        jdbc.update("delete from posiciones_ranking where estudiante_id in " + ids);
        jdbc.update("delete from notificaciones where usuario_id in " + ids);
        jdbc.update("update inscripciones set porcentaje_completado = 0, estado = 'ACTIVA' where estudiante_id in " + ids);
    }

    private <T extends Usuario> T activo(UsuarioRepository repo, T u) {
        u.setEstado(EstadoUsuario.ACTIVO);
        return repo.save(u);
    }

    private Modulo modulo(Curso curso, String titulo, String descripcion, int orden) {
        Modulo m = new Modulo();
        m.setCurso(curso);
        m.setTitulo(titulo);
        m.setDescripcion(descripcion);
        m.setOrden(orden);
        return modulos.save(m);
    }

    private Contenido contenido(Modulo modulo, String titulo, FormatoContenido formato, int minutos, String cuerpoODescripcion, String url) {
        Contenido c = new Contenido();
        c.setModulo(modulo);
        c.setTitulo(titulo);
        c.setFormato(formato);
        c.setDuracionMinutos(minutos);
        c.setUrlRecurso(url);
        if (formato == FormatoContenido.LECTURA) {
            c.setDescripcion(titulo);
            c.setCuerpo(cuerpoODescripcion);
        } else {
            c.setDescripcion(cuerpoODescripcion);
        }
        c.publicar();
        return contenidos.save(c);
    }

    private void recurso(Contenido contenido, TipoRecursoAccesible tipo, String url, String descripcion) {
        RecursoAccesible r = new RecursoAccesible();
        r.setContenido(contenido);
        r.setTipo(tipo);
        r.setUrl(url);
        r.setDescripcion(descripcion);
        recursos.save(r);
    }

    private void pregunta(Evaluacion e, int orden, TipoPregunta tipo, String enunciado, BigDecimal puntaje, String[] textos, int correcta) {
        Pregunta p = new Pregunta();
        p.setEvaluacion(e);
        p.setOrden(orden);
        p.setTipo(tipo);
        p.setEnunciado(enunciado);
        p.setPuntaje(puntaje);
        p = preguntas.save(p);
        for (int i = 0; i < textos.length; i++) {
            OpcionRespuesta o = new OpcionRespuesta();
            o.setPregunta(p);
            o.setTexto(textos[i]);
            o.setCorrecta(i == correcta);
            opciones.save(o);
        }
    }
}