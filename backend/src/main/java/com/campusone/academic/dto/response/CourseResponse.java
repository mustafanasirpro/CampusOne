package com.campusone.academic.dto.response;

import java.util.UUID;

public record CourseResponse(
        UUID id,
        UUID departmentId,
        String courseCode,
        String title,
        Integer recommendedSemester,
        boolean active) {
}
