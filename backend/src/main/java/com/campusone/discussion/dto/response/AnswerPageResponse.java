package com.campusone.discussion.dto.response;

import java.util.List;

public record AnswerPageResponse(
        List<AnswerResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
