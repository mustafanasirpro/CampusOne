package com.campusone.marketplace.dto.response;

import java.util.UUID;

public record MarketplaceSellerResponse(
        UUID userId,
        String fullName,
        String avatarUrl,
        String university) {
}
