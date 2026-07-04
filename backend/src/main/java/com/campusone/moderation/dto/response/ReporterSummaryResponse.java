package com.campusone.moderation.dto.response;

import java.util.UUID;

public record ReporterSummaryResponse(
        UUID userId,
        String fullName) {
}
