package com.campusone.internship.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SavedInternshipResponse(
        UUID internshipId,
        UUID userId,
        boolean saved,
        Instant savedAt) {
}
