package com.campusone.user.dto.response;

import com.campusone.academic.dto.response.DepartmentResponse;
import com.campusone.academic.dto.response.UniversityResponse;
import com.campusone.user.entity.ProfileVisibility;
import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String fullName,
        String email,
        String bio,
        UniversityResponse university,
        DepartmentResponse department,
        int semester,
        String avatarUrl,
        String coverImageUrl,
        String location,
        List<String> skills,
        ProfileVisibility visibility,
        long totalXp,
        PreferenceResponse preferences) {
}
