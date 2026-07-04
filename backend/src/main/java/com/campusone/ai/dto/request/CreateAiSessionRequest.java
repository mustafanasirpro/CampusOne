package com.campusone.ai.dto.request;

import com.campusone.ai.entity.AiSessionMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAiSessionRequest(
        @NotBlank
        @Size(min = 3, max = 160)
        String title,

        @NotNull
        AiSessionMode mode) {

    public CreateAiSessionRequest {
        title = trim(title);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
