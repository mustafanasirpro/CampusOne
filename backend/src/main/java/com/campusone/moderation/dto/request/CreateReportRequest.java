package com.campusone.moderation.dto.request;

import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateReportRequest(
        @NotNull
        ModerationTargetType targetType,

        @NotNull
        UUID targetId,

        @NotNull
        ReportReason reason,

        @Size(max = 1000)
        String details) {

    public CreateReportRequest {
        details = normalizeOptional(details);
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
