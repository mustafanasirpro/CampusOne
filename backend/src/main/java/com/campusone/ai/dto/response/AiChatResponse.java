package com.campusone.ai.dto.response;

import java.util.UUID;

public record AiChatResponse(
        UUID sessionId,
        AiMessageResponse userMessage,
        AiMessageResponse assistantMessage,
        String provider) {
}
