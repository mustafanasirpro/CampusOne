package com.campusone.search.service;

import com.campusone.search.dto.SearchSort;
import com.campusone.search.dto.SearchType;
import com.campusone.search.dto.response.GlobalSearchResponse;
import com.campusone.search.dto.response.SearchResultResponse;
import com.campusone.search.dto.response.SearchSuggestionResponse;
import com.campusone.search.dto.response.SearchTypeResponse;
import com.campusone.search.exception.SearchValidationException;
import com.campusone.search.mapper.SearchResultMapper;
import com.campusone.search.repository.GlobalSearchRepository;
import com.campusone.search.repository.SearchRepositoryResult;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GlobalSearchService {

    private static final int MINIMUM_QUERY_LENGTH = 2;
    private static final int MAXIMUM_QUERY_LENGTH = 100;

    private final GlobalSearchRepository searchRepository;
    private final SearchResultMapper searchResultMapper;
    private final SearchQueryNormalizer queryNormalizer;

    public GlobalSearchService(
            GlobalSearchRepository searchRepository,
            SearchResultMapper searchResultMapper,
            SearchQueryNormalizer queryNormalizer) {
        this.searchRepository = searchRepository;
        this.searchResultMapper = searchResultMapper;
        this.queryNormalizer = queryNormalizer;
    }

    @Transactional(readOnly = true)
    public GlobalSearchResponse search(
            String query,
            Set<SearchType> requestedTypes,
            int page,
            int size,
            SearchSort sort) {
        String normalizedQuery = normalizeQuery(query);
        Set<SearchType> types = normalizeTypes(requestedTypes);
        long offset = (long) page * size;
        SearchRepositoryResult repositoryResult = searchRepository.search(
                normalizedQuery,
                types,
                offset,
                size,
                sort);
        List<SearchResultResponse> results =
                repositoryResult.documents().stream()
                        .map(document -> searchResultMapper.toResponse(
                                document,
                                normalizedQuery))
                        .toList();
        long totalElements = repositoryResult.totalElements();
        long calculatedPages = totalElements == 0
                ? 0
                : (totalElements + size - 1) / size;
        int totalPages = (int) Math.min(
                Integer.MAX_VALUE,
                calculatedPages);
        boolean hasNext = ((long) page + 1) * size < totalElements;
        return new GlobalSearchResponse(
                displayQuery(query),
                page,
                size,
                totalElements,
                totalPages,
                hasNext,
                results);
    }

    @Transactional(readOnly = true)
    public SearchSuggestionResponse suggestions(
            String query,
            int limit) {
        String normalizedQuery = normalizeQuery(query);
        List<String> suggestions = deduplicateSuggestions(
                searchRepository.findSuggestions(
                        normalizedQuery,
                        limit));
        return new SearchSuggestionResponse(
                displayQuery(query),
                suggestions);
    }

    @Transactional(readOnly = true)
    public List<SearchTypeResponse> types() {
        return List.of(
                new SearchTypeResponse(
                        SearchType.NOTE,
                        "Notes",
                        "Approved public study notes and course material."),
                new SearchTypeResponse(
                        SearchType.MARKETPLACE,
                        "Marketplace",
                        "Active student marketplace listings."),
                new SearchTypeResponse(
                        SearchType.DISCUSSION,
                        "Discussions",
                        "Visible questions from the campus community."),
                new SearchTypeResponse(
                        SearchType.EVENT,
                        "Events",
                        "Public campus events and activities."),
                new SearchTypeResponse(
                        SearchType.INTERNSHIP,
                        "Internships",
                        "Available internship opportunities."));
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            throw new SearchValidationException(
                    "q",
                    "Query is required.");
        }
        String normalized = queryNormalizer.normalize(query);
        if (normalized.length() < MINIMUM_QUERY_LENGTH
                || normalized.length() > MAXIMUM_QUERY_LENGTH) {
            throw new SearchValidationException(
                    "q",
                    "Query must contain 2 to 100 searchable characters.");
        }
        return normalized;
    }

    private String displayQuery(String query) {
        return queryNormalizer.display(query);
    }

    private Set<SearchType> normalizeTypes(
            Set<SearchType> requestedTypes) {
        if (requestedTypes == null || requestedTypes.isEmpty()) {
            return EnumSet.allOf(SearchType.class);
        }
        return EnumSet.copyOf(requestedTypes);
    }

    private List<String> deduplicateSuggestions(
            List<String> suggestions) {
        Map<String, String> uniqueSuggestions = new LinkedHashMap<>();
        suggestions.stream()
                .filter(suggestion ->
                        suggestion != null && !suggestion.isBlank())
                .map(String::trim)
                .forEach(suggestion -> uniqueSuggestions.putIfAbsent(
                        suggestion.toLowerCase(Locale.ROOT),
                        suggestion));
        return List.copyOf(uniqueSuggestions.values());
    }
}
