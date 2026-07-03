package com.campusone.internship.dto.response;

import java.util.List;

public record InternshipPageResponse(
        List<InternshipSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
