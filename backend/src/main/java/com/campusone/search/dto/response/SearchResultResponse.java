package com.campusone.search.dto.response;

import com.campusone.search.dto.SearchType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SearchResultResponse(
        UUID id,
        SearchType type,
        String title,
        String snippet,
        String targetUrl,
        String ownerOrAuthorName,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt) {
}
