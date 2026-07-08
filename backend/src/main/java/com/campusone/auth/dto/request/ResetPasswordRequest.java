package com.campusone.auth.dto.request;

import com.campusone.common.validation.Utf8ByteLength;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank
        @Size(min = 20, max = 300)
        String token,

        @NotBlank
        @Size(min = 8, max = 72)
        @Utf8ByteLength(max = 72)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "must contain an uppercase letter, a lowercase letter, and a digit")
        String newPassword) {

    public ResetPasswordRequest {
        token = token == null ? null : token.trim();
    }
}
