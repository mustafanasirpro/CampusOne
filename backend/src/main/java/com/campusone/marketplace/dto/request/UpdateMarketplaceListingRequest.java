package com.campusone.marketplace.dto.request;

import com.campusone.marketplace.entity.MarketplaceCategory;
import com.campusone.marketplace.entity.MarketplaceItemCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public record UpdateMarketplaceListingRequest(
        @Size(min = 5, max = 160)
        String title,

        @Size(min = 10, max = 5000)
        String description,

        MarketplaceCategory category,

        @DecimalMin(value = "0.00", inclusive = false)
        @DecimalMax("10000000.00")
        BigDecimal price,

        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,

        MarketplaceItemCondition condition,

        MarketplaceListingUpdateStatus status,

        @Size(max = 6)
        List<@Valid MarketplaceImageRequest> images) {

    public UpdateMarketplaceListingRequest {
        title = trim(title);
        description = trim(description);
        currency = currency == null
                ? null
                : currency.trim().toUpperCase(Locale.ROOT);
        if (images != null) {
            images = List.copyOf(images);
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
