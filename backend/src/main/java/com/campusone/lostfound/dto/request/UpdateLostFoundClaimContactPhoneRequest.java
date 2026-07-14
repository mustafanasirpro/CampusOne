package com.campusone.lostfound.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateLostFoundClaimContactPhoneRequest(
        @NotBlank @Size(max = 40) String contactPhone,
        @NotNull @AssertTrue Boolean contactSharingConsent) {
}
