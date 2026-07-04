package com.campusone.search.repository;

import com.campusone.search.dto.SearchType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SearchDocument(
        UUID id,
        SearchType type,
        String title,
        String snippetSource,
        String ownerOrAuthorName,
        String category,
        String location,
        String companyName,
        BigDecimal price,
        String currency,
        String status,
        Instant relevantDate,
        Instant createdAt,
        Instant updatedAt,
        int relevanceScore) {
}
