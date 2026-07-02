package com.campusone.discussion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAnswerRequest(
        @NotBlank
        @Size(min = 10, max = 5000)
        String body) {

    public CreateAnswerRequest {
        body = body == null ? null : body.trim();
    }
}
