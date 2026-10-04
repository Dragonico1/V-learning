package com.elearning.platform.repository;

import com.elearning.platform.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RankingRepository extends JpaRepository<Ranking, Long> {

    Optional<Ranking> findByCursoIdAndPeriodo(Long cursoId, String periodo);
}
