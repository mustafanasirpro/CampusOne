package com.campusone.academic.repository;

import com.campusone.academic.entity.Department;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    List<Department> findAllByUniversityIdOrderByNameAsc(UUID universityId);

    Optional<Department> findByUniversityIdAndCodeIgnoreCase(UUID universityId, String code);
}
