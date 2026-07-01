package com.campusone.academic.repository;

import com.campusone.academic.entity.University;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University, UUID> {

    Optional<University> findByShortNameIgnoreCase(String shortName);

    boolean existsByNameIgnoreCase(String name);
}
