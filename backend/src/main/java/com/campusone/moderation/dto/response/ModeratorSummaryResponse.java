package com.campusone.moderation.dto.response;

import java.util.UUID;

public record ModeratorSummaryResponse(
        UUID userId,
        String fullName) {
}
