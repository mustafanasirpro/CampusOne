package com.campusone.search.repository;

import java.util.List;

public record SearchRepositoryResult(
        List<SearchDocument> documents,
        long totalElements) {
}
