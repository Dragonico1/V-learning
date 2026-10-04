package com.elearning.platform.repository;

import com.elearning.platform.entity.PosicionRanking;
import com.elearning.platform.entity.PosicionRankingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PosicionRankingRepository extends JpaRepository<PosicionRanking, PosicionRankingId> {

    @Query("select p from PosicionRanking p join fetch p.estudiante where p.rankingId = :rankingId order by p.posicion")
    List<PosicionRanking> delRanking(@Param("rankingId") Long rankingId);

    @Modifying
    @Query("delete from PosicionRanking p where p.rankingId = :rankingId")
    int borrarDelRanking(@Param("rankingId") Long rankingId);
}
