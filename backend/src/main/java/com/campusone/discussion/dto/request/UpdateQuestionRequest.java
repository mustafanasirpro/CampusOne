package com.campusone.discussion.dto.request;

import com.campusone.discussion.entity.DiscussionCategory;
import jakarta.validation.constraints.Size;

public record UpdateQuestionRequest(
        @Size(min = 5, max = 180)
        String title,

        @Size(min = 10, max = 5000)
        String body,

        DiscussionCategory category,

        QuestionUpdateStatus status) {

    public UpdateQuestionRequest {
        title = trim(title);
        body = trim(body);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
