package com.campusone.discussion.dto.response;

import java.util.List;

public record QuestionPageResponse(
        List<QuestionSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
