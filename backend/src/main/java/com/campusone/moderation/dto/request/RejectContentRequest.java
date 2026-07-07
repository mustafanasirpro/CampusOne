package com.campusone.moderation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectContentRequest(
        @NotBlank
        @Size(max = 1000)
        String reason) {
}
