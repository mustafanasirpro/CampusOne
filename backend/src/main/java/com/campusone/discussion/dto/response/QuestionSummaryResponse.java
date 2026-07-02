package com.campusone.discussion.dto.response;

import com.campusone.discussion.entity.DiscussionCategory;
import com.campusone.discussion.entity.DiscussionQuestionStatus;
import java.time.Instant;
import java.util.UUID;

public record QuestionSummaryResponse(
        UUID id,
        String title,
        String bodyPreview,
        DiscussionCategory category,
        DiscussionQuestionStatus status,
        DiscussionAuthorResponse author,
        int answerCount,
        int voteScore,
        int viewCount,
        UUID acceptedAnswerId,
        Instant createdAt,
        Instant updatedAt) {
}
