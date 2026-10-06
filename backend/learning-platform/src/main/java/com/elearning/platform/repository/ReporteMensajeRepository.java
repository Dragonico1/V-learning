package com.elearning.platform.repository;

import com.elearning.platform.entity.ReporteMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReporteMensajeRepository extends JpaRepository<ReporteMensaje, Long> {

    boolean existsByMensajeIdAndReportanteId(Long mensajeId, Long reportanteId);

    @Query("select r from ReporteMensaje r join fetch r.reportante where r.mensaje.id = :mensajeId order by r.fecha asc")
    List<ReporteMensaje> delMensaje(@Param("mensajeId") Long mensajeId);
}
