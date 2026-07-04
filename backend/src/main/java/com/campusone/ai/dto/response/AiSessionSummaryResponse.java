package com.campusone.ai.dto.response;

import com.campusone.ai.entity.AiSessionMode;
import java.time.Instant;
import java.util.UUID;

public record AiSessionSummaryResponse(
        UUID id,
        String title,
        AiSessionMode mode,
        Instant createdAt,
        Instant updatedAt) {
}
