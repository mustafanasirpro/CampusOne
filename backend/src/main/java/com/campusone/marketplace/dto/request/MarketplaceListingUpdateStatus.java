package com.campusone.marketplace.dto.request;

import com.campusone.marketplace.entity.MarketplaceListingStatus;

public enum MarketplaceListingUpdateStatus {
    ACTIVE,
    SOLD;

    public MarketplaceListingStatus toListingStatus() {
        return MarketplaceListingStatus.valueOf(name());
    }
}
