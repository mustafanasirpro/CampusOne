package com.campusone.event.dto.response;

import java.util.List;

public record EventPageResponse(
        List<EventSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
