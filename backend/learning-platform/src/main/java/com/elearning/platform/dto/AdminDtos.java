package com.elearning.platform.dto;

import com.elearning.platform.enums.ResultadoAuditoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs del panel de administración. */
public final class AdminDtos {

    private AdminDtos() {}

    public record AdminDashboard(long cursosPublicados, long cursosBorrador, long cursosArchivados, long estudiantesActivos,
                                 long instructoresActivos, long inscripcionesActivas, long inscripcionesFinalizadas,
                                 BigDecimal avancePromedio, long usuariosBloqueados) {}

    public record RegistroAuditoriaDto(Long id, Long usuarioId, String usuarioCorreo, String accion, LocalDateTime fecha,
                                       String ipOrigen, ResultadoAuditoria resultado, String recurso) {}

    public record PaginaAuditoria(List<RegistroAuditoriaDto> registros, int pagina, int tamano, long total) {}
}
