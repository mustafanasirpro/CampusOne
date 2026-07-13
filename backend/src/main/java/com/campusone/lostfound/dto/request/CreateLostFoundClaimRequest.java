package com.campusone.lostfound.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLostFoundClaimRequest(
        @NotBlank @Size(min = 10, max = 2000) String proofText) {
}
