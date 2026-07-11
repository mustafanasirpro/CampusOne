package com.campusone.note.repository;

import com.campusone.search.service.SearchQueryNormalizer;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JdbcNoteSearchRepository implements NoteSearchRepository {

    private static final String MATCHING_NOTES = """
            SELECT
                note.id,
                note.created_at,
                CASE
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(note.title, ''), '[^[:alnum:]]+', ' ', 'g'))) = :exactQuery
                        THEN 1000
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(note.title, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :prefixPattern ESCAPE '\\'
                        THEN 900
                    WHEN (' ' || BTRIM(LOWER(REGEXP_REPLACE(COALESCE(note.title, ''), '[^[:alnum:]]+', ' ', 'g'))) || ' ')
                            LIKE :wholeWordPattern ESCAPE '\\'
                        THEN 850
                    WHEN NOT EXISTS (
                            SELECT 1
                            FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                            WHERE query_token.token <> ''
                              AND BTRIM(LOWER(REGEXP_REPLACE(COALESCE(note.title, ''), '[^[:alnum:]]+', ' ', 'g')))
                                    NOT LIKE '%' || query_token.token || '%'
                        )
                        THEN 820
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(note.title, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 800
                    WHEN COALESCE((
                            SELECT LOWER(STRING_AGG(LEFT(title_word.word, 1), ''))
                            FROM REGEXP_SPLIT_TO_TABLE(COALESCE(note.title, ''), '[^[:alnum:]]+')
                                AS title_word(word)
                            WHERE title_word.word <> ''
                        ), '') LIKE :initialPattern ESCAPE '\\'
                        THEN 780
                    WHEN LOWER(note.title) % :exactQuery
                        THEN 770
                    WHEN REPLACE(BTRIM(LOWER(REGEXP_REPLACE(COALESCE(course.course_code, ''), '[^[:alnum:]]+', ' ', 'g'))), ' ', '') = :compactExactQuery
                        THEN 760
                    WHEN REPLACE(BTRIM(LOWER(REGEXP_REPLACE(COALESCE(course.course_code, ''), '[^[:alnum:]]+', ' ', 'g'))), ' ', '') LIKE :compactPrefixPattern ESCAPE '\\'
                        THEN 740
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(course.title, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                         OR NOT EXISTS (
                            SELECT 1
                            FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                            WHERE query_token.token <> ''
                              AND BTRIM(LOWER(REGEXP_REPLACE(COALESCE(course.title, ''), '[^[:alnum:]]+', ' ', 'g')))
                                    NOT LIKE '%' || query_token.token || '%'
                         )
                        THEN 720
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(note.teacher_name, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 700
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(note_tags.tags, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 660
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(file_asset.original_filename, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 640
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(profile.full_name, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 620
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ', department.name, department.code), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 600
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(note.description, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                         OR NOT EXISTS (
                            SELECT 1
                            FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                            WHERE query_token.token <> ''
                              AND BTRIM(LOWER(REGEXP_REPLACE(COALESCE(note.description, ''), '[^[:alnum:]]+', ' ', 'g')))
                                    NOT LIKE '%' || query_token.token || '%'
                         )
                        THEN 500
                    ELSE 300
                END
                + LEAST(note.download_count, 50)
                + LEAST(note.rating_count, 20)
                + LEAST(GREATEST(0, 30 - EXTRACT(DAY FROM (CURRENT_TIMESTAMP - note.created_at)))::INT, 30)
                    AS relevance_score
            FROM notes note
            JOIN courses course ON course.id = note.course_id
            JOIN departments department ON department.id = course.department_id
            JOIN file_assets file_asset ON file_asset.id = note.file_asset_id
            LEFT JOIN student_profiles profile
                ON profile.user_id = note.uploader_id
            LEFT JOIN LATERAL (
                SELECT
                    STRING_AGG(tag.name, ' ') AS tags,
                    STRING_AGG(tag.normalized_name, ' ') AS normalized_tags
                FROM note_tags note_tag
                JOIN tags tag ON tag.id = note_tag.tag_id
                WHERE note_tag.note_id = note.id
            ) note_tags ON TRUE
            WHERE note.deleted_at IS NULL
              AND note.moderation_status = 'APPROVED'
              AND note.visibility = 'PUBLIC'
              AND (:courseId IS NULL OR course.id = :courseId)
              AND (
                    :courseSearchPattern IS NULL
                    OR BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ', course.course_code, course.title), '[^[:alnum:]]+', ' ', 'g')))
                        LIKE :courseSearchPattern ESCAPE '\\'
                    OR REPLACE(BTRIM(LOWER(REGEXP_REPLACE(COALESCE(course.course_code, ''), '[^[:alnum:]]+', ' ', 'g'))), ' ', '')
                        LIKE :courseCompactPattern ESCAPE '\\'
              )
              AND (
                    :normalizedTag IS NULL
                    OR note_tags.normalized_tags LIKE :tagSearchPattern ESCAPE '\\'
              )
              AND (
                    BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ',
                        note.title,
                        note.description,
                        note.teacher_name,
                        course.course_code,
                        course.title,
                        department.name,
                        department.code,
                        note_tags.tags,
                        note_tags.normalized_tags,
                        file_asset.original_filename,
                        profile.full_name,
                        note.file_type,
                        note.semester::TEXT
                    ), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                    OR REPLACE(BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ',
                        note.title,
                        note.description,
                        note.teacher_name,
                        course.course_code,
                        course.title,
                        department.name,
                        department.code,
                        note_tags.tags,
                        note_tags.normalized_tags,
                        file_asset.original_filename,
                        profile.full_name,
                        note.file_type,
                        note.semester::TEXT
                    ), '[^[:alnum:]]+', ' ', 'g'))), ' ', '') LIKE :compactSearchPattern ESCAPE '\\'
                    OR COALESCE((
                        SELECT LOWER(STRING_AGG(LEFT(title_word.word, 1), ''))
                        FROM REGEXP_SPLIT_TO_TABLE(COALESCE(note.title, ''), '[^[:alnum:]]+')
                            AS title_word(word)
                        WHERE title_word.word <> ''
                    ), '') LIKE :initialPattern ESCAPE '\\'
                    OR LOWER(note.title) % :exactQuery
                    OR LOWER(course.title) % :exactQuery
                    OR NOT EXISTS (
                        SELECT 1
                        FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                        WHERE query_token.token <> ''
                          AND BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ',
                                note.title,
                                note.description,
                                note.teacher_name,
                                course.course_code,
                                course.title,
                                department.name,
                                department.code,
                                note_tags.tags,
                                note_tags.normalized_tags,
                                file_asset.original_filename,
                                profile.full_name,
                                note.file_type,
                                note.semester::TEXT
                              ), '[^[:alnum:]]+', ' ', 'g')))
                                NOT LIKE '%' || query_token.token || '%'
                    )
              )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SearchQueryNormalizer normalizer;

    public JdbcNoteSearchRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            SearchQueryNormalizer normalizer) {
        this.jdbcTemplate = jdbcTemplate;
        this.normalizer = normalizer;
    }

    @Override
    public NoteSearchResult searchPublicNotes(
            String normalizedQuery,
            UUID courseId,
            String normalizedCourseFilter,
            String normalizedTagFilter,
            long offset,
            int limit) {
        MapSqlParameterSource parameters = parameters(
                normalizedQuery,
                courseId,
                normalizedCourseFilter,
                normalizedTagFilter)
                .addValue("offset", offset)
                .addValue("limit", limit);
        String searchSql = """
                WITH matching_notes AS (
                %s
                )
                SELECT id
                FROM matching_notes
                ORDER BY
                    relevance_score DESC,
                    created_at DESC,
                    id ASC
                LIMIT :limit OFFSET :offset
                """.formatted(MATCHING_NOTES);
        String countSql = """
                WITH matching_notes AS (
                %s
                )
                SELECT COUNT(*)
                FROM matching_notes
                """.formatted(MATCHING_NOTES);
        List<UUID> noteIds = jdbcTemplate.queryForList(
                searchSql,
                parameters,
                UUID.class);
        Long totalElements = jdbcTemplate.queryForObject(
                countSql,
                parameters,
                Long.class);
        return new NoteSearchResult(
                noteIds,
                totalElements == null ? 0 : totalElements);
    }

    private MapSqlParameterSource parameters(
            String normalizedQuery,
            UUID courseId,
            String normalizedCourseFilter,
            String normalizedTagFilter) {
        String compactQuery = normalizer.compact(normalizedQuery);
        String courseCompact = normalizedCourseFilter == null
                ? null
                : normalizer.compact(normalizedCourseFilter);
        return new MapSqlParameterSource()
                .addValue("exactQuery", normalizedQuery)
                .addValue("compactExactQuery", compactQuery)
                .addValue("tokensText", normalizedQuery)
                .addValue("prefixPattern", normalizer.prefixPattern(normalizedQuery))
                .addValue("compactPrefixPattern", normalizer.prefixPattern(compactQuery))
                .addValue("initialPattern", normalizer.prefixPattern(compactQuery))
                .addValue("wholeWordPattern", normalizer.wholeWordPattern(normalizedQuery))
                .addValue("searchPattern", normalizer.likePattern(normalizedQuery))
                .addValue("compactSearchPattern", normalizer.likePattern(compactQuery))
                .addValue("courseId", courseId)
                .addValue("courseSearchPattern", normalizedCourseFilter == null
                        ? null
                        : normalizer.likePattern(normalizedCourseFilter))
                .addValue("courseCompactPattern", courseCompact == null
                        ? null
                        : normalizer.likePattern(courseCompact))
                .addValue("normalizedTag", normalizedTagFilter)
                .addValue("tagSearchPattern", normalizedTagFilter == null
                        ? null
                        : normalizer.likePattern(normalizedTagFilter));
    }
}
