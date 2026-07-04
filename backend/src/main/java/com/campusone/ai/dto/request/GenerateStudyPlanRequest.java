package com.campusone.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GenerateStudyPlanRequest(
        @NotBlank
        @Size(min = 5, max = 500)
        String goal,

        @NotNull
        @Min(1)
        @Max(90)
        Integer days,

        @NotNull
        @Min(10)
        @Max(600)
        Integer dailyMinutes,

        @Size(max = 5000)
        String context) {

    public GenerateStudyPlanRequest {
        goal = trim(goal);
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
