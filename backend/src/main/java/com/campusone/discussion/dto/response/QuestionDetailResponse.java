package com.campusone.discussion.dto.response;

import com.campusone.discussion.entity.DiscussionCategory;
import com.campusone.discussion.entity.DiscussionQuestionStatus;
import java.time.Instant;
import java.util.UUID;

public record QuestionDetailResponse(
        UUID id,
        String title,
        String body,
        DiscussionCategory category,
        DiscussionQuestionStatus status,
        DiscussionAuthorResponse author,
        int answerCount,
        int voteScore,
        int viewCount,
        UUID acceptedAnswerId,
        Integer currentUserVote,
        boolean ownedByCurrentUser,
        AnswerPageResponse answers,
        Instant createdAt,
        Instant updatedAt) {
}
