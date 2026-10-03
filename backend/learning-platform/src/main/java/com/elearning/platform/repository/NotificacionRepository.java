package com.elearning.platform.repository;

import com.elearning.platform.entity.Notificacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** Notificaciones visibles: las pospuestas no aparecen hasta su fecha (RF-011). */
    @Query("select n from Notificacion n where n.usuario.id = :usuarioId "
            + "and (n.pospuestaHasta is null or n.pospuestaHasta <= :ahora) "
            + "order by n.fechaCreacion desc")
    List<Notificacion> visibles(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora,
                                Pageable pageable);

    @Query("select count(n) from Notificacion n where n.usuario.id = :usuarioId and n.leida = false "
            + "and (n.pospuestaHasta is null or n.pospuestaHasta <= :ahora)")
    long contarNoLeidas(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);

    /** Evita repetir el mismo recordatorio el mismo día. */
    @Query("select count(n) from Notificacion n where n.usuario.id = :usuarioId and n.titulo = :titulo "
            + "and n.fechaCreacion >= :desde")
    long contarPorTituloDesde(@Param("usuarioId") Long usuarioId, @Param("titulo") String titulo,
                              @Param("desde") LocalDateTime desde);
}
