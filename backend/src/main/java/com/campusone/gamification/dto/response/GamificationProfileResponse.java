package com.campusone.gamification.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GamificationProfileResponse(
        UUID userId,
        String fullName,
        int totalXp,
        int level,
        int currentStreak,
        int longestStreak,
        Instant lastActivityAt,
        List<UserBadgeResponse> badges,
        Instant createdAt,
        Instant updatedAt) {
}
