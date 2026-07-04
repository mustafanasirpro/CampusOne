package com.campusone.gamification.dto.response;

import java.util.UUID;

public record BadgeResponse(
        UUID id,
        String code,
        String name,
        String description,
        String category,
        String icon,
        int xpRequired,
        boolean active,
        int sortOrder) {
}
