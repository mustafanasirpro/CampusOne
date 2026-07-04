package com.campusone.ai.dto.response;

import com.campusone.ai.entity.AiMessageRole;
import java.time.Instant;
import java.util.UUID;

public record AiMessageResponse(
        UUID id,
        AiMessageRole role,
        String content,
        int tokenEstimate,
        Instant createdAt) {
}
