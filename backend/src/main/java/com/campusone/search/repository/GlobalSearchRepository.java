package com.campusone.search.repository;

import com.campusone.search.dto.SearchSort;
import com.campusone.search.dto.SearchType;
import java.util.List;
import java.util.Set;

public interface GlobalSearchRepository {

    SearchRepositoryResult search(
            String normalizedQuery,
            Set<SearchType> types,
            long offset,
            int limit,
            SearchSort sort);

    List<String> findSuggestions(
            String normalizedQuery,
            int limit);
}
