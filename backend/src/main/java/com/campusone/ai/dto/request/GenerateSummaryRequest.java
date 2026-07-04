package com.campusone.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateSummaryRequest(
        @Size(min = 3, max = 160)
        String title,

        @NotBlank
        @Size(min = 20, max = 10000)
        String text) {

    public GenerateSummaryRequest {
        title = optional(title);
        text = trim(text);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
