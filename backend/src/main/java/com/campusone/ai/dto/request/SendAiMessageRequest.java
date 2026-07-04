package com.campusone.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendAiMessageRequest(
        @NotBlank
        @Size(max = 5000)
        String content) {

    public SendAiMessageRequest {
        content = trim(content);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
