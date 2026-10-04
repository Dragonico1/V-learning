package com.elearning.platform.services;

import com.elearning.platform.dto.AccesibilidadDtos.*;
import com.elearning.platform.entity.ConfiguracionAccesibilidad;
import com.elearning.platform.entity.PerfilAccesibilidad;
import com.elearning.platform.enums.CategoriaAccesibilidad;
import com.elearning.platform.enums.ResultadoAuditoria;
import com.elearning.platform.exception.ApiException;
import com.elearning.platform.repository.ConfiguracionAccesibilidadRepository;
import com.elearning.platform.repository.EstudianteRepository;
import com.elearning.platform.repository.PerfilAccesibilidadRepository;
import com.elearning.platform.util.ContrasteWcag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RF-014 Perfil de accesibilidad (categorías, previsualizar, ajuste manual) y RF-016 visualización
 * (validar, advertir, restablecer). Cada guardado agrega una fila a configuraciones_accesibilidad
 * (historial); la vigente es la más reciente.
 */
@Service
@RequiredArgsConstructor
public class AccesibilidadService {

    private final PerfilAccesibilidadRepository perfiles;
    private final ConfiguracionAccesibilidadRepository configuraciones;
    private final EstudianteRepository estudiantes;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public PerfilAccesibilidadRespuesta obtener(Long estudianteId) {
        Optional<PerfilAccesibilidad> perfil = perfiles.findByEstudianteId(estudianteId);
        if (perfil.isEmpty()) {
            return new PerfilAccesibilidadRespuesta(false, Set.of(), ReglasAccesibilidad.base(), null);
        }
        PerfilAccesibilidad p = perfil.get();
        return new PerfilAccesibilidadRespuesta(true, Set.copyOf(p.getCategorias()), configuracionDe(p), p.getFechaActualizacion());
    }

    /** Categorías vigentes del estudiante (vacío si no configuró nada). Lo usan otros servicios. */
    @Transactional(readOnly = true)
    public Set<CategoriaAccesibilidad> categoriasDe(Long estudianteId) {
        return perfiles.findByEstudianteId(estudianteId)
                .map(p -> p.getCategorias().isEmpty() ? EnumSet.noneOf(CategoriaAccesibilidad.class)
                        : EnumSet.copyOf(p.getCategorias()))
                .orElse(EnumSet.noneOf(CategoriaAccesibilidad.class));
    }

    @Transactional(readOnly = true)
    public ConfiguracionDto configuracionDe(Long estudianteId) {
        return perfiles.findByEstudianteId(estudianteId).map(this::configuracionDe)
                .orElse(ReglasAccesibilidad.base());
    }

    public PrevisualizacionRespuesta previsualizar(Set<CategoriaAccesibilidad> categorias) {
        return new PrevisualizacionRespuesta(categorias, ReglasAccesibilidad.porCategorias(categorias));
    }

    /** Guarda las categorías y activa automáticamente los ajustes de cada una. [] = «Ninguna». */
    @Transactional
    public PerfilAccesibilidadRespuesta guardarCategorias(Long estudianteId, Set<CategoriaAccesibilidad> categorias, String ip) {
        PerfilAccesibilidad perfil = perfilDe(estudianteId);
        perfil.getCategorias().clear();
        perfil.getCategorias().addAll(categorias);
        perfiles.save(perfil);
        insertarConfiguracion(perfil, ReglasAccesibilidad.porCategorias(categorias));
        auditoria.registrar(estudianteId, "ACCESIBILIDAD_PERFIL_GUARDADO", categorias.toString(),
                ResultadoAuditoria.PERMITIDO, ip);
        return obtener(estudianteId);
    }

    /** Ajuste manual de cada opción (RF-014) y panel de visualización (RF-016). */
    @Transactional
    public ConfiguracionRespuesta guardarConfiguracion(Long estudianteId, ConfiguracionDto dto, String ip) {
        validar(dto);
        PerfilAccesibilidad perfil = perfilDe(estudianteId);
        insertarConfiguracion(perfil, dto);
        Set<CategoriaAccesibilidad> categorias = perfil.getCategorias().isEmpty()
                ? EnumSet.noneOf(CategoriaAccesibilidad.class) : EnumSet.copyOf(perfil.getCategorias());
        List<String> avisos = ReglasAccesibilidad.advertencias(dto, categorias);
        auditoria.registrar(estudianteId, "ACCESIBILIDAD_CONFIGURACION_GUARDADA", "avisos=" + avisos.size(),
                ResultadoAuditoria.PERMITIDO, ip);
        return new ConfiguracionRespuesta(dto, avisos, avisos.isEmpty() ? null : ReglasAccesibilidad.sugerir(dto, categorias));
    }

