package com.campusone.lostfound.dto.response;

import com.campusone.lostfound.entity.LostFoundCategory;
import com.campusone.lostfound.entity.LostFoundItemStatus;
import com.campusone.lostfound.entity.LostFoundItemType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LostFoundItemDetailResponse(
        UUID id,
        LostFoundItemType type,
        LostFoundCategory category,
        String title,
        String description,
        String locationText,
        LocalDate itemDate,
        String brand,
        String color,
        LostFoundItemStatus status,
        String moderationReason,
        LostFoundReporterResponse reporter,
        List<LostFoundImageResponse> images,
        boolean ownedByCurrentUser,
        boolean claimable,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant expiresAt,
        Instant resolvedAt) {
}
