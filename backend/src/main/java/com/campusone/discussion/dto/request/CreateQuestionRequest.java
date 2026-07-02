package com.campusone.discussion.dto.request;

import com.campusone.discussion.entity.DiscussionCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuestionRequest(
        @NotBlank
        @Size(min = 5, max = 180)
        String title,

        @NotBlank
        @Size(min = 10, max = 5000)
        String body,

        @NotNull
        DiscussionCategory category) {

    public CreateQuestionRequest {
        title = trim(title);
        body = trim(body);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
