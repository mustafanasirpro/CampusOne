package com.campusone.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MarketplaceImageRequest(
        @NotBlank
        @Size(max = 2048)
        @Pattern(regexp = "(?i)^https?://\\S+$")
        String imageUrl,

        @Size(max = 160)
        String altText) {

    public MarketplaceImageRequest {
        imageUrl = trim(imageUrl);
        altText = normalizeOptional(altText);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