    /** Vuelve a los ajustes automáticos de las categorías elegidas (o a los valores base si no hay). */
    @Transactional
    public ConfiguracionDto restablecer(Long estudianteId, String ip) {
        PerfilAccesibilidad perfil = perfilDe(estudianteId);
        Set<CategoriaAccesibilidad> categorias = perfil.getCategorias().isEmpty()
                ? EnumSet.noneOf(CategoriaAccesibilidad.class) : EnumSet.copyOf(perfil.getCategorias());
        ConfiguracionDto dto = ReglasAccesibilidad.porCategorias(categorias);
        insertarConfiguracion(perfil, dto);
        auditoria.registrar(estudianteId, "ACCESIBILIDAD_RESTABLECIDA", "perfiles-accesibilidad/" + perfil.getId(),
                ResultadoAuditoria.PERMITIDO, ip);
        return dto;
    }

    /** Temas disponibles con su contraste calculado (validación WCAG 2.1 AA). */
    public List<TemaRespuesta> temas() {
        return List.of(
                tema("normal", "Normal", "#1F2937", "#FFFFFF"),
                tema("alto-contraste", "Alto contraste (blanco sobre negro)", "#FFFFFF", "#000000"),
                tema("alto-contraste-amarillo", "Alto contraste (amarillo sobre negro)", "#FFFF00", "#000000"));
    }

    // ------------------------------------------------------------------

    private static TemaRespuesta tema(String id, String nombre, String texto, String fondo) {
        double ratio = Math.round(ContrasteWcag.ratio(texto, fondo) * 100.0) / 100.0;
        return new TemaRespuesta(id, nombre, texto, fondo, ratio,
                ratio >= ContrasteWcag.MINIMO_AA, ratio >= ContrasteWcag.MINIMO_AAA);
    }

    private void validar(ConfiguracionDto dto) {
        if (dto.tipografia() != null && !ReglasAccesibilidad.TIPOGRAFIAS.contains(dto.tipografia())) {
            throw ApiException.solicitudInvalida("TIPOGRAFIA_NO_DISPONIBLE",
                    "La tipografía debe ser una de: " + String.join(", ", ReglasAccesibilidad.TIPOGRAFIAS) + ".");
        }
    }

    private PerfilAccesibilidad perfilDe(Long estudianteId) {
        return perfiles.findByEstudianteId(estudianteId).orElseGet(() -> {
            PerfilAccesibilidad nuevo = new PerfilAccesibilidad();
            nuevo.setEstudiante(estudiantes.getReferenceById(estudianteId));
            return perfiles.save(nuevo);
        });
    }

    private ConfiguracionDto configuracionDe(PerfilAccesibilidad perfil) {
        return configuraciones.findFirstByPerfilAccesibilidadIdOrderByFechaConfiguracionDescIdDesc(perfil.getId())
                .map(AccesibilidadService::aDto).orElse(ReglasAccesibilidad.base());
    }

    private void insertarConfiguracion(PerfilAccesibilidad perfil, ConfiguracionDto dto) {
        ConfiguracionAccesibilidad c = new ConfiguracionAccesibilidad();
        c.setPerfilAccesibilidad(perfil);
        c.setAltoContraste(dto.altoContraste());
        c.setTamanoFuente(dto.tamanoFuente());
        c.setTipografia(dto.tipografia());
        c.setEspaciadoLinea(dto.espaciadoLinea());
        c.setNavegacionTeclado(dto.navegacionTeclado());
        c.setLectorPantalla(dto.lectorPantalla());
        c.setSubtitulos(dto.subtitulos());
        c.setTranscripcion(dto.transcripcion());
        c.setTextoAVoz(dto.textoAVoz());
        c.setTiempoAdicional(dto.tiempoAdicional());
        configuraciones.save(c);
    }

    private static ConfiguracionDto aDto(ConfiguracionAccesibilidad c) {
        return new ConfiguracionDto(c.isAltoContraste(), c.getTamanoFuente(), c.getTipografia(), c.getEspaciadoLinea(),
                c.isNavegacionTeclado(), c.isLectorPantalla(), c.isSubtitulos(), c.isTranscripcion(),
                c.isTextoAVoz(), c.getTiempoAdicional());
    }
}
