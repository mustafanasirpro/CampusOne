package com.campusone.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExplainConceptRequest(
        @NotBlank
        @Size(min = 2, max = 200)
        String concept,

        @Size(max = 5000)
        String context) {

    public ExplainConceptRequest {
        concept = trim(concept);
        context = optional(context);
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
