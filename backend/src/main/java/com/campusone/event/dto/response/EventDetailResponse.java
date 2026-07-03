package com.campusone.event.dto.response;

import com.campusone.event.entity.EventStatus;
import com.campusone.event.entity.EventVisibility;
import java.time.Instant;
import java.util.UUID;

public record EventDetailResponse(
        UUID id,
        String title,
        String description,
        String location,
        Instant startTime,
        Instant endTime,
        int capacity,
        int participantCount,
        EventVisibility visibility,
        EventStatus status,
        EventOrganizerResponse organizer,
        boolean joinedByCurrentUser,
        boolean ownedByCurrentUser,
        Instant createdAt,
        Instant updatedAt) {
}
