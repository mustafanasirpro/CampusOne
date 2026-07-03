package com.campusone.internship.dto.response;

import com.campusone.internship.entity.InternshipStatus;
import com.campusone.internship.entity.InternshipType;
import com.campusone.internship.entity.WorkMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InternshipSummaryResponse(
        UUID id,
        String title,
        String companyName,
        String description,
        String location,
        InternshipType internshipType,
        WorkMode workMode,
        boolean paid,
        BigDecimal stipendAmount,
        String currency,
        String applyUrl,
        Instant deadline,
        InternshipStatus status,
        InternshipPosterResponse poster,
        boolean savedByCurrentUser,
        boolean ownedByCurrentUser,
        Instant createdAt,
        Instant updatedAt) {
}
