package com.campusone.moderation.dto.response;

import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.entity.ReportReason;
import com.campusone.moderation.entity.ReportStatus;
import java.time.Instant;
import java.util.UUID;

public record ContentReportDetailResponse(
        UUID id,
        ReporterSummaryResponse reporter,
        ModerationTargetType targetType,
        UUID targetId,
        ReportReason reason,
        String details,
        ReportStatus status,
        ModeratorSummaryResponse reviewedBy,
        Instant reviewedAt,
        String resolutionNote,
        Instant createdAt,
        Instant updatedAt) {
}
