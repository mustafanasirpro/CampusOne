package com.campusone.event.dto.response;

import java.time.Instant;
import java.util.UUID;

public record EventParticipantResponse(
        UUID eventId,
        UUID userId,
        boolean joined,
        Instant joinedAt,
        int participantCount) {
}
