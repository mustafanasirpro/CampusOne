package com.campusone.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateFlashcardsRequest(
        @Size(min = 3, max = 160)
        String title,

        @NotBlank
        @Size(min = 20, max = 10000)
        String text,

        @Min(1)
        @Max(20)
        Integer count) {

    public GenerateFlashcardsRequest {
        title = optional(title);
        text = trim(text);
        count = count == null ? 5 : count;
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
