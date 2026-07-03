package com.campusone.internship.dto.response;

import java.util.UUID;

public record InternshipPosterResponse(
        UUID userId,
        String fullName,
        String avatarUrl,
        String university) {
}
