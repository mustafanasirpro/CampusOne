package com.campusone.lostfound.dto.response;

import com.campusone.lostfound.entity.LostFoundMatchStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record LostFoundMatchResponse(
        UUID id,
        LostFoundItemSummaryResponse lostItem,
        LostFoundItemSummaryResponse foundItem,
        int score,
        JsonNode reasons,
        LostFoundMatchStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
