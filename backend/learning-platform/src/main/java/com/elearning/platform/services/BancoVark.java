package com.elearning.platform.services;

import com.elearning.platform.dto.VarkDtos.OpcionVark;
import com.elearning.platform.dto.VarkDtos.PreguntaVark;
import com.elearning.platform.enums.EstiloVark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Banco de 12 preguntas VARK (redacción propia, situaciones cotidianas). Cada pregunta tiene un
 * texto de ayuda (RF-004, extensión «pedir ayuda»). El orden de las opciones rota por pregunta
 * para que la posición no delate el estilo.
 */
public final class BancoVark {

    public static final String VERSION = "VARK-12-v1";
    public static final int TOTAL = 12;

    private static final String AYUDA_GENERAL =
            " No hay respuestas buenas ni malas: elige lo que harías de forma natural, aunque a veces uses más de una.";

    private record Fila(String enunciado, String ayuda, String v, String a, String r, String k) {}

    private static final List<Fila> FILAS = List.of(
            new Fila("Vas a aprender a usar una aplicación nueva en tu celular. ¿Qué haces primero?",
                    "Piensa en la última vez que usaste una herramienta nueva: ¿mirabas imágenes, escuchabas, leías o probabas?",
                    "Miro capturas de pantalla o un diagrama paso a paso.",
                    "Le pido a alguien que me explique en voz alta cómo se usa.",
                    "Leo las instrucciones escritas o la ayuda de la aplicación.",
                    "La abro y pruebo los botones hasta entenderla."),
            new Fila("Alguien te pide indicaciones para llegar a un lugar. ¿Cómo prefieres dárselas?",
                    "Imagina cómo le explicarías a un amigo el camino a tu casa.",
                    "Dibujo un mapa sencillo.",
                    "Se lo explico hablando, paso a paso.",
                    "Se lo escribo en una lista de pasos.",
                    "Lo acompaño o le digo que se guíe caminando por los lugares conocidos."),
            new Fila("Vas a cocinar una receta nueva. ¿Qué te ayuda más?",
                    "Piensa en qué te da más seguridad antes de empezar a cocinar.",
                    "Ver fotos o un video del resultado y de cada paso.",
                    "Escuchar a alguien contarme cómo se prepara.",
                    "Leer la receta con calma, con ingredientes y cantidades.",
                    "Ponerme a cocinar y ajustar sobre la marcha."),
            new Fila("Te preparas para un examen. ¿Qué técnica usas con más frecuencia?",
                    "Elige la forma de estudiar que más te funciona, no la que se supone que debes usar.",
                    "Hago esquemas, mapas mentales o subrayo con colores.",
                    "Me explico el tema en voz alta o lo discuto con compañeros.",
                    "Hago resúmenes escritos y los releo.",
                    "Resuelvo ejercicios y ejemplos reales."),
            new Fila("En clase, ¿qué hace que entiendas mejor un tema?",
                    "Recuerda la clase en la que más aprendiste: ¿qué estaba pasando?",
                    "Que se usen gráficos, diagramas o imágenes.",
                    "Que se explique oralmente y se pueda conversar.",
                    "Que haya apuntes y lecturas para repasar.",
                    "Que haya demostraciones y práctica con casos reales."),
            new Fila("Quieres recordar algo importante. ¿Cómo lo recuerdas mejor después?",
                    "Piensa en un recuerdo reciente y en qué parte de él se te quedó más grabada.",
                    "Recuerdo cómo se veía: colores, lugar, imágenes.",
                    "Recuerdo lo que se dijo y cómo se dijo.",
                    "Lo anoto y recuerdo lo que escribí.",
                    "Recuerdo lo que estaba haciendo en ese momento."),
            new Fila("Vas a comprar un aparato nuevo (por ejemplo, unos audífonos). ¿Cómo decides?",
                    "Imagina el proceso que sigues antes de gastar tu dinero.",
                    "Comparo fotos y el diseño.",
                    "Pregunto opiniones o escucho reseñas en audio o video.",
                    "Leo las especificaciones y los comentarios escritos.",
                    "Lo pruebo en la tienda antes de decidir."),
            new Fila("En un trabajo en grupo, ¿qué parte prefieres hacer?",
                    "Piensa en la tarea con la que te sientes más cómodo o cómoda.",
                    "Diseñar la presentación, los gráficos o las imágenes.",
                    "Exponer y explicar las ideas al grupo.",
                    "Redactar el informe o los documentos.",
                    "Construir el prototipo o hacer la parte práctica."),
            new Fila("Un profesor te va a dar retroalimentación sobre tu trabajo. ¿Cómo la prefieres?",
                    "Piensa en la forma de comentarios que más te ha servido para mejorar.",
                    "Con ejemplos marcados directamente sobre mi trabajo.",
                    "En una conversación, para poder preguntar.",
                    "Con comentarios escritos y detallados.",
                    "Con ejemplos prácticos de qué hacer distinto."),
            new Fila("En tu tiempo libre quieres aprender algo nuevo. ¿Qué eliges?",
                    "Elige lo que realmente harías un sábado en la tarde.",
                    "Videos con muchas imágenes o animaciones.",
                    "Podcasts, charlas o clases en audio.",
                    "Libros, artículos o tutoriales escritos.",
                    "Talleres o proyectos donde hago cosas con las manos."),
            new Fila("Quieres entender cómo funciona algo, por ejemplo un motor. ¿Qué te ayuda más?",
                    "Imagina que tienes curiosidad por un aparato y quieres entenderlo bien.",
                    "Ver una animación o un esquema.",
                    "Escuchar la explicación de alguien que sepa.",
                    "Leer un artículo bien explicado.",
                    "Manipular un modelo o una simulación."),
            new Fila("Piensa en tu curso en línea ideal. ¿Cómo sería?",
                    "Cierra los ojos un momento e imagina cómo te gustaría que fuera cada clase.",
                    "Con infografías y videos.",
                    "Con clases en audio, podcasts y conversación.",
                    "Con textos claros, resúmenes y glosarios.",
                    "Con simulaciones y actividades interactivas."));

    private BancoVark() {}

    public static List<PreguntaVark> preguntas() {
        List<PreguntaVark> resultado = new ArrayList<>();
        for (int i = 0; i < FILAS.size(); i++) {
            Fila f = FILAS.get(i);
            List<OpcionVark> opciones = new ArrayList<>(List.of(
                    new OpcionVark(EstiloVark.VISUAL, f.v()),
                    new OpcionVark(EstiloVark.AUDITIVO, f.a()),
                    new OpcionVark(EstiloVark.LECTURA_ESCRITURA, f.r()),
                    new OpcionVark(EstiloVark.KINESTESICO, f.k())));
            Collections.rotate(opciones, i % 4);
            resultado.add(new PreguntaVark(i + 1, f.enunciado(), f.ayuda() + AYUDA_GENERAL, opciones));
        }
        return resultado;
    }
}
