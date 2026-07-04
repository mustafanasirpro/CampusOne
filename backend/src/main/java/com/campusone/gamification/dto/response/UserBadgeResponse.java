package com.campusone.gamification.dto.response;

import com.campusone.gamification.entity.GamificationSourceType;
import java.time.Instant;
import java.util.UUID;

public record UserBadgeResponse(
        BadgeResponse badge,
        Instant awardedAt,
        GamificationSourceType sourceType,
        UUID sourceId) {
}
