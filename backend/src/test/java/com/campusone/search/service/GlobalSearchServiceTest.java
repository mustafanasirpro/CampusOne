package com.campusone.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.search.dto.SearchSort;
import com.campusone.search.dto.SearchType;
import com.campusone.search.exception.SearchValidationException;
import com.campusone.search.mapper.SearchResultMapper;
import com.campusone.search.repository.GlobalSearchRepository;
import com.campusone.search.repository.SearchDocument;
import com.campusone.search.repository.SearchRepositoryResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceTest {

    private static final UUID RESULT_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-04T08:00:00Z");

    @Mock
    private GlobalSearchRepository searchRepository;

    private GlobalSearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new GlobalSearchService(
                searchRepository,
                new SearchResultMapper(),
                new SearchQueryNormalizer());
    }

    @Test
    void search_withoutTypes_searchesAllSupportedTypes() {
        when(searchRepository.search(
                "java",
                EnumSet.allOf(SearchType.class),
                0,
                10,
                SearchSort.RELEVANCE))
                .thenReturn(result(document(SearchType.NOTE), 1));

        var response = searchService.search(
                "  Java  ",
                null,
                0,
                10,
                SearchSort.RELEVANCE);

        assertThat(response.query()).isEqualTo("Java");
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().targetUrl())
                .isEqualTo("/notes/" + RESULT_ID);
    }

    @ParameterizedTest
    @EnumSource(SearchType.class)
    void search_withSingleType_searchesOnlyRequestedType(
            SearchType searchType) {
        when(searchRepository.search(
                "campus",
                Set.of(searchType),
                0,
                10,
                SearchSort.RELEVANCE))
                .thenReturn(result(document(searchType), 1));

        var response = searchService.search(
                "campus",
                Set.of(searchType),
                0,
                10,
                SearchSort.RELEVANCE);

        assertThat(response.results())
                .extracting(result -> result.type())
                .containsExactly(searchType);
    }

    @Test
    void search_pageAndSize_calculateOffsetAndPagination() {
        when(searchRepository.search(
                eq("java"),
                eq(EnumSet.allOf(SearchType.class)),
                eq(20L),
                eq(10),
                eq(SearchSort.RELEVANCE)))
                .thenReturn(result(document(SearchType.NOTE), 35));

        var response = searchService.search(
                "java",
                null,
                2,
                10,
                SearchSort.RELEVANCE);

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(4);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void search_normalizesCasePunctuationAndDuplicateSpaces() {
        when(searchRepository.search(
                "machine learning",
                EnumSet.allOf(SearchType.class),
                0,
                10,
                SearchSort.RELEVANCE))
                .thenReturn(result(document(SearchType.NOTE), 1));

        var response = searchService.search(
                "  Machine---   Learning!!!  ",
                null,
                0,
                10,
                SearchSort.RELEVANCE);

        assertThat(response.query()).isEqualTo("Machine Learning");
        verify(searchRepository).search(
                "machine learning",
                EnumSet.allOf(SearchType.class),
                0,
                10,
                SearchSort.RELEVANCE);
    }

    @ParameterizedTest
    @EnumSource(
            value = SearchSort.class,
            names = {"NEWEST", "OLDEST"})
    void search_dateSort_passesRequestedSort(SearchSort sort) {
        when(searchRepository.search(
                eq("java"),
                eq(EnumSet.allOf(SearchType.class)),
                anyLong(),
                eq(10),
                eq(sort)))
                .thenReturn(result(document(SearchType.NOTE), 1));

        searchService.search(
                "java",
                null,
                0,
                10,
                sort);

        verify(searchRepository).search(
                "java",
                EnumSet.allOf(SearchType.class),
                0,
                10,
                sort);
    }

    @Test
    void search_queryShorterThanTwoAfterTrimming_isRejected() {
        assertThatThrownBy(() -> searchService.search(
                " a ",
                null,
                0,
                10,
                SearchSort.RELEVANCE))
                .isInstanceOf(SearchValidationException.class)
                .hasMessageContaining("2 to 100");
    }

    @Test
    void suggestions_deduplicatesCaseInsensitiveValues() {
        when(searchRepository.findSuggestions("java", 5))
                .thenReturn(List.of(
                        "Java Backend Internship",
                        " java backend internship ",
                        "Java OOP Notes"));

        var response = searchService.suggestions("Java", 5);

        assertThat(response.suggestions()).containsExactly(
                "Java Backend Internship",
                "Java OOP Notes");
    }

    @Test
    void types_returnsEverySupportedType() {
        assertThat(searchService.types())
                .extracting(type -> type.type())
                .containsExactly(SearchType.values());
    }

    private SearchRepositoryResult result(
            SearchDocument document,
            long totalElements) {
        return new SearchRepositoryResult(
                List.of(document),
                totalElements);
    }

    private SearchDocument document(SearchType type) {
        return new SearchDocument(
                RESULT_ID,
                type,
                "Java Campus Resource",
                "A detailed Java resource for CampusOne students.",
                "Ayesha Malik",
                type == SearchType.NOTE ? "Object Oriented Programming" : null,
                type == SearchType.EVENT ? "Main Auditorium" : null,
                type == SearchType.INTERNSHIP ? "Systems Limited" : null,
                type == SearchType.MARKETPLACE
                        ? new BigDecimal("2500.00")
                        : null,
                type == SearchType.MARKETPLACE ? "PKR" : null,
                "ACTIVE",
                type == SearchType.EVENT ? NOW.plusSeconds(86400) : null,
                NOW,
                NOW,
                80);
    }
}
