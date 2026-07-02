package com.campusone.marketplace.dto.response;

import com.campusone.marketplace.entity.MarketplaceCategory;
import com.campusone.marketplace.entity.MarketplaceItemCondition;
import com.campusone.marketplace.entity.MarketplaceListingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MarketplaceListingSummaryResponse(
        UUID id,
        String title,
        MarketplaceCategory category,
        BigDecimal price,
        String currency,
        MarketplaceItemCondition condition,
        MarketplaceListingStatus status,
        MarketplaceImageResponse primaryImage,
        MarketplaceSellerResponse seller,
        Instant createdAt,
        Instant updatedAt) {
}
