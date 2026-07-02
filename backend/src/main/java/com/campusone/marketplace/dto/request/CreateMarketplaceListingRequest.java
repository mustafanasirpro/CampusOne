package com.campusone.marketplace.dto.request;

import com.campusone.marketplace.entity.MarketplaceCategory;
import com.campusone.marketplace.entity.MarketplaceItemCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public record CreateMarketplaceListingRequest(
        @NotBlank
        @Size(min = 5, max = 160)
        String title,

        @NotBlank
        @Size(min = 10, max = 5000)
        String description,

        @NotNull
        MarketplaceCategory category,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @DecimalMax("10000000.00")
        BigDecimal price,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,

        @NotNull
        MarketplaceItemCondition condition,

        @Size(max = 6)
        List<@Valid MarketplaceImageRequest> images) {

    public CreateMarketplaceListingRequest {
        title = trim(title);
        description = trim(description);
        currency = currency == null
                ? null
                : currency.trim().toUpperCase(Locale.ROOT);
        images = images == null ? List.of() : List.copyOf(images);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
