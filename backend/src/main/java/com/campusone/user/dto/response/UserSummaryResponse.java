package com.campusone.user.dto.response;

import java.util.UUID;

public record UserSummaryResponse(
        UUID userId,
        String fullName,
        String avatarUrl,
        String university,
        String department) {
}
