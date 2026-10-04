package com.elearning.platform.repository;

import com.elearning.platform.entity.RespuestaVarkParcial;
import com.elearning.platform.entity.RespuestaVarkParcialId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RespuestaVarkParcialRepository extends JpaRepository<RespuestaVarkParcial, RespuestaVarkParcialId> {

    List<RespuestaVarkParcial> findByPerfilAprendizajeIdOrderByNumeroPregunta(Long perfilAprendizajeId);

    @Modifying
    @Query("delete from RespuestaVarkParcial r where r.perfilAprendizajeId = :perfilId")
    int borrarDelPerfil(@Param("perfilId") Long perfilId);
}
