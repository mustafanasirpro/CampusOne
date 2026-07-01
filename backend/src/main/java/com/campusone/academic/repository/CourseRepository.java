package com.campusone.academic.repository;

import com.campusone.academic.entity.Course;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findAllByDepartmentIdOrderByCourseCodeAsc(UUID departmentId);

    Optional<Course> findByDepartmentIdAndCourseCodeIgnoreCase(
            UUID departmentId,
            String courseCode);
}
