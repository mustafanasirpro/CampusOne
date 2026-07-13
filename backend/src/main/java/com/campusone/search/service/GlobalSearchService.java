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
import com.campusone.user.repository.UserRepository;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GlobalSearchService {

    private static final int MINIMUM_QUERY_LENGTH = 2;
    private static final int MAXIMUM_QUERY_LENGTH = 100;

    private final GlobalSearchRepository searchRepository;
    private final SearchResultMapper searchResultMapper;
    private final SearchQueryNormalizer queryNormalizer;
    private final UserRepository userRepository;

    public GlobalSearchService(
            GlobalSearchRepository searchRepository,
            SearchResultMapper searchResultMapper,
            SearchQueryNormalizer queryNormalizer,
            UserRepository userRepository) {
        this.searchRepository = searchRepository;
        this.searchResultMapper = searchResultMapper;
        this.queryNormalizer = queryNormalizer;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public GlobalSearchResponse search(
            String query,
            Set<SearchType> requestedTypes,
            int page,
            int size,
            SearchSort sort,
            UUID requesterUserId) {
        String normalizedQuery = normalizeQuery(query);
        UUID requesterUniversityId = requesterUniversityId(requesterUserId);
        Set<SearchType> types = normalizeTypes(
                requestedTypes,
                requesterUniversityId);
        if (types.isEmpty()) {
            return emptyResponse(query, page, size);
        }
        long offset = (long) page * size;
        SearchRepositoryResult repositoryResult = searchRepository.search(
                normalizedQuery,
                types,
                requesterUniversityId,
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
            int limit,
            UUID requesterUserId) {
        String normalizedQuery = normalizeQuery(query);
        List<String> suggestions = deduplicateSuggestions(
                searchRepository.findSuggestions(
                        normalizedQuery,
                        requesterUniversityId(requesterUserId),
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
                        "Available internship opportunities."),
                new SearchTypeResponse(
                        SearchType.LOST_FOUND,
                        "Lost & Found",
                        "Published campus lost-and-found reports for signed-in students."));
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
            Set<SearchType> requestedTypes,
            UUID requesterUniversityId) {
        EnumSet<SearchType> types =
                requestedTypes == null || requestedTypes.isEmpty()
                        ? EnumSet.allOf(SearchType.class)
                        : EnumSet.copyOf(requestedTypes);
        if (requesterUniversityId == null) {
            types.remove(SearchType.LOST_FOUND);
        }
        return types;
    }

    private UUID requesterUniversityId(UUID requesterUserId) {
        if (requesterUserId == null) {
            return null;
        }
        return userRepository.findById(requesterUserId)
                .map(user -> user.getStudentProfile() == null
                        ? null
                        : user.getStudentProfile().getUniversity())
                .map(university -> university == null ? null : university.getId())
                .orElse(null);
    }

    private GlobalSearchResponse emptyResponse(
            String query,
            int page,
            int size) {
        return new GlobalSearchResponse(
                displayQuery(query),
                page,
                size,
                0,
                0,
                false,
                List.of());
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
