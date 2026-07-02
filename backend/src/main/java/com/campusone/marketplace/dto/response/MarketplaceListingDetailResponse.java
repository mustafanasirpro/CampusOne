package com.campusone.marketplace.dto.response;

import com.campusone.marketplace.entity.MarketplaceCategory;
import com.campusone.marketplace.entity.MarketplaceItemCondition;
import com.campusone.marketplace.entity.MarketplaceListingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MarketplaceListingDetailResponse(
        UUID id,
        String title,
        String description,
        MarketplaceCategory category,
        BigDecimal price,
        String currency,
        MarketplaceItemCondition condition,
        MarketplaceListingStatus status,
        List<MarketplaceImageResponse> images,
        MarketplaceSellerResponse seller,
        Instant createdAt,
        Instant updatedAt) {
}
