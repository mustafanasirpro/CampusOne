package com.campusone.moderation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DismissReportRequest(
        @NotBlank
        @Size(min = 3, max = 1000)
        String resolutionNote) {

    public DismissReportRequest {
        resolutionNote = trim(resolutionNote);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
