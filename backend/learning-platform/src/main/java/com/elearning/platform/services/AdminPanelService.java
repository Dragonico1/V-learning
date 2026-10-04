package com.elearning.platform.services;

import com.elearning.platform.dto.AdminDtos.*;
import com.elearning.platform.entity.RegistroAuditoria;
import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.*;
import com.elearning.platform.repository.CursoRepository;
import com.elearning.platform.repository.InscripcionRepository;
import com.elearning.platform.repository.RegistroAuditoriaRepository;
import com.elearning.platform.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/** Resumen de la plataforma y consulta de la auditoría para el administrador. */
@Service
@RequiredArgsConstructor
public class AdminPanelService {

    private final CursoRepository cursos;
    private final UsuarioRepository usuarios;
    private final InscripcionRepository inscripciones;
    private final RegistroAuditoriaRepository auditoria;

    @Transactional(readOnly = true)
    public AdminDashboard dashboard() {
        BigDecimal avance = inscripciones.promedioAvance();
        return new AdminDashboard(
                cursos.countByEstado(EstadoCurso.PUBLICADO), cursos.countByEstado(EstadoCurso.BORRADOR),
                cursos.countByEstado(EstadoCurso.ARCHIVADO),
                usuarios.countByRolAndEstado(RolUsuario.ESTUDIANTE, EstadoUsuario.ACTIVO),
                usuarios.countByRolAndEstado(RolUsuario.INSTRUCTOR, EstadoUsuario.ACTIVO),
                inscripciones.countByEstado(EstadoInscripcion.ACTIVA), inscripciones.countByEstado(EstadoInscripcion.FINALIZADA),
                avance == null ? BigDecimal.ZERO.setScale(1) : avance.setScale(1, RoundingMode.HALF_UP),
                usuarios.countByRolAndEstado(RolUsuario.ESTUDIANTE, EstadoUsuario.BLOQUEADO)
                        + usuarios.countByRolAndEstado(RolUsuario.INSTRUCTOR, EstadoUsuario.BLOQUEADO));
    }

    @Transactional(readOnly = true)
    public PaginaAuditoria auditoria(Long usuarioId, String accion, ResultadoAuditoria resultado, LocalDate desde,
                                     LocalDate hasta, int pagina, int tamano) {
        Specification<RegistroAuditoria> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (usuarioId != null) p.add(cb.equal(root.get("usuarioId"), usuarioId));
            if (accion != null && !accion.isBlank()) p.add(cb.like(cb.lower(root.get("accion")), "%" + accion.trim().toLowerCase() + "%"));
            if (resultado != null) p.add(cb.equal(root.get("resultado"), resultado));
            if (desde != null) p.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde.atStartOfDay()));
            if (hasta != null) p.add(cb.lessThan(root.get("fecha"), hasta.plusDays(1).atStartOfDay()));
            return cb.and(p.toArray(new Predicate[0]));
        };
        int size = Math.min(Math.max(tamano, 1), 100);
        Page<RegistroAuditoria> r = auditoria.findAll(spec, PageRequest.of(Math.max(pagina, 0), size, Sort.by(Sort.Direction.DESC, "fecha", "id")));
        Set<Long> ids = new HashSet<>();
        r.forEach(x -> { if (x.getUsuarioId() != null) ids.add(x.getUsuarioId()); });
        Map<Long, String> correos = new HashMap<>();
        for (Usuario u : usuarios.findAllById(ids)) correos.put(u.getId(), u.getCorreoInstitucional());
        List<RegistroAuditoriaDto> lista = r.stream().map(x -> new RegistroAuditoriaDto(x.getId(), x.getUsuarioId(),
                correos.get(x.getUsuarioId()), x.getAccion(), x.getFecha(), x.getIpOrigen(), x.getResultado(), x.getRecurso())).toList();
        return new PaginaAuditoria(lista, r.getNumber(), r.getSize(), r.getTotalElements());
    }
}
