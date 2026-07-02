package com.campusone.discussion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAnswerRequest(
        @NotBlank
        @Size(min = 10, max = 5000)
        String body) {

    public UpdateAnswerRequest {
        body = body == null ? null : body.trim();
    }
}
