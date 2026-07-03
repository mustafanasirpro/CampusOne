package com.campusone.search.dto.response;

import java.util.List;

public record SearchSuggestionResponse(
        String query,
        List<String> suggestions) {
}
