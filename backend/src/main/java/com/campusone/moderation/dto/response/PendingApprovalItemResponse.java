package com.campusone.moderation.dto.response;

import com.campusone.moderation.entity.ModerationTargetType;
import java.time.Instant;
import java.util.UUID;

public record PendingApprovalItemResponse(
        UUID id,
        ModerationTargetType targetType,
        String title,
        String description,
        ReporterSummaryResponse submittedBy,
        Instant submittedAt,
        String status,
        String previewUrl,
        String detailUrl) {
}
