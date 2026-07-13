package com.campusone.lostfound.dto.request;

import jakarta.validation.constraints.Size;

public record ReviewLostFoundClaimRequest(
        @Size(max = 1000) String note) {
}
