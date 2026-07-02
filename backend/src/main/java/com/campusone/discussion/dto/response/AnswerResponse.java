package com.campusone.discussion.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AnswerResponse(
        UUID id,
        UUID questionId,
        String body,
        DiscussionAuthorResponse author,
        boolean accepted,
        int voteScore,
        Integer currentUserVote,
        boolean ownedByCurrentUser,
        Instant createdAt,
        Instant updatedAt) {
}
