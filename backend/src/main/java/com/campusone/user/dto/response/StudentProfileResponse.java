package com.campusone.user.dto.response;

import com.campusone.academic.dto.response.DepartmentResponse;
import com.campusone.academic.dto.response.UniversityResponse;
import com.campusone.user.entity.ProfileVisibility;
import java.util.List;
import java.util.UUID;

public record StudentProfileResponse(
        UUID id,
        UUID userId,
        UniversityResponse university,
        DepartmentResponse department,
        String fullName,
        int semester,
        String bio,
        String avatarUrl,
        String coverImageUrl,
        String location,
        List<String> skills,
        ProfileVisibility visibility,
        long totalXp) {
}
