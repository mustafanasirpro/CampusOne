package com.campusone.ai.dto.response;

import java.util.List;

public record AiGeneratedItemPageResponse(
        List<AiGeneratedItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
