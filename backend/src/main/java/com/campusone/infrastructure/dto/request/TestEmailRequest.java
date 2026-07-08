package com.campusone.infrastructure.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record TestEmailRequest(
        @Email(message = "Enter a valid email address.")
        @Size(max = 254, message = "Email must be 254 characters or fewer.")
        String email) {
}
