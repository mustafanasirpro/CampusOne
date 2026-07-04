package com.campusone.ai.dto.response;

import com.campusone.ai.entity.AiGeneratedItemType;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record AiGeneratedItemResponse(
        UUID id,
        AiGeneratedItemType itemType,
        String title,
        String sourceText,
        JsonNode generatedContent,
        Instant createdAt,
        Instant updatedAt) {
}
