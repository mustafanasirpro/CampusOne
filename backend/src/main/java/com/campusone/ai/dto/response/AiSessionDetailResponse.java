package com.campusone.ai.dto.response;

import com.campusone.ai.entity.AiSessionMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiSessionDetailResponse(
        UUID id,
        String title,
        AiSessionMode mode,
        List<AiMessageResponse> messages,
        Instant createdAt,
        Instant updatedAt) {
}
