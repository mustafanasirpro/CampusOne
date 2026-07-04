package com.campusone.gamification.dto.response;

import java.util.UUID;

public record LeaderboardEntryResponse(
        long rank,
        UUID userId,
        String fullName,
        long totalXpForPeriod,
        int allTimeXp,
        int level) {
}
