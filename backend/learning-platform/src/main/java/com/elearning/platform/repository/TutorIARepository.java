package com.elearning.platform.repository;

import com.elearning.platform.entity.TutorIA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TutorIARepository extends JpaRepository<TutorIA, Long> {

    Optional<TutorIA> findFirstByActivoTrueOrderByIdAsc();
}
