package com.campusone.marketplace.dto.response;

import java.util.UUID;

public record MarketplaceImageResponse(
        UUID id,
        String imageUrl,
        String altText,
        int displayOrder,
        String originalFilename,
        String mimeType,
        Long sizeBytes) {
}
