package com.elearning.platform.services;

import com.elearning.platform.dto.InformeDtos.*;
import com.elearning.platform.entity.*;
import com.elearning.platform.enums.*;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.*;
import com.elearning.platform.security.UsuarioPrincipal;
import com.elearning.platform.util.ExportadorInforme;
import com.elearning.platform.util.TablaInforme;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * RF-012 Informes: administrador (toda la plataforma) e instructor (solo sus cursos), con filtros por estudiante,
 * curso y periodo, uso agregado de accesibilidad (sin nombres) y exportación PDF/Excel. RF-006: el estudiante
 * exporta su propio progreso.
 */
@Service
@RequiredArgsConstructor
public class InformeService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final InscripcionRepository inscripciones;
    private final IntentoEvaluacionRepository intentos;
    private final PerfilAccesibilidadRepository perfilesAccesibilidad;
    private final InformeRepository informes;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    private final ObjectMapper mapper;

    // ------------------------------------------------------------------ vista y exportación

    @Transactional(readOnly = true)
    public VistaInforme vista(UsuarioPrincipal u, InformeRequest req) {
        TablaInforme t = construir(u, req);
        return new VistaInforme(t.titulo(), t.contexto(), t.columnas(), t.filas(), t.notas(), t.sinDatos(),
                t.sinDatos() ? "No hay datos para los filtros elegidos." : null);
    }

    @Transactional
    public ArchivoGenerado generar(UsuarioPrincipal u, InformeRequest req, String ip) {
        TablaInforme t = construir(u, req);
        if (t.sinDatos()) throw ApiException.noEncontrado("No hay datos para los filtros elegidos.");
        ArchivoGenerado archivo = renderizar(t, req.formato(), "informe");
        guardar(u, req);
        auditoria.registrar(u.id(), "INFORME_GENERADO", "informes/" + req.formato(), ResultadoAuditoria.PERMITIDO, ip);
        return archivo;
    }

    /** RF-006: el estudiante descarga su propio avance. */
    @Transactional
    public ArchivoGenerado exportarProgresoPropio(UsuarioPrincipal u, FormatoInforme formato, String ip) {
        TablaInforme t = tablaEstudiante(u.id(), u.nombre());
        if (t.sinDatos()) throw ApiException.noEncontrado("Aún no tienes cursos para exportar.");
        ArchivoGenerado archivo = renderizar(t, formato, "mi-progreso");
        guardar(u, new InformeRequest(formato, u.id(), null, null, null));
        auditoria.registrar(u.id(), "PROGRESO_EXPORTADO", "progreso/" + formato, ResultadoAuditoria.PERMITIDO, ip);
        return archivo;
    }

    @Transactional(readOnly = true)
    public List<InformeHistorial> historial(Long usuarioId) {
        return informes.findByUsuarioIdOrderByFechaGeneracionDesc(usuarioId, PageRequest.of(0, 50)).stream()
                .map(i -> new InformeHistorial(i.getId(), i.getFechaGeneracion(), i.getFormato(), i.getFiltros())).toList();
    }

    // ------------------------------------------------------------------ construcción

    private TablaInforme construir(UsuarioPrincipal u, InformeRequest req) {
        if (u.rol() == RolUsuario.ESTUDIANTE) throw ApiException.prohibido("ACCESO_DENEGADO", "No tienes permiso para generar informes.");
        if (req.desde() != null && req.hasta() != null && req.desde().isAfter(req.hasta())) {
            throw ApiException.solicitudInvalida("PERIODO_INVALIDO", "La fecha inicial no puede ser posterior a la final.");
        }
        boolean admin = u.rol() == RolUsuario.ADMINISTRADOR;
        List<Inscripcion> base = new ArrayList<>();
        for (Inscripcion i : inscripciones.conDetalle()) {
            Curso c = i.getCurso();
            if (!admin && !c.getInstructor().getId().equals(u.id())) continue;
            if (req.cursoId() != null && !c.getId().equals(req.cursoId())) continue;
            if (req.estudianteId() != null && !i.getEstudiante().getId().equals(req.estudianteId())) continue;
            LocalDate f = i.getFechaInscripcion().toLocalDate();
            if (req.desde() != null && f.isBefore(req.desde())) continue;
            if (req.hasta() != null && f.isAfter(req.hasta())) continue;
            base.add(i);
        }
        if (!admin && req.cursoId() != null && base.isEmpty()
                && inscripciones.conDetalle().stream().anyMatch(i -> i.getCurso().getId().equals(req.cursoId())
                && !i.getCurso().getInstructor().getId().equals(u.id()))) {
            throw ApiException.prohibido("ACCESO_DENEGADO", "Ese curso no es tuyo.");
        }

        Map<Long, Map<Long, BigDecimal>> promedios = new HashMap<>();
        List<List<String>> filas = new ArrayList<>();
        BigDecimal suma = BigDecimal.ZERO;
        for (Inscripcion i : base) {
            Long cursoId = i.getCurso().getId();
            Map<Long, BigDecimal> porEstudiante = promedios.computeIfAbsent(cursoId, this::promedioNotasPorEstudiante);
            BigDecimal nota = porEstudiante.get(i.getEstudiante().getId());
            suma = suma.add(i.getPorcentajeCompletado());
            filas.add(List.of(i.getEstudiante().getNombre(), i.getEstudiante().getCodigoEstudiante(), i.getCurso().getTitulo(),
                    i.getEstado().name(), i.getPorcentajeCompletado().setScale(1, RoundingMode.HALF_UP) + " %",
                    nota == null ? "—" : nota + " %", i.getFechaInscripcion().format(FECHA)));
        }

        List<String> contexto = new ArrayList<>();
        contexto.add("Generado: " + LocalDateTime.now().format(FECHA) + " por " + u.nombre()
                + (admin ? " (administrador: toda la plataforma)" : " (instructor: sus cursos)"));
        contexto.add("Filtros: " + describirFiltros(req));
        if (!filas.isEmpty()) {
            contexto.add("Inscripciones: " + filas.size() + " · Avance promedio: "
                    + suma.divide(BigDecimal.valueOf(filas.size()), 1, RoundingMode.HALF_UP) + " %");
        }

        List<String> notas = new ArrayList<>(usoAccesibilidad(base));
        return new TablaInforme("V-Learning · Informe de avance y rendimiento", contexto,
                List.of("Estudiante", "Código", "Curso", "Estado", "Avance", "Promedio evaluaciones", "Inscripción"), filas, notas);
    }

    private TablaInforme tablaEstudiante(Long estudianteId, String nombre) {
        List<List<String>> filas = new ArrayList<>();
        Map<Long, Map<Long, BigDecimal>> promedios = new HashMap<>();
        for (Inscripcion i : inscripciones.delEstudiante(estudianteId)) {
            BigDecimal nota = promedios.computeIfAbsent(i.getCurso().getId(), this::promedioNotasPorEstudiante).get(estudianteId);
            filas.add(List.of(i.getCurso().getTitulo(), i.getEstado().name(),
                    i.getPorcentajeCompletado().setScale(1, RoundingMode.HALF_UP) + " %", nota == null ? "—" : nota + " %",
                    i.getFechaInscripcion().format(FECHA)));
        }
        return new TablaInforme("V-Learning · Mi progreso", List.of("Estudiante: " + nombre, "Generado: " + LocalDateTime.now().format(FECHA)),
                List.of("Curso", "Estado", "Avance", "Promedio evaluaciones", "Inscripción"), filas, List.of());
    }

    /** Promedio (en %) de la mejor nota de cada evaluación, por estudiante. */
    private Map<Long, BigDecimal> promedioNotasPorEstudiante(Long cursoId) {
        Map<Long, List<BigDecimal>> acumulado = new HashMap<>();
        for (Object[] f : intentos.mejoresPorCurso(cursoId)) {
            BigDecimal mejor = (BigDecimal) f[2];
            BigDecimal max = (BigDecimal) f[3];
            if (mejor == null || max == null || max.signum() == 0) continue;
            acumulado.computeIfAbsent((Long) f[0], k -> new ArrayList<>())
                    .add(mejor.multiply(CIEN).divide(max, 2, RoundingMode.HALF_UP));
        }
        Map<Long, BigDecimal> res = new HashMap<>();
        acumulado.forEach((est, lista) -> res.put(est, lista.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(lista.size()), 1, RoundingMode.HALF_UP)));
        return res;
    }

    /** Conteo agregado por categoría entre los estudiantes del informe; nunca identifica personas. */
    private List<String> usoAccesibilidad(List<Inscripcion> base) {
        Set<Long> ids = new HashSet<>();
        for (Inscripcion i : base) ids.add(i.getEstudiante().getId());
        if (ids.isEmpty()) return List.of();
        EnumMap<CategoriaAccesibilidad, Integer> conteo = new EnumMap<>(CategoriaAccesibilidad.class);
        for (PerfilAccesibilidad p : perfilesAccesibilidad.findAll()) {
            if (!ids.contains(p.getEstudiante().getId())) continue;
            for (CategoriaAccesibilidad c : p.getCategorias()) conteo.merge(c, 1, Integer::sum);
        }
        if (conteo.isEmpty()) return List.of("Uso de accesibilidad: ningún estudiante del informe tiene categorías activas.");
        StringBuilder sb = new StringBuilder("Uso de accesibilidad (estudiantes por categoría, de " + ids.size() + "): ");
        conteo.forEach((c, n) -> sb.append(c.name()).append(" ").append(n).append(" · "));
        return List.of(sb.substring(0, sb.length() - 3));
    }

    private String describirFiltros(InformeRequest r) {
        List<String> p = new ArrayList<>();
        if (r.estudianteId() != null) p.add("estudiante #" + r.estudianteId());
        if (r.cursoId() != null) p.add("curso #" + r.cursoId());
        if (r.desde() != null) p.add("desde " + r.desde());
        if (r.hasta() != null) p.add("hasta " + r.hasta());
        return p.isEmpty() ? "ninguno" : String.join(", ", p);
    }

    private ArchivoGenerado renderizar(TablaInforme t, FormatoInforme formato, String base) {
        String sello = LocalDate.now().toString();
        if (formato == FormatoInforme.PDF) {
            return new ArchivoGenerado(base + "-" + sello + ".pdf", "application/pdf", ExportadorInforme.aPdf(t));
        }
        return new ArchivoGenerado(base + "-" + sello + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ExportadorInforme.aExcel(t));
    }

    private void guardar(UsuarioPrincipal u, InformeRequest req) {
        Informe i = new Informe();
        i.setUsuario(usuarios.getReferenceById(u.id()));
        i.setFormato(req.formato());
        try {
            i.setFiltros(mapper.writeValueAsString(Map.of("estudianteId", Objects.toString(req.estudianteId(), ""),
                    "cursoId", Objects.toString(req.cursoId(), ""), "desde", Objects.toString(req.desde(), ""),
                    "hasta", Objects.toString(req.hasta(), ""))));
        } catch (JsonProcessingException e) {
            i.setFiltros(describirFiltros(req));
        }
        informes.save(i);
    }
}
