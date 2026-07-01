package com.campusone.auth.dto.request;

import com.campusone.common.util.EmailNormalizer;
import com.campusone.common.validation.Utf8ByteLength;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(max = 72)
        @Utf8ByteLength(max = 72)
        String password) {

    public LoginRequest {
        email = EmailNormalizer.normalize(email);
    }
}
