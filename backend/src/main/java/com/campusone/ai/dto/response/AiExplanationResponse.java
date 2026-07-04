package com.campusone.ai.dto.response;

public record AiExplanationResponse(
        String concept,
        String explanation,
        String provider) {
}
