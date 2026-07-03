package com.campusone.internship.dto.request;

import com.campusone.internship.entity.InternshipType;
import com.campusone.internship.entity.WorkMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

public record CreateInternshipRequest(
        @NotBlank
        @Size(min = 5, max = 180)
        String title,

        @NotBlank
        @Size(min = 2, max = 160)
        String companyName,

        @NotBlank
        @Size(min = 20, max = 5000)
        String description,

        @NotBlank
        @Size(min = 2, max = 255)
        String location,

        @NotNull
        InternshipType internshipType,

        @NotNull
        WorkMode workMode,

        @NotNull
        Boolean paid,

        @DecimalMin("0.00")
        BigDecimal stipendAmount,

        @Size(min = 3, max = 10)
        @Pattern(regexp = "^[A-Z]{3,10}$")
        String currency,

        @NotBlank
        @Size(max = 1000)
        @Pattern(regexp = "(?i)^https?://\\S+$")
        String applyUrl,

        @NotNull
        @Future
        Instant deadline) {

    public CreateInternshipRequest {
        title = trim(title);
        companyName = trim(companyName);
        description = trim(description);
        location = trim(location);
        currency = currency == null
                ? null
                : currency.trim().toUpperCase(Locale.ROOT);
        applyUrl = trim(applyUrl);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
