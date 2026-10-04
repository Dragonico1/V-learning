package com.elearning.platform.services;

import com.elearning.platform.entity.Contenido;
import com.elearning.platform.entity.Curso;
import com.elearning.platform.entity.Modulo;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.ContenidoRepository;
import com.elearning.platform.repository.CursoRepository;
import com.elearning.platform.repository.ModuloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Comprueba que un instructor solo toque lo suyo (autorización por propiedad, RF-013). */
@Service
@RequiredArgsConstructor
public class AccesoService {

    private final CursoRepository cursos;
    private final ModuloRepository modulos;
    private final ContenidoRepository contenidos;

    @Transactional(propagation = Propagation.REQUIRED)
    public Curso cursoPropio(Long cursoId, Long instructorId) {
        Curso c = cursos.findById(cursoId).orElseThrow(() -> ApiException.noEncontrado("No encontramos ese curso."));
        exigirPropietario(c, instructorId);
        return c;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Modulo moduloPropio(Long moduloId, Long instructorId) {
        Modulo m = modulos.findById(moduloId).orElseThrow(() -> ApiException.noEncontrado("No encontramos ese módulo."));
        exigirPropietario(m.getCurso(), instructorId);
        return m;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Contenido contenidoPropio(Long contenidoId, Long instructorId) {
        Contenido c = contenidos.findById(contenidoId)
                .orElseThrow(() -> ApiException.noEncontrado("No encontramos ese contenido."));
        exigirPropietario(c.getModulo().getCurso(), instructorId);
        return c;
    }

    private static void exigirPropietario(Curso curso, Long instructorId) {
        if (!curso.getInstructor().getId().equals(instructorId)) {
            throw ApiException.prohibido("CURSO_AJENO", "Este curso pertenece a otro instructor.");
        }
    }
}
