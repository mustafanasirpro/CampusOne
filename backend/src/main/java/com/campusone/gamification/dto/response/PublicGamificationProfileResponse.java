package com.campusone.gamification.dto.response;

import java.util.List;
import java.util.UUID;

public record PublicGamificationProfileResponse(
        UUID userId,
        String fullName,
        int totalXp,
        int level,
        List<BadgeResponse> badges) {
}
