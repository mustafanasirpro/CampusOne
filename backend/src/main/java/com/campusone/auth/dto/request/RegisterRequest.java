package com.campusone.auth.dto.request;

import com.campusone.common.util.EmailNormalizer;
import com.campusone.common.validation.Utf8ByteLength;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RegisterRequest(
        @NotBlank
        @Size(min = 2, max = 80)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        @Utf8ByteLength(max = 72)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "must contain an uppercase letter, a lowercase letter, and a digit")
        String password,

        @NotNull
        UUID universityId,

        @NotNull
        UUID departmentId,

        @NotNull
        @Min(1)
        @Max(8)
        Integer semester) {

    public RegisterRequest {
        fullName = fullName == null ? null : fullName.trim();
        email = EmailNormalizer.normalize(email);
    }
}
