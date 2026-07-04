package com.campusone.ai.dto.response;

import java.util.List;

public record AiSessionPageResponse(
        List<AiSessionSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
