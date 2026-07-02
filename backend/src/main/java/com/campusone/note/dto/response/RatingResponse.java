package com.campusone.note.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RatingResponse(
        UUID noteId,
        int rating,
        long ratingCount,
        BigDecimal averageRating) {
}
