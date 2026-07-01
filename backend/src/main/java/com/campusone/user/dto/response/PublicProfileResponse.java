package com.campusone.user.dto.response;

import java.util.List;
import java.util.UUID;

public record PublicProfileResponse(
        UUID userId,
        String fullName,
        String bio,
        String university,
        String department,
        int semester,
        String avatarUrl,
        String coverImageUrl,
        String location,
        List<String> skills,
        long totalXp) {
}
