package com.campusone.note.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.search.service.SearchQueryNormalizer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class JdbcNoteSearchRepositoryTest {

    private static final UUID COURSE_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private JdbcNoteSearchRepository searchRepository;

    @BeforeEach
    void setUp() {
        searchRepository = new JdbcNoteSearchRepository(
                jdbcTemplate,
                new SearchQueryNormalizer());
    }

    @Test
    void searchPublicNotes_hidesPendingRejectedPrivateAndDeletedNotes() {
        stubEmptySearch();

        searchRepository.searchPublicNotes(
                "machine learning",
                COURSE_ID,
                "computer science",
                "midterm",
                0,
                10);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(
                sql.capture(),
                any(SqlParameterSource.class),
                eq(UUID.class));

        assertThat(sql.getValue())
                .contains(
                        "note.deleted_at IS NULL",
                        "note.moderation_status = 'APPROVED'",
                        "note.visibility = 'PUBLIC'",
                        "(:hasCourseIdFilter = FALSE OR course.id = :courseId)",
                        ":hasCourseTextFilter = FALSE",
                        ":hasTagFilter = FALSE");
    }

    @Test
    void searchPublicNotes_searchesEveryMeaningfulNoteField() {
        stubEmptySearch();

        searchRepository.searchPublicNotes(
                "machine learning",
                null,
                null,
                null,
                0,
                10);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(
                sql.capture(),
                any(SqlParameterSource.class),
                eq(UUID.class));

        assertThat(sql.getValue())
                .contains(
                        "note.title",
                        "note.description",
                        "note.teacher_name",
                        "course.course_code",
                        "course.title",
                        "department.name",
                        "department.code",
                        "note_tags.tags",
                        "note_tags.normalized_tags",
                        "file_asset.original_filename",
                        "profile.full_name",
                        "note.file_type",
                        "note.semester::TEXT",
                        "STRING_TO_ARRAY(:tokensText, ' ')",
                        "REGEXP_SPLIT_TO_TABLE",
                        "LOWER(note.title) % :exactQuery",
                        "LOWER(course.title) % :exactQuery");
    }

    @Test
    void searchPublicNotes_usesEscapedPatternsAndCompactCourseCodeParameters() {
        stubEmptySearch();

        searchRepository.searchPublicNotes(
                "cs_100%",
                COURSE_ID,
                "csc 275",
                "machine_learning",
                12,
                6);

        ArgumentCaptor<SqlParameterSource> parameters =
                ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbcTemplate).queryForList(
                anyString(),
                parameters.capture(),
                eq(UUID.class));

        assertThat(parameters.getValue().getValue("searchPattern"))
                .isEqualTo("%cs\\_100\\%%");
        assertThat(parameters.getValue().getValue("prefixPattern"))
                .isEqualTo("cs\\_100\\%%");
        assertThat(parameters.getValue().getValue("compactSearchPattern"))
                .isEqualTo("%cs\\_100\\%%");
        assertThat(parameters.getValue().getValue("courseSearchPattern"))
                .isEqualTo("%csc 275%");
        assertThat(parameters.getValue().getValue("courseCompactPattern"))
                .isEqualTo("%csc275%");
        assertThat(parameters.getValue().getValue("hasCourseIdFilter"))
                .isEqualTo(true);
        assertThat(parameters.getValue().getValue("hasCourseTextFilter"))
                .isEqualTo(true);
        assertThat(parameters.getValue().getValue("hasTagFilter"))
                .isEqualTo(true);
        assertThat(parameters.getValue().getValue("tagSearchPattern"))
                .isEqualTo("%machine\\_learning%");
        assertThat(parameters.getValue().getValue("offset")).isEqualTo(12L);
        assertThat(parameters.getValue().getValue("limit")).isEqualTo(6);
    }

    @Test
    void searchPublicNotes_usesBooleanFlagsWhenOptionalFiltersAreMissing() {
        stubEmptySearch();

        searchRepository.searchPublicNotes(
                "data",
                null,
                null,
                null,
                0,
                10);

        ArgumentCaptor<SqlParameterSource> parameters =
                ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbcTemplate).queryForList(
                anyString(),
                parameters.capture(),
                eq(UUID.class));

        assertThat(parameters.getValue().getValue("hasCourseIdFilter"))
                .isEqualTo(false);
        assertThat(parameters.getValue().getValue("hasCourseTextFilter"))
                .isEqualTo(false);
        assertThat(parameters.getValue().getValue("hasTagFilter"))
                .isEqualTo(false);
        assertThat(parameters.getValue().getValue("courseSearchPattern"))
                .isNull();
        assertThat(parameters.getValue().getValue("courseCompactPattern"))
                .isNull();
        assertThat(parameters.getValue().getValue("tagSearchPattern"))
                .isNull();
    }

    @Test
    void searchPublicNotes_rankingPrioritizesTitleCourseAndTeacherBeforeBody() {
        stubEmptySearch();

        searchRepository.searchPublicNotes(
                "machine learning",
                null,
                null,
                null,
                0,
                10);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(
                sql.capture(),
                any(SqlParameterSource.class),
                eq(UUID.class));
        String searchSql = sql.getValue();

        assertThat(searchSql.indexOf("THEN 1000"))
                .isLessThan(searchSql.indexOf("THEN 900"));
        assertThat(searchSql.indexOf("THEN 900"))
                .isLessThan(searchSql.indexOf("THEN 850"));
        assertThat(searchSql.indexOf("THEN 850"))
                .isLessThan(searchSql.indexOf("THEN 820"));
        assertThat(searchSql.indexOf("THEN 820"))
                .isLessThan(searchSql.indexOf("THEN 800"));
        assertThat(searchSql.indexOf("THEN 800"))
                .isLessThan(searchSql.indexOf("THEN 780"));
        assertThat(searchSql.indexOf("THEN 780"))
                .isLessThan(searchSql.indexOf("THEN 770"));
        assertThat(searchSql.indexOf("THEN 770"))
                .isLessThan(searchSql.indexOf("THEN 760"));
        assertThat(searchSql.indexOf("THEN 760"))
                .isLessThan(searchSql.indexOf("THEN 700"));
        assertThat(searchSql.indexOf("THEN 700"))
                .isLessThan(searchSql.indexOf("THEN 660"));
        assertThat(searchSql.indexOf("THEN 660"))
                .isLessThan(searchSql.indexOf("THEN 500"));
        assertThat(searchSql)
                .contains(
                        "relevance_score DESC",
                        "created_at DESC",
                        "id ASC");
    }

    private void stubEmptySearch() {
        when(jdbcTemplate.queryForList(
                anyString(),
                any(SqlParameterSource.class),
                eq(UUID.class)))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForObject(
                anyString(),
                any(SqlParameterSource.class),
                eq(Long.class)))
                .thenReturn(0L);
    }
}
