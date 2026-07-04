package com.campusone.ai.dto.response;

import com.campusone.ai.entity.AiUsageFeature;
import java.time.Instant;
import java.util.UUID;

public record AiUsageRecordResponse(
        UUID id,
        AiUsageFeature feature,
        int inputTokenEstimate,
        int outputTokenEstimate,
        String provider,
        Instant createdAt) {
}
