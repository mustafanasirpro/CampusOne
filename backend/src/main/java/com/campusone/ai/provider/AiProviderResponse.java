package com.campusone.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;

public record AiProviderResponse(
        String text,
        JsonNode generatedContent,
        String provider) {
}
