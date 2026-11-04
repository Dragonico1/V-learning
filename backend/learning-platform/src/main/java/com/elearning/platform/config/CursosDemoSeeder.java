package com.elearning.platform.config;

import com.elearning.platform.entity.Contenido;
import com.elearning.platform.entity.Curso;
import com.elearning.platform.entity.Evaluacion;
import com.elearning.platform.entity.Instructor;
import com.elearning.platform.entity.Modulo;
import com.elearning.platform.entity.OpcionRespuesta;
import com.elearning.platform.entity.Pregunta;
import com.elearning.platform.entity.RecursoAccesible;
import com.elearning.platform.enums.FormatoContenido;
import com.elearning.platform.enums.TipoEvaluacion;
import com.elearning.platform.enums.TipoPregunta;
import com.elearning.platform.enums.TipoRecursoAccesible;
import com.elearning.platform.repository.ContenidoRepository;
import com.elearning.platform.repository.CursoRepository;
import com.elearning.platform.repository.EvaluacionRepository;
import com.elearning.platform.repository.InstructorRepository;
import com.elearning.platform.repository.ModuloRepository;
import com.elearning.platform.repository.OpcionRespuestaRepository;
import com.elearning.platform.repository.PreguntaRepository;
import com.elearning.platform.repository.RecursoAccesibleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cursos de ejemplo para desarrollo: cinco cursos de Ingeniería de Software con módulos,
 * contenidos en varios formatos, recursos accesibles y un quiz por curso.
 * Es idempotente: no vuelve a crear un curso si ya existe uno con el mismo título.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CursosDemoSeeder {

    private final CursoRepository cursos;
    private final ModuloRepository modulos;
    private final ContenidoRepository contenidos;
    private final RecursoAccesibleRepository recursos;
    private final EvaluacionRepository evaluaciones;
    private final PreguntaRepository preguntas;
    private final OpcionRespuestaRepository opciones;
    private final InstructorRepository instructores;

    /** Crea los cursos de ejemplo que falten, asignados al primer instructor disponible. */
    public void sembrar() {
        Instructor instructor = instructores.findAll().stream().findFirst().orElse(null);
        if (instructor == null) {
            log.info("No hay instructores: no se crean los cursos de ejemplo.");
            return;
        }

        int creados = 0;
        for (CursoDemo curso : cursosDemo()) {
            if (cursos.existsByTitulo(curso.titulo())) continue;
            crear(instructor, curso);
            creados++;
        }
        if (creados > 0) {
            log.warn("Se crearon {} cursos de ejemplo con módulos, contenidos y evaluaciones.", creados);
        }
    }

    private void crear(Instructor instructor, CursoDemo datos) {
        Curso curso = new Curso();
        curso.setInstructor(instructor);
        curso.setTitulo(datos.titulo());
        curso.setDescripcion(datos.descripcion());
        curso.publicar();
        curso = cursos.save(curso);

        int orden = 1;
        for (ModuloDemo datosModulo : datos.modulos()) {
            Modulo modulo = new Modulo();
            modulo.setCurso(curso);
            modulo.setTitulo(datosModulo.titulo());
            modulo.setDescripcion(datosModulo.descripcion());
            modulo.setOrden(orden++);
            modulo = modulos.save(modulo);

            for (ContenidoDemo datosContenido : datosModulo.contenidos()) {
                Contenido contenido = new Contenido();
                contenido.setModulo(modulo);
                contenido.setTitulo(datosContenido.titulo());
                contenido.setDescripcion(datosContenido.descripcion());
                contenido.setFormato(datosContenido.formato());
                contenido.setDuracionMinutos(datosContenido.minutos());
                contenido.setUrlRecurso(datosContenido.url());
                contenido.setCuerpo(datosContenido.cuerpo());
                contenido.publicar();
                contenido = contenidos.save(contenido);

                if (datosContenido.subtituloUrl() != null) {
                    recurso(contenido, TipoRecursoAccesible.SUBTITULO,
                            datosContenido.subtituloUrl(), "Subtítulos en español.");
                }
                if (datosContenido.transcripcion() != null) {
                    recurso(contenido, TipoRecursoAccesible.TRANSCRIPCION,
                            "interno:texto", datosContenido.transcripcion());
                }
            }

            if (datosModulo.evaluacion() != null) {
                crearEvaluacion(modulo, datosModulo.evaluacion());
            }
        }
    }

    private void crearEvaluacion(Modulo modulo, EvaluacionDemo datos) {
        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setModulo(modulo);
        evaluacion.setTitulo(datos.titulo());
        evaluacion.setDescripcion(datos.descripcion());
        evaluacion.setTipo(TipoEvaluacion.QUIZ);
        evaluacion.setPuntajeMaximo(BigDecimal.valueOf(datos.preguntas().size() * 5L));
        evaluacion.setTiempoLimite(15);
        evaluacion = evaluaciones.save(evaluacion);

        int orden = 1;
        for (PreguntaDemo datosPregunta : datos.preguntas()) {
            Pregunta pregunta = new Pregunta();
            pregunta.setEvaluacion(evaluacion);
            pregunta.setOrden(orden++);
            pregunta.setTipo(datosPregunta.tipo());
            pregunta.setEnunciado(datosPregunta.enunciado());
            pregunta.setPuntaje(new BigDecimal("5"));
            pregunta = preguntas.save(pregunta);

            for (int i = 0; i < datosPregunta.opciones().size(); i++) {
                OpcionRespuesta opcion = new OpcionRespuesta();
                opcion.setPregunta(pregunta);
                opcion.setTexto(datosPregunta.opciones().get(i));
                opcion.setCorrecta(i == datosPregunta.correcta());
                opciones.save(opcion);
            }
        }
    }

    private void recurso(Contenido contenido, TipoRecursoAccesible tipo, String url, String descripcion) {
        RecursoAccesible recurso = new RecursoAccesible();
        recurso.setContenido(contenido);
        recurso.setTipo(tipo);
        recurso.setUrl(url);
        recurso.setDescripcion(descripcion);
        recursos.save(recurso);
    }

    // ------------------------------------------------------------------ datos

    static List<CursoDemo> cursosDemo() {
        return List.of(
                new CursoDemo(
                        "Fundamentos de Programación con Python",
                        "Aprende desde cero a escribir programas: variables, decisiones, ciclos y buenas prácticas.",
                        List.of(
                                modulo("Primeros pasos en Python",
                                        "Instalación, sintaxis básica y tu primer programa.",
                                        List.of(
                                                lectura("¿Qué es un programa y cómo se ejecuta?", 8, """
                                                        Un programa es una secuencia ordenada de instrucciones que le dice a la computadora qué hacer. En Python, esas instrucciones se escriben en un archivo con extensión .py y se ejecutan con el intérprete de Python.

                                                        Al ejecutar el archivo, Python lee las instrucciones de arriba hacia abajo. Si encuentra un error, muestra el número de línea y una descripción para que puedas corregirlo.

                                                        Para empezar necesitas un editor de código y Python instalado. Un buen primer programa es mostrar un saludo en pantalla con print("Hola, mundo")."""),
                                                video("Tu primer programa en Python", 6,
                                                        "Creamos un programa que saluda y explicamos cómo ejecutarlo.",
                                                        "https://example.org/demo/python-primer-programa",
                                                        "En este video abrimos el editor, creamos un archivo llamado saludo.py y escribimos una instrucción con print. Luego ejecutamos el archivo desde la terminal y vemos el mensaje en pantalla. También explicamos qué significa un error de sintaxis y cómo leer el número de línea que muestra Python."))),
                                modulo("Variables, tipos y operadores",
                                        "Guarda datos y realiza cálculos con expresiones.",
                                        List.of(
                                                lectura("Variables y tipos de datos", 10, """
                                                        Una variable es un nombre que guarda un valor para usarlo más adelante. En Python no necesitas declarar el tipo: el intérprete lo deduce del valor. Por ejemplo, edad = 20 crea una variable entera y nombre = "Ana" crea una de texto.

                                                        Los tipos básicos son int (números enteros), float (decimales), str (texto) y bool (verdadero o falso). Puedes cambiar el valor de una variable cuando quieras: edad = 21 reemplaza el valor anterior.

                                                        Usa nombres descriptivos y evita palabras reservadas como if, for o class."""),
                                                podcast("Operadores y expresiones", 7,
                                                        "Cómo combinar datos con operadores aritméticos, de comparación y lógicos.",
                                                        "https://example.org/demo/python-operadores",
                                                        "En este episodio hablamos de los operadores que usa Python para calcular y comparar. Los aritméticos son suma, resta, multiplicación, división y módulo. Los de comparación devuelven verdadero o falso, por ejemplo 5 > 3. También existen los operadores lógicos and, or y not, que permiten combinar condiciones. Recuerda que el orden de las operaciones importa: usa paréntesis cuando quieras dejar clara la intención."))),
                                modulo("Decisiones y ciclos",
                                        "Controla el flujo del programa con condicionales y repeticiones.",
                                        List.of(
                                                lectura("Condicionales: if, elif y else", 12, """
                                                        Los condicionales permiten que el programa tome decisiones. La estructura if evalúa una condición; si es verdadera, ejecuta un bloque. Con elif puedes probar otras condiciones y con else indicas qué hacer cuando ninguna se cumple.

                                                        En Python la indentación es obligatoria: el bloque que pertenece al if debe estar desplazado a la derecha. Una condición puede combinar comparaciones con and, or y not.

                                                        Ejemplo: si la edad es mayor o igual a 18, muestra «adulto»; si no, muestra «menor»."""),
                                                lectura("Ciclos for y while", 12, """
                                                        Un ciclo repite instrucciones sin necesidad de copiarlas. for recorre una secuencia o un rango de números; while repite mientras una condición sea verdadera.

                                                        Por ejemplo, for i in range(5) ejecuta el bloque cinco veces. Con while debes asegurarte de que la condición cambie en algún momento para evitar un ciclo infinito.

                                                        Usa break para salir antes y continue para saltar a la siguiente repetición.""")
                                        ),
                                        quiz("Quiz: fundamentos de Python",
                                                        "Comprueba lo aprendido sobre tipos de datos y estructuras de control.",
                                                        seleccion("¿Qué tipo de dato guarda el valor 3.14 en Python?", 1,
                                                                "int", "float", "str", "bool"),
                                                        seleccion("¿Qué estructura permite repetir instrucciones?", 1,
                                                                "if", "for", "print", "def"),
                                                        verdaderoFalso("En Python, el valor de una variable puede cambiar durante la ejecución del programa.", true))))),

                new CursoDemo(
                        "Desarrollo Web con React",
                        "Construye interfaces modernas con componentes, estado y rutas, y conecta tu aplicación a una API.",
                        List.of(
                                modulo("Fundamentos de la web y React",
                                        "Cómo funciona la web y por qué usar React.",
                                        List.of(
                                                lectura("HTML, CSS y JavaScript en pocas palabras", 10, """
                                                        HTML describe la estructura de una página con etiquetas como header, main y button. CSS define la apariencia: colores, tamaños, espacios y distribución. JavaScript añade comportamiento: responde a clics, valida formularios y actualiza la página sin recargarla.

                                                        React es una biblioteca de JavaScript para construir interfaces con componentes. Un componente combina estructura, estilos y lógica en una pieza reutilizable.

                                                        Antes de aprender React conviene entender el DOM y los eventos del navegador."""),
                                                video("Tu primera aplicación con React", 8,
                                                        "Creamos un proyecto con Vite y renderizamos nuestro primer componente.",
                                                        "https://example.org/demo/react-primera-app",
                                                        "En este video creamos una aplicación con Vite y React. Primero ejecutamos el comando de creación y revisamos la estructura de carpetas. El archivo principal renderiza un componente llamado App dentro del elemento root. Después cambiamos el texto de bienvenida y guardamos: el navegador se actualiza automáticamente gracias al servidor de desarrollo. Al final explicamos qué es un componente y cómo se reutiliza."))),
                                modulo("Componentes y estado",
                                        "Construye interfaces con componentes reutilizables y datos que cambian.",
                                        List.of(
                                                lectura("Componentes y props", 12, """
                                                        Un componente es una función que devuelve JSX, una sintaxis que parece HTML pero vive dentro de JavaScript. Los componentes se pueden reutilizar y combinar para formar pantallas completas.

                                                        Las props son datos que un componente recibe desde su componente padre. Se leen como un objeto y no deben modificarse dentro del componente: si necesitas cambiarlas, el padre debe enviar un nuevo valor.

                                                        Usa props para personalizar textos, imágenes y estilos sin duplicar componentes."""),
                                                lectura("Estado con useState y eventos", 12, """
                                                        El estado es información que cambia con el tiempo y que provoca una nueva renderización cuando se actualiza. En React se declara con useState, que devuelve el valor actual y una función para cambiarlo.

                                                        Los eventos, como onClick o onChange, conectan la interfaz con la lógica. Al hacer clic en un botón puedes actualizar el estado y la pantalla reflejará el nuevo valor.

                                                        No modifiques el estado directamente: usa siempre la función que devuelve useState."""))),
                                modulo("Rutas y consumo de datos",
                                        "Navega entre vistas y conecta tu aplicación con un servicio externo.",
                                        List.of(
                                                lectura("Rutas con React Router", 10, """
                                                        React Router permite mostrar distintas páginas según la URL sin recargar el navegador. Se definen rutas con Route y se agrupan dentro de Routes. Los enlaces se crean con Link o NavLink en lugar de la etiqueta a.

                                                        Los parámetros de ruta, como /cursos/:id, se leen con useParams. Para navegar desde código se usa useNavigate.

                                                        Una buena estructura de rutas separa las páginas públicas de las que requieren autenticación."""),
                                                simulacion("Consumiendo una API con fetch", 15,
                                                        "Practica el flujo completo para pedir datos y mostrarlos en pantalla.",
                                                        """
                                                        Paso 1: elige un endpoint público o de tu backend y verifica que responda JSON.

                                                        Paso 2: crea una función asíncrona y llama a fetch con la URL.

                                                        Paso 3: revisa response.ok; si es falso, lanza un error.

                                                        Paso 4: convierte la respuesta con response.json().

                                                        Paso 5: guarda los datos en el estado y renderiza una lista.

                                                        Recuerda manejar los estados de carga y de error para que la interfaz no quede vacía.""")
                                        ),
                                        quiz("Quiz: desarrollo web con React",
                                                        "Repasa los conceptos clave de componentes, estado y rutas.",
                                                        seleccion("¿Qué permite JSX en React?", 0,
                                                                "Escribir una sintaxis similar a HTML dentro de JavaScript",
                                                                "Reemplazar por completo a JavaScript",
                                                                "Diseñar bases de datos",
                                                                "Compilar CSS automáticamente"),
                                                        seleccion("¿Qué hook se usa para guardar estado local en un componente?", 1,
                                                                "useEffect", "useState", "useMemo", "useContext"),
                                                        verdaderoFalso("Las props son datos que el componente recibe y no debe modificar directamente.", true))))),

                new CursoDemo(
                        "Bases de Datos Relacionales",
                        "Diseña, consulta y mantén bases de datos con SQL y buenas prácticas de modelado.",
                        List.of(
                                modulo("Modelo relacional",
                                        "Tablas, relaciones y claves para organizar la información.",
                                        List.of(
                                                lectura("Tablas, filas y columnas", 8, """
                                                        Una base de datos relacional organiza la información en tablas. Cada tabla tiene columnas que definen los campos y filas que contienen los registros. Por ejemplo, una tabla estudiantes puede tener id, nombre, correo y programa.

                                                        Cada columna tiene un tipo de dato: entero, texto, fecha o booleano. El diseño de la tabla debe reflejar las preguntas que quieres responder.

                                                        Las relaciones entre tablas permiten evitar repetir información y mantener la consistencia."""),
                                                video("Cómo modelar una base de datos", 10,
                                                        "Del enunciado a las tablas: entidades, atributos y relaciones.",
                                                        "https://example.org/demo/bd-modelado",
                                                        "En este video partimos de un enunciado y identificamos las entidades principales: estudiantes, cursos e inscripciones. Luego definimos los atributos de cada entidad y elegimos una llave primaria. Después representamos las relaciones con llaves foráneas y revisamos la cardinalidad. Al final comparamos un modelo con redundancia frente a uno normalizado y explicamos por qué el segundo facilita el mantenimiento."))),
                                modulo("SQL esencial",
                                        "Consulta y modifica datos con las sentencias más usadas.",
                                        List.of(
                                                lectura("SELECT, WHERE y ORDER BY", 12, """
                                                        SELECT indica las columnas que quieres consultar. El asterisco devuelve todas, pero en producción conviene enumerar solo las necesarias. FROM indica la tabla de origen.

                                                        WHERE filtra las filas que cumplen una condición, por ejemplo WHERE programa = 'Ingeniería de Software'. Puedes combinar condiciones con AND, OR y NOT.

                                                        ORDER BY ordena el resultado de forma ascendente (ASC) o descendente (DESC). LIMIT reduce el número de filas devueltas."""),
                                                lectura("INSERT, UPDATE y DELETE", 12, """
                                                        INSERT INTO agrega filas nuevas a una tabla. Debes indicar las columnas y los valores en el mismo orden. UPDATE modifica filas existentes y casi siempre debe incluir un WHERE para no cambiar toda la tabla.

                                                        DELETE elimina filas; si omites el WHERE, borras todos los registros. Antes de ejecutar una modificación, prueba la condición con un SELECT.

                                                        Los cambios deben confirmarse con COMMIT o revertirse con ROLLBACK según el motor y la transacción."""))),
                                modulo("Relaciones y normalización",
                                        "Diseña bases de datos sin redundancia y con integridad referencial.",
                                        List.of(
                                                lectura("Llaves primarias y foráneas", 10, """
                                                        Una llave primaria identifica de forma única cada fila de una tabla. Puede ser un número generado automáticamente o una combinación de columnas. No admite valores nulos ni duplicados.

                                                        Una llave foránea es una columna que apunta a la llave primaria de otra tabla. Así se representan relaciones como «un estudiante tiene muchas inscripciones».

                                                        Las restricciones de integridad referencial impiden que existan inscripciones sin un estudiante o un curso válido."""),
                                                podcast("Normalización explicada con ejemplos", 8,
                                                        "Cómo separar entidades y evitar información repetida.",
                                                        "https://example.org/demo/bd-normalizacion",
                                                        "En este episodio explicamos la normalización como un proceso para organizar las tablas y evitar redundancia. Comenzamos con una tabla que mezcla estudiantes, cursos y notas. Luego separamos cada entidad en su propia tabla y conectamos con llaves foráneas. Revisamos la primera, segunda y tercera forma normal con ejemplos sencillos. Al final comentamos cuándo conviene desnormalizar para mejorar el rendimiento de las consultas.")
                                        ),
                                        quiz("Quiz: bases de datos relacionales",
                                                        "Comprueba lo aprendido sobre SQL, llaves y normalización.",
                                                        seleccion("¿Qué comando de SQL se usa para leer datos?", 0,
                                                                "SELECT", "INSERT", "UPDATE", "DELETE"),
                                                        seleccion("¿Qué llave identifica de forma única cada fila de una tabla?", 1,
                                                                "Llave foránea", "Llave primaria", "Índice", "Vista"),
                                                        verdaderoFalso("Una llave foránea conecta una fila con la llave primaria de otra tabla.", true))))),

                new CursoDemo(
                        "Ingeniería de Software y Scrum",
                        "Aprende el ciclo de vida del software, el trabajo con Scrum y las pruebas que aseguran la calidad.",
                        List.of(
                                modulo("Ciclo de vida del software",
                                        "Del requisito al producto funcionando.",
                                        List.of(
                                                lectura("Del requisito al producto", 10, """
                                                        Un requisito describe una necesidad que el software debe satisfacer. Los requisitos funcionales indican qué debe hacer el sistema; los no funcionales describen calidad, rendimiento, seguridad o usabilidad.

                                                        El ciclo de vida conecta requisitos, diseño, implementación, pruebas y mantenimiento. Cada etapa produce artefactos que permiten revisar y mejorar el trabajo.

                                                        Un requisito ambiguo genera retrabajo. Por eso conviene escribirlo con ejemplos y criterios de aceptación verificables."""),
                                                lectura("Modelos de desarrollo: cascada e iterativo", 12, """
                                                        El modelo en cascada avanza por etapas secuenciales: requisitos, diseño, implementación, pruebas y despliegue. Funciona cuando los requisitos son estables, pero dificulta incorporar cambios tarde.

                                                        Los modelos iterativos e incrementales construyen el producto en ciclos cortos. Cada iteración entrega una parte funcional y permite obtener retroalimentación temprana.

                                                        Scrum es un marco ágil basado en iteraciones llamadas sprints. No es una receta rígida: se adapta al contexto del equipo."""))),
                                modulo("Scrum en la práctica",
                                        "Roles, eventos, artefactos e historias de usuario.",
                                        List.of(
                                                video("Roles, eventos y artefactos de Scrum", 10,
                                                        "Un recorrido práctico por el marco de trabajo Scrum.",
                                                        "https://example.org/demo/scrum-roles",
                                                        "En este video repasamos los tres roles de Scrum: Product Owner, Scrum Master y equipo de desarrollo. Después explicamos los eventos: planificación del sprint, reunión diaria, revisión y retrospectiva. Los artefactos principales son el product backlog, el sprint backlog y el incremento. Cerramos con un ejemplo de cómo se toma una historia de usuario y se convierte en tareas para el sprint."),
                                                lectura("Historias de usuario y criterios de aceptación", 12, """
                                                        Una historia de usuario describe una necesidad desde la perspectiva de quien usa el sistema. Un formato habitual es: «Como [rol], quiero [acción] para [beneficio]».

                                                        Los criterios de aceptación definen cuándo la historia está terminada. Deben ser concretos y comprobables, por ejemplo: «El usuario puede iniciar sesión con correo y contraseña válidos».

                                                        Las historias se priorizan en el product backlog y se estiman con el equipo."""))),
                                modulo("Calidad y pruebas",
                                        "Asegura la calidad con pruebas y revisión de código.",
                                        List.of(
                                                lectura("Pruebas unitarias, de integración y de aceptación", 12, """
                                                        Las pruebas unitarias verifican una función o clase de forma aislada. Son rápidas y ayudan a detectar errores antes de integrar cambios.

                                                        Las pruebas de integración comprueban que varias piezas funcionan juntas, como la API y la base de datos. Las de aceptación validan que una historia cumple los criterios acordados con el usuario.

                                                        Automatizar las pruebas permite repetirlas en cada cambio y reducir el riesgo de regresiones."""),
                                                podcast("Buenas prácticas de revisión de código", 8,
                                                        "Cómo dar y recibir retroalimentación útil sobre el código.",
                                                        "https://example.org/demo/revision-codigo",
                                                        "En este episodio hablamos de la revisión de código como una conversación, no como un examen. Recomendamos cambios pequeños, descripciones claras y comentarios sobre el código, no sobre la persona. Revisamos los aspectos que más importan: claridad, manejo de errores, pruebas y seguridad. También explicamos cómo priorizar los comentarios y cuándo es mejor conversar en persona. Al final recordamos que una buena revisión mejora el producto y comparte conocimiento.")
                                        ),
                                        quiz("Quiz: ingeniería de software y Scrum",
                                                        "Repasa requisitos, Scrum y pruebas.",
                                                        seleccion("¿Qué es una historia de usuario?", 0,
                                                                "Una descripción breve de una necesidad desde la perspectiva del usuario",
                                                                "Un diagrama de clases",
                                                                "Un tipo de base de datos",
                                                                "Una prueba de rendimiento"),
                                                        seleccion("¿En qué evento de Scrum se revisa el incremento con los interesados?", 1,
                                                                "Daily Scrum", "Sprint Review", "Retrospectiva", "Planificación del sprint"),
                                                        verdaderoFalso("Las pruebas unitarias verifican una parte pequeña del código de forma aislada.", true))))),

                new CursoDemo(
                        "Seguridad Informática Básica",
                        "Protege tu información, reconoce amenazas comunes y aplica buenas prácticas de seguridad.",
                        List.of(
                                modulo("Fundamentos de seguridad",
                                        "Los pilares que protegen la información.",
                                        List.of(
                                                lectura("Confidencialidad, integridad y disponibilidad", 8, """
                                                        La seguridad de la información se apoya en tres pilares. La confidencialidad garantiza que solo las personas autorizadas accedan a los datos. La integridad asegura que la información no se altere sin autorización. La disponibilidad garantiza que los servicios estén accesibles cuando se necesitan.

                                                        Un incidente puede afectar uno o varios pilares. Por ejemplo, un ransomware puede cifrar archivos y dejar el servicio no disponible.

                                                        Las políticas, los controles de acceso y los respaldos ayudan a proteger los tres pilares."""),
                                                video("Contraseñas seguras y autenticación", 7,
                                                        "Buenas prácticas para proteger tus cuentas.",
                                                        "https://example.org/demo/seguridad-contrasenas",
                                                        "En este video explicamos por qué una contraseña larga y única es más segura que una corta con símbolos predecibles. Mostramos cómo funciona un gestor de contraseñas y la verificación en dos pasos. Después hablamos de no reutilizar credenciales entre servicios y de cerrar sesión en equipos compartidos. Al final revisamos qué hacer si sospechas que tu cuenta fue comprometida."))),
                                modulo("Amenazas comunes",
                                        "Reconoce los ataques más frecuentes.",
                                        List.of(
                                                lectura("Phishing e ingeniería social", 10, """
                                                        El phishing es un engaño que busca que entregues datos, hagas clic en un enlace o descargues un archivo. Suele imitar a una entidad conocida y usa urgencia, premios o amenazas para presionarte.

                                                        Antes de responder, verifica el remitente, el dominio del enlace y el contexto del mensaje. Desconfía de solicitudes de contraseñas o pagos por canales inesperados.

                                                        La ingeniería social explota la confianza y las emociones. Si algo parece extraño, confirma por otro medio antes de actuar."""),
                                                lectura("Malware y ransomware", 10, """
                                                        Malware es cualquier software diseñado para causar daño: virus, troyanos, spyware y ransomware. El ransomware cifra archivos y exige un pago para liberarlos.

                                                        La infección puede llegar por adjuntos, descargas o sitios falsos. Mantén el sistema actualizado, usa antivirus y evita ejecutar archivos de origen desconocido.

                                                        Los respaldos fuera de línea son la defensa más eficaz: permiten recuperar la información sin pagar el rescate."""))),
                                modulo("Buenas prácticas",
                                        "Protege tus datos y dispositivos en el día a día.",
                                        List.of(
                                                lectura("Copias de seguridad y actualizaciones", 8, """
                                                        Una copia de seguridad es una réplica de tus datos que puedes restaurar si algo falla. Aplica la regla 3-2-1: tres copias, en dos medios distintos y una fuera del sitio.

                                                        Prueba la restauración periódicamente: un respaldo que nunca se ha probado no garantiza nada. Define qué datos son críticos y con qué frecuencia cambian.

                                                        Las actualizaciones corrigen errores y cierran vulnerabilidades. Activa las actualizaciones automáticas y reinicia cuando sea necesario."""),
                                                podcast("Seguridad en el trabajo remoto", 8,
                                                        "Riesgos y recomendaciones para trabajar fuera de la oficina.",
                                                        "https://example.org/demo/seguridad-remoto",
                                                        "En este episodio hablamos de los riesgos de trabajar desde casa o en espacios públicos. Recomendamos usar una red confiable, una VPN cuando corresponda y contraseñas únicas. También explicamos cómo proteger la pantalla y los documentos frente a miradas ajenas. Revisamos el uso de dispositivos personales y la importancia de mantener el equipo actualizado. Cerramos con una lista breve de verificación para antes de conectarte.")
                                        ),
                                        quiz("Quiz: seguridad informática básica",
                                                        "Comprueba lo aprendido sobre los pilares de seguridad y las amenazas comunes.",
                                                        seleccion("¿Qué garantiza el principio de confidencialidad?", 0,
                                                                "Que solo las personas autorizadas accedan a la información",
                                                                "Que la información esté siempre disponible",
                                                                "Que los archivos no se puedan copiar",
                                                                "Que el software sea más rápido"),
                                                        seleccion("¿Cuál es una señal típica de phishing?", 0,
                                                                "Un mensaje urgente que pide datos personales o contraseñas",
                                                                "Una actualización automática del sistema",
                                                                "Un respaldo programado",
                                                                "Una contraseña larga y única"),
                                                        verdaderoFalso("Usar la misma contraseña en todos los servicios es una buena práctica de seguridad.", false))))));
    }

    private static ModuloDemo modulo(String titulo, String descripcion, List<ContenidoDemo> contenidos) {
        return new ModuloDemo(titulo, descripcion, contenidos, null);
    }

    private static ModuloDemo modulo(String titulo, String descripcion, List<ContenidoDemo> contenidos,
                                     EvaluacionDemo evaluacion) {
        return new ModuloDemo(titulo, descripcion, contenidos, evaluacion);
    }

    private static ContenidoDemo lectura(String titulo, int minutos, String cuerpo) {
        return new ContenidoDemo(titulo, FormatoContenido.LECTURA, minutos, titulo, cuerpo, null, null, null);
    }

    private static ContenidoDemo video(String titulo, int minutos, String descripcion, String url, String transcripcion) {
        return new ContenidoDemo(titulo, FormatoContenido.VIDEO, minutos, descripcion, descripcion, url,
                transcripcion, url + ".vtt");
    }

    private static ContenidoDemo podcast(String titulo, int minutos, String descripcion, String url, String transcripcion) {
        return new ContenidoDemo(titulo, FormatoContenido.PODCAST, minutos, descripcion, descripcion, url,
                transcripcion, null);
    }

    private static ContenidoDemo simulacion(String titulo, int minutos, String descripcion, String cuerpo) {
        return new ContenidoDemo(titulo, FormatoContenido.SIMULACION, minutos, descripcion, cuerpo, null, null, null);
    }

    private static EvaluacionDemo quiz(String titulo, String descripcion, PreguntaDemo... preguntas) {
        return new EvaluacionDemo(titulo, descripcion, List.of(preguntas));
    }

    private static PreguntaDemo seleccion(String enunciado, int correcta, String... opciones) {
        return new PreguntaDemo(enunciado, TipoPregunta.SELECCION_UNICA, List.of(opciones), correcta);
    }

    private static PreguntaDemo verdaderoFalso(String enunciado, boolean verdadero) {
        return new PreguntaDemo(enunciado, TipoPregunta.VERDADERO_FALSO,
                List.of("Verdadero", "Falso"), verdadero ? 0 : 1);
    }

    record CursoDemo(String titulo, String descripcion, List<ModuloDemo> modulos) {}

    record ModuloDemo(String titulo, String descripcion, List<ContenidoDemo> contenidos,
                      EvaluacionDemo evaluacion) {}

    record ContenidoDemo(String titulo, FormatoContenido formato, int minutos, String descripcion,
                         String cuerpo, String url, String transcripcion, String subtituloUrl) {}

    record EvaluacionDemo(String titulo, String descripcion, List<PreguntaDemo> preguntas) {}

    record PreguntaDemo(String enunciado, TipoPregunta tipo, List<String> opciones, int correcta) {}
}
