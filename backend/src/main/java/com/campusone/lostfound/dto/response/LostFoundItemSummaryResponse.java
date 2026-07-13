package com.campusone.lostfound.dto.response;

import com.campusone.lostfound.entity.LostFoundCategory;
import com.campusone.lostfound.entity.LostFoundItemStatus;
import com.campusone.lostfound.entity.LostFoundItemType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LostFoundItemSummaryResponse(
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
        LostFoundImageResponse primaryImage,
        LostFoundReporterResponse reporter,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant expiresAt) {
}
