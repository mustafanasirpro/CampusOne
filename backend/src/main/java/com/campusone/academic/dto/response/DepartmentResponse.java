package com.campusone.academic.dto.response;

import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        UUID universityId,
        String name,
        String code,
        boolean active) {
}
