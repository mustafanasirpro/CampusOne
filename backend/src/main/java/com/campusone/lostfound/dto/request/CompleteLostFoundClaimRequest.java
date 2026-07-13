package com.campusone.lostfound.dto.request;

import jakarta.validation.constraints.Size;

public record CompleteLostFoundClaimRequest(
        @Size(max = 1000) String handoverNote) {
}
