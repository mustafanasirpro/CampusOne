package com.campusone.discussion.dto.response;

import java.util.UUID;

public record VoteResponse(
        UUID targetId,
        Integer currentUserVote,
        int voteScore) {
}
