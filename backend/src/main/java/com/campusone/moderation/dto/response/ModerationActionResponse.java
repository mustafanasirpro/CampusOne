package com.campusone.moderation.dto.response;

import com.campusone.moderation.entity.ModerationActionType;
import com.campusone.moderation.entity.ModerationTargetType;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ModerationActionResponse(
        UUID id,
        ModeratorSummaryResponse moderator,
        UUID reportId,
        ModerationActionType actionType,
        ModerationTargetType targetType,
        UUID targetId,
        String reason,
        JsonNode metadata,
        Instant createdAt) {
}
