package com.campusone.lostfound.dto.response;

import com.campusone.lostfound.entity.LostFoundClaimStatus;
import java.time.Instant;
import java.util.UUID;

public record LostFoundClaimResponse(
        UUID id,
        UUID itemId,
        String itemTitle,
        LostFoundReporterResponse claimant,
        String proofText,
        LostFoundClaimStatus status,
        String reviewerNote,
        String handoverNote,
        Instant createdAt,
        Instant reviewedAt,
        Instant reporterHandoverConfirmedAt,
        Instant claimantHandoverConfirmedAt,
        Instant handoverCompletedAt,
        boolean claimantIsCurrentUser,
        boolean reporterIsCurrentUser) {
}
