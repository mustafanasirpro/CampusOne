package com.campusone.moderation.dto.response;

import com.campusone.moderation.entity.ModeratorRole;
import java.time.Instant;

public record ModeratorStatusResponse(
        boolean activeModerator,
        ModeratorRole role,
        Instant assignedAt,
        Instant revokedAt) {
}
