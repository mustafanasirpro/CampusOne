package com.campusone.gamification.dto.response;

import com.campusone.gamification.entity.GamificationActionType;
import com.campusone.gamification.entity.GamificationSourceType;
import java.time.Instant;
import java.util.UUID;

public record XpTransactionResponse(
        UUID id,
        GamificationActionType actionType,
        int points,
        GamificationSourceType sourceType,
        UUID sourceId,
        String description,
        Instant createdAt) {
}
