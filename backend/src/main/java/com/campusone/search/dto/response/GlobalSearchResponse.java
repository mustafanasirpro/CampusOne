package com.campusone.search.dto.response;

import java.util.List;

public record GlobalSearchResponse(
        String query,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        List<SearchResultResponse> results) {
}
