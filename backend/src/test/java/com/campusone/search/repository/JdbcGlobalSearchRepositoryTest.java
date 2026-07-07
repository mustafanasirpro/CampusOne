package com.campusone.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.search.dto.SearchSort;
import com.campusone.search.dto.SearchType;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class JdbcGlobalSearchRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private JdbcGlobalSearchRepository searchRepository;

    @BeforeEach
    void setUp() {
        searchRepository = new JdbcGlobalSearchRepository(jdbcTemplate);
    }

    @Test
    void search_queryExcludesDeletedHiddenAndPrivateContent() {
        when(jdbcTemplate.query(
                anyString(),
                any(SqlParameterSource.class),
                org.mockito.ArgumentMatchers
                        .<RowMapper<SearchDocument>>any()))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForObject(
                anyString(),
                any(SqlParameterSource.class),
                eq(Long.class)))
                .thenReturn(0L);

        searchRepository.search(
                "java",
                Set.of(SearchType.NOTE),
                0,
                10,
                SearchSort.RELEVANCE);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sql.capture(),
                any(SqlParameterSource.class),
                org.mockito.ArgumentMatchers
                        .<RowMapper<SearchDocument>>any());

        assertThat(sql.getValue())
                .contains(
                        "note.deleted_at IS NULL",
                        "note.moderation_status = 'APPROVED'",
                        "note.visibility = 'PUBLIC'",
                        "listing.deleted_at IS NULL",
                        "listing.status = 'ACTIVE'",
                        "question.deleted = FALSE",
                        "question.status IN ('OPEN', 'RESOLVED', 'CLOSED')",
                        "event.deleted = FALSE",
                        "event.visibility = 'PUBLIC'",
                        "event.status IN ('UPCOMING', 'CANCELLED', 'COMPLETED')",
                        "internship.status IN ('OPEN', 'CLOSED', 'EXPIRED')",
                        "internship.deleted = FALSE");
    }

    @Test
    void search_usesBoundTypesAndEscapedQueryParameters() {
        when(jdbcTemplate.query(
                anyString(),
                any(SqlParameterSource.class),
                org.mockito.ArgumentMatchers
                        .<RowMapper<SearchDocument>>any()))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForObject(
                anyString(),
                any(SqlParameterSource.class),
                eq(Long.class)))
                .thenReturn(0L);

        searchRepository.search(
                "java_100%",
                Set.of(SearchType.NOTE, SearchType.EVENT),
                5,
                5,
                SearchSort.NEWEST);

        ArgumentCaptor<SqlParameterSource> parameters =
                ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbcTemplate).query(
                anyString(),
                parameters.capture(),
                org.mockito.ArgumentMatchers
                        .<RowMapper<SearchDocument>>any());

        assertThat(parameters.getValue().getValue("searchPattern"))
                .isEqualTo("%java\\_100\\%%");
        Object configuredTypes =
                parameters.getValue().getValue("types");
        assertThat(configuredTypes).isInstanceOf(Collection.class);
        List<String> configuredTypeNames =
                ((Collection<?>) configuredTypes).stream()
                        .map(Object::toString)
                        .toList();
        assertThat(configuredTypeNames)
                .containsExactlyInAnyOrder("NOTE", "EVENT");
        assertThat(parameters.getValue().getValue("offset"))
                .isEqualTo(5L);
    }

    @Test
    void suggestions_deduplicatesInDatabaseAndIncludesCompanies() {
        when(jdbcTemplate.queryForList(
                anyString(),
                any(MapSqlParameterSource.class),
                eq(String.class)))
                .thenReturn(List.of("Systems Limited"));

        var suggestions = searchRepository.findSuggestions("systems", 5);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(
                sql.capture(),
                any(MapSqlParameterSource.class),
                eq(String.class));
        assertThat(suggestions).containsExactly("Systems Limited");
        assertThat(sql.getValue())
                .contains(
                        "DISTINCT ON (LOWER(suggestion))",
                        "internship.company_name AS suggestion",
                        "internship.deleted = FALSE",
                        "internship.status IN ('OPEN', 'CLOSED', 'EXPIRED')");
    }
}
