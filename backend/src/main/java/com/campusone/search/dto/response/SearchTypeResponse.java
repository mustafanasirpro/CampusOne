package com.campusone.search.dto.response;

import com.campusone.search.dto.SearchType;

public record SearchTypeResponse(
        SearchType type,
        String displayName,
        String description) {
}
