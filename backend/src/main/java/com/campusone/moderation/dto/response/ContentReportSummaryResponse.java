package com.campusone.moderation.dto.response;

import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.entity.ReportReason;
import com.campusone.moderation.entity.ReportStatus;
import java.time.Instant;
import java.util.UUID;

public record ContentReportSummaryResponse(
        UUID id,
        ModerationTargetType targetType,
        UUID targetId,
        ReportReason reason,
        ReportStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
