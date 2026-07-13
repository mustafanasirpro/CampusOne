package com.campusone.lostfound.dto.response;

import java.util.UUID;

public record LostFoundReporterResponse(
        UUID userId,
        String fullName,
        String avatarUrl) {
}
