package com.campusone.discussion.dto.response;

import java.util.UUID;

public record DiscussionAuthorResponse(
        UUID userId,
        String fullName,
        String avatarUrl,
        String university) {
}
