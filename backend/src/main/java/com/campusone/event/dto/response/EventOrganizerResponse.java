package com.campusone.event.dto.response;

import java.util.UUID;

public record EventOrganizerResponse(
        UUID userId,
        String fullName,
        String avatarUrl,
        String university) {
}
