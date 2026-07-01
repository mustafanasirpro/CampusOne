package com.campusone.user.dto.response;

import com.campusone.academic.dto.response.DepartmentResponse;
import com.campusone.academic.dto.response.UniversityResponse;
import java.util.Set;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String fullName,
        String email,
        UniversityResponse university,
        DepartmentResponse department,
        int semester,
        Set<String> roles) {
}
