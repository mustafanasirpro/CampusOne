package com.campusone.auth.dto.request;

import com.campusone.common.util.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank
        @Email
        @Size(max = 254)
        String email) {

    public ForgotPasswordRequest {
        email = EmailNormalizer.normalize(email);
    }
}
