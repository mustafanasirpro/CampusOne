package com.campusone.academic.mapper;

import com.campusone.academic.dto.response.CourseResponse;
import com.campusone.academic.entity.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getDepartment().getId(),
                course.getCourseCode(),
                course.getTitle(),
                course.getRecommendedSemester(),
                course.isActive());
    }
}
