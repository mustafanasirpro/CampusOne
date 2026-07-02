package com.campusone.marketplace.dto.response;

import java.util.List;

public record MarketplaceListingPageResponse(
        List<MarketplaceListingSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
