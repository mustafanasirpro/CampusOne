package com.campusone.search.repository;

import com.campusone.search.dto.SearchSort;
import com.campusone.search.dto.SearchType;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface GlobalSearchRepository {

    SearchRepositoryResult search(
            String normalizedQuery,
            Set<SearchType> types,
            UUID requesterUniversityId,
            long offset,
            int limit,
            SearchSort sort);

    List<String> findSuggestions(
            String normalizedQuery,
            UUID requesterUniversityId,
            int limit);
}
