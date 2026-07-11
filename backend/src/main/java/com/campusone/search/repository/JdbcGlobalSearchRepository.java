package com.campusone.search.repository;

import com.campusone.search.dto.SearchSort;
import com.campusone.search.dto.SearchType;
import com.campusone.search.service.SearchQueryNormalizer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JdbcGlobalSearchRepository
        implements GlobalSearchRepository {

    private static final String SEARCH_DOCUMENTS = """
            SELECT
                note.id,
                'NOTE'::VARCHAR AS search_type,
                note.title,
                note.description AS snippet_source,
                profile.full_name AS owner_name,
                CONCAT_WS(' · ', course.course_code, course.title)::VARCHAR AS category,
                NULL::VARCHAR AS location,
                NULL::VARCHAR AS company_name,
                NULL::NUMERIC AS price,
                NULL::VARCHAR AS currency,
                note.moderation_status AS status,
                note.published_at AS relevant_date,
                note.created_at,
                note.updated_at,
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
                + LEAST(note.rating_count, 20) AS relevance_score
            FROM notes note
            JOIN courses course ON course.id = note.course_id
            JOIN departments department ON department.id = course.department_id
            JOIN file_assets file_asset ON file_asset.id = note.file_asset_id
            LEFT JOIN student_profiles profile
                ON profile.user_id = note.uploader_id
            LEFT JOIN LATERAL (
                SELECT STRING_AGG(tag.name, ' ') AS tags
                FROM note_tags note_tag
                JOIN tags tag ON tag.id = note_tag.tag_id
                WHERE note_tag.note_id = note.id
            ) note_tags ON TRUE
            WHERE note.deleted_at IS NULL
              AND note.moderation_status = 'APPROVED'
              AND note.visibility = 'PUBLIC'
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
                                file_asset.original_filename,
                                profile.full_name,
                                note.file_type,
                                note.semester::TEXT
                              ), '[^[:alnum:]]+', ' ', 'g')))
                                NOT LIKE '%' || query_token.token || '%'
                    )
              )

            UNION ALL

            SELECT
                listing.id,
                'MARKETPLACE'::VARCHAR AS search_type,
                listing.title,
                listing.description AS snippet_source,
                profile.full_name AS owner_name,
                REPLACE(listing.category, '_', ' ') AS category,
                NULL::VARCHAR AS location,
                NULL::VARCHAR AS company_name,
                listing.price,
                listing.currency,
                listing.status,
                NULL::TIMESTAMPTZ AS relevant_date,
                listing.created_at,
                listing.updated_at,
                CASE
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(listing.title, ''), '[^[:alnum:]]+', ' ', 'g'))) = :exactQuery
                        THEN 1000
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(listing.title, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :prefixPattern ESCAPE '\\'
                        THEN 900
                    WHEN (' ' || BTRIM(LOWER(REGEXP_REPLACE(COALESCE(listing.title, ''), '[^[:alnum:]]+', ' ', 'g'))) || ' ')
                            LIKE :wholeWordPattern ESCAPE '\\'
                        THEN 850
                    WHEN NOT EXISTS (
                            SELECT 1
                            FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                            WHERE query_token.token <> ''
                              AND BTRIM(LOWER(REGEXP_REPLACE(COALESCE(listing.title, ''), '[^[:alnum:]]+', ' ', 'g')))
                                    NOT LIKE '%' || query_token.token || '%'
                        )
                        THEN 820
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(listing.title, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 800
                    WHEN LOWER(listing.title) % :exactQuery
                        THEN 770
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(listing.category, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 660
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(marketplace_images.image_terms, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 640
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(profile.full_name, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 620
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(listing.description, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                         OR NOT EXISTS (
                            SELECT 1
                            FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                            WHERE query_token.token <> ''
                              AND BTRIM(LOWER(REGEXP_REPLACE(COALESCE(listing.description, ''), '[^[:alnum:]]+', ' ', 'g')))
                                    NOT LIKE '%' || query_token.token || '%'
                         )
                        THEN 500
                    ELSE 300
                END AS relevance_score
            FROM marketplace_listings listing
            LEFT JOIN student_profiles profile
                ON profile.user_id = listing.seller_id
            LEFT JOIN LATERAL (
                SELECT STRING_AGG(CONCAT_WS(' ', image.original_filename, image.alt_text), ' ') AS image_terms
                FROM marketplace_listing_images image
                WHERE image.listing_id = listing.id
            ) marketplace_images ON TRUE
            WHERE listing.deleted_at IS NULL
              AND listing.status = 'ACTIVE'
              AND (
                    BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ',
                        listing.title,
                        listing.description,
                        listing.category,
                        listing.item_condition,
                        marketplace_images.image_terms,
                        profile.full_name
                    ), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(listing.title) % :exactQuery
                    OR NOT EXISTS (
                        SELECT 1
                        FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                        WHERE query_token.token <> ''
                          AND BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ',
                                listing.title,
                                listing.description,
                                listing.category,
                                listing.item_condition,
                                marketplace_images.image_terms,
                                profile.full_name
                              ), '[^[:alnum:]]+', ' ', 'g')))
                                NOT LIKE '%' || query_token.token || '%'
                    )
              )

            UNION ALL

            SELECT
                question.id,
                'DISCUSSION'::VARCHAR AS search_type,
                question.title,
                question.body AS snippet_source,
                profile.full_name AS owner_name,
                REPLACE(question.category, '_', ' ') AS category,
                NULL::VARCHAR AS location,
                NULL::VARCHAR AS company_name,
                NULL::NUMERIC AS price,
                NULL::VARCHAR AS currency,
                question.status,
                NULL::TIMESTAMPTZ AS relevant_date,
                question.created_at,
                question.updated_at,
                CASE
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(question.title, ''), '[^[:alnum:]]+', ' ', 'g'))) = :exactQuery
                        THEN 1000
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(question.title, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :prefixPattern ESCAPE '\\'
                        THEN 900
                    WHEN (' ' || BTRIM(LOWER(REGEXP_REPLACE(COALESCE(question.title, ''), '[^[:alnum:]]+', ' ', 'g'))) || ' ')
                            LIKE :wholeWordPattern ESCAPE '\\'
                        THEN 850
                    WHEN NOT EXISTS (
                            SELECT 1
                            FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                            WHERE query_token.token <> ''
                              AND BTRIM(LOWER(REGEXP_REPLACE(COALESCE(question.title, ''), '[^[:alnum:]]+', ' ', 'g')))
                                    NOT LIKE '%' || query_token.token || '%'
                        )
                        THEN 820
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(question.title, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 800
                    WHEN LOWER(question.title) % :exactQuery
                        THEN 770
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(question.category, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 660
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(profile.full_name, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 620
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(question.body, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 500
                    ELSE 300
                END AS relevance_score
            FROM discussion_questions question
            LEFT JOIN student_profiles profile
                ON profile.user_id = question.author_user_id
            WHERE question.deleted = FALSE
              AND question.status IN ('OPEN', 'RESOLVED', 'CLOSED')
              AND (
                    BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ',
                        question.title,
                        question.body,
                        question.category,
                        profile.full_name
                    ), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(question.title) % :exactQuery
                    OR NOT EXISTS (
                        SELECT 1
                        FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                        WHERE query_token.token <> ''
                          AND BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ',
                                question.title,
                                question.body,
                                question.category,
                                profile.full_name
                              ), '[^[:alnum:]]+', ' ', 'g')))
                                NOT LIKE '%' || query_token.token || '%'
                    )
              )

            UNION ALL

            SELECT
                event.id,
                'EVENT'::VARCHAR AS search_type,
                event.title,
                event.description AS snippet_source,
                profile.full_name AS owner_name,
                NULL::VARCHAR AS category,
                event.location,
                NULL::VARCHAR AS company_name,
                NULL::NUMERIC AS price,
                NULL::VARCHAR AS currency,
                event.status,
                event.start_time AS relevant_date,
                event.created_at,
                event.updated_at,
                CASE
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(event.title, ''), '[^[:alnum:]]+', ' ', 'g'))) = :exactQuery
                        THEN 1000
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(event.title, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :prefixPattern ESCAPE '\\'
                        THEN 900
                    WHEN (' ' || BTRIM(LOWER(REGEXP_REPLACE(COALESCE(event.title, ''), '[^[:alnum:]]+', ' ', 'g'))) || ' ')
                            LIKE :wholeWordPattern ESCAPE '\\'
                        THEN 850
                    WHEN NOT EXISTS (
                            SELECT 1
                            FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                            WHERE query_token.token <> ''
                              AND BTRIM(LOWER(REGEXP_REPLACE(COALESCE(event.title, ''), '[^[:alnum:]]+', ' ', 'g')))
                                    NOT LIKE '%' || query_token.token || '%'
                        )
                        THEN 820
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(event.title, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 800
                    WHEN LOWER(event.title) % :exactQuery
                        THEN 770
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(event.location, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 660
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(profile.full_name, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 620
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(event.description, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 500
                    ELSE 300
                END AS relevance_score
            FROM events event
            LEFT JOIN student_profiles profile
                ON profile.user_id = event.organizer_user_id
            WHERE event.deleted = FALSE
              AND event.visibility = 'PUBLIC'
              AND event.status IN ('UPCOMING', 'CANCELLED', 'COMPLETED')
              AND (
                    BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ',
                        event.title,
                        event.description,
                        event.location,
                        profile.full_name
                    ), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(event.title) % :exactQuery
                    OR NOT EXISTS (
                        SELECT 1
                        FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                        WHERE query_token.token <> ''
                          AND BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ',
                                event.title,
                                event.description,
                                event.location,
                                profile.full_name
                              ), '[^[:alnum:]]+', ' ', 'g')))
                                NOT LIKE '%' || query_token.token || '%'
                    )
              )

            UNION ALL

            SELECT
                internship.id,
                'INTERNSHIP'::VARCHAR AS search_type,
                internship.title,
                internship.description AS snippet_source,
                profile.full_name AS owner_name,
                REPLACE(internship.internship_type, '_', ' ') AS category,
                internship.location,
                internship.company_name,
                NULL::NUMERIC AS price,
                internship.currency,
                internship.status,
                internship.deadline AS relevant_date,
                internship.created_at,
                internship.updated_at,
                CASE
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.title, ''), '[^[:alnum:]]+', ' ', 'g'))) = :exactQuery
                        THEN 1000
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.title, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :prefixPattern ESCAPE '\\'
                        THEN 900
                    WHEN (' ' || BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.title, ''), '[^[:alnum:]]+', ' ', 'g'))) || ' ')
                            LIKE :wholeWordPattern ESCAPE '\\'
                        THEN 850
                    WHEN NOT EXISTS (
                            SELECT 1
                            FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                            WHERE query_token.token <> ''
                              AND BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.title, ''), '[^[:alnum:]]+', ' ', 'g')))
                                    NOT LIKE '%' || query_token.token || '%'
                        )
                        THEN 820
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.title, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 800
                    WHEN LOWER(internship.title) % :exactQuery
                        THEN 770
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.company_name, ''), '[^[:alnum:]]+', ' ', 'g'))) = :exactQuery
                        THEN 700
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.company_name, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :prefixPattern ESCAPE '\\'
                        THEN 680
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.internship_type, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 660
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(profile.full_name, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 620
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.description, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                         OR BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.location, ''), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                        THEN 500
                    ELSE 300
                END AS relevance_score
            FROM internships internship
            LEFT JOIN student_profiles profile
                ON profile.user_id = internship.poster_user_id
            WHERE internship.deleted = FALSE
              AND internship.status IN ('OPEN', 'CLOSED', 'EXPIRED')
              AND (
                    BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ',
                        internship.title,
                        internship.company_name,
                        internship.description,
                        internship.location,
                        internship.internship_type,
                        internship.work_mode,
                        internship.currency,
                        profile.full_name
                    ), '[^[:alnum:]]+', ' ', 'g'))) LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(internship.title) % :exactQuery
                    OR NOT EXISTS (
                        SELECT 1
                        FROM UNNEST(STRING_TO_ARRAY(:tokensText, ' ')) AS query_token(token)
                        WHERE query_token.token <> ''
                          AND BTRIM(LOWER(REGEXP_REPLACE(CONCAT_WS(' ',
                                internship.title,
                                internship.company_name,
                                internship.description,
                                internship.location,
                                internship.internship_type,
                                internship.work_mode,
                                internship.currency,
                                profile.full_name
                              ), '[^[:alnum:]]+', ' ', 'g')))
                                NOT LIKE '%' || query_token.token || '%'
                    )
              )
            """;

    private static final String INTERNSHIP_COMPANY_SUGGESTIONS = """
            SELECT
                internship.company_name AS suggestion,
                CASE
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.company_name, ''), '[^[:alnum:]]+', ' ', 'g'))) = :exactQuery
                        THEN 700
                    WHEN BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.company_name, ''), '[^[:alnum:]]+', ' ', 'g')))
                        LIKE :prefixPattern ESCAPE '\\'
                        THEN 680
                    ELSE 600
                END AS relevance_score,
                internship.created_at
            FROM internships internship
            WHERE internship.deleted = FALSE
              AND internship.status IN ('OPEN', 'CLOSED', 'EXPIRED')
              AND BTRIM(LOWER(REGEXP_REPLACE(COALESCE(internship.company_name, ''), '[^[:alnum:]]+', ' ', 'g')))
                    LIKE :searchPattern ESCAPE '\\'
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SearchQueryNormalizer normalizer;

    public JdbcGlobalSearchRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            SearchQueryNormalizer normalizer) {
        this.jdbcTemplate = jdbcTemplate;
        this.normalizer = normalizer;
    }

    @Override
    public SearchRepositoryResult search(
            String normalizedQuery,
            Set<SearchType> types,
            long offset,
            int limit,
            SearchSort sort) {
        MapSqlParameterSource parameters = parameters(normalizedQuery)
                .addValue(
                        "types",
                        types.stream().map(Enum::name).toList())
                .addValue("offset", offset)
                .addValue("limit", limit);

        String filteredDocuments = """
                SELECT *
                FROM (
                %s
                ) search_documents
                WHERE search_type IN (:types)
                """.formatted(SEARCH_DOCUMENTS);
        String searchSql = filteredDocuments
                + orderBy(sort)
                + "\nLIMIT :limit OFFSET :offset";
        String countSql = """
                SELECT COUNT(*)
                FROM (
                %s
                ) counted_documents
                """.formatted(filteredDocuments);

        List<SearchDocument> documents = jdbcTemplate.query(
                searchSql,
                parameters,
                this::mapDocument);
        Long totalElements = jdbcTemplate.queryForObject(
                countSql,
                parameters,
                Long.class);
        return new SearchRepositoryResult(
                documents,
                totalElements == null ? 0 : totalElements);
    }

    @Override
    public List<String> findSuggestions(
            String normalizedQuery,
            int limit) {
        MapSqlParameterSource parameters = parameters(normalizedQuery)
                .addValue("suggestionLimit", limit);
        String sql = """
                WITH matching_documents AS (
                %s
                ),
                suggestion_candidates AS (
                    SELECT
                        title AS suggestion,
                        relevance_score,
                        created_at
                    FROM matching_documents

                    UNION ALL

                    SELECT
                        owner_name AS suggestion,
                        relevance_score - 120 AS relevance_score,
                        created_at
                    FROM matching_documents
                    WHERE owner_name IS NOT NULL
                      AND BTRIM(LOWER(REGEXP_REPLACE(owner_name, '[^[:alnum:]]+', ' ', 'g')))
                            LIKE :searchPattern ESCAPE '\\'

                    UNION ALL

                    SELECT
                        category AS suggestion,
                        relevance_score - 140 AS relevance_score,
                        created_at
                    FROM matching_documents
                    WHERE category IS NOT NULL
                      AND BTRIM(LOWER(REGEXP_REPLACE(category, '[^[:alnum:]]+', ' ', 'g')))
                            LIKE :searchPattern ESCAPE '\\'

                    UNION ALL

                    SELECT
                        location AS suggestion,
                        relevance_score - 160 AS relevance_score,
                        created_at
                    FROM matching_documents
                    WHERE location IS NOT NULL
                      AND BTRIM(LOWER(REGEXP_REPLACE(location, '[^[:alnum:]]+', ' ', 'g')))
                            LIKE :searchPattern ESCAPE '\\'

                    UNION ALL

                    SELECT
                        company_name AS suggestion,
                        relevance_score - 100 AS relevance_score,
                        created_at
                    FROM matching_documents
                    WHERE company_name IS NOT NULL
                      AND BTRIM(LOWER(REGEXP_REPLACE(company_name, '[^[:alnum:]]+', ' ', 'g')))
                            LIKE :searchPattern ESCAPE '\\'

                    UNION ALL

                %s
                ),
                deduplicated AS (
                    SELECT DISTINCT ON (LOWER(suggestion))
                        suggestion,
                        relevance_score,
                        created_at
                    FROM suggestion_candidates
                    WHERE suggestion IS NOT NULL
                      AND BTRIM(suggestion) <> ''
                    ORDER BY
                        LOWER(suggestion),
                        relevance_score DESC,
                        created_at DESC
                )
                SELECT suggestion
                FROM deduplicated
                ORDER BY
                    relevance_score DESC,
                    created_at DESC,
                    LOWER(suggestion)
                LIMIT :suggestionLimit
                """.formatted(
                SEARCH_DOCUMENTS,
                INTERNSHIP_COMPANY_SUGGESTIONS);
        return jdbcTemplate.queryForList(
                sql,
                parameters,
                String.class);
    }

    private SearchDocument mapDocument(
            ResultSet resultSet,
            int rowNumber) throws SQLException {
        return new SearchDocument(
                resultSet.getObject("id", UUID.class),
                SearchType.valueOf(resultSet.getString("search_type")),
                resultSet.getString("title"),
                resultSet.getString("snippet_source"),
                resultSet.getString("owner_name"),
                resultSet.getString("category"),
                resultSet.getString("location"),
                resultSet.getString("company_name"),
                resultSet.getBigDecimal("price"),
                resultSet.getString("currency"),
                resultSet.getString("status"),
                instant(resultSet, "relevant_date"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"),
                resultSet.getInt("relevance_score"));
    }

    private MapSqlParameterSource parameters(String normalizedQuery) {
        String compactQuery = normalizer.compact(normalizedQuery);
        return new MapSqlParameterSource()
                .addValue("exactQuery", normalizedQuery)
                .addValue("compactExactQuery", compactQuery)
                .addValue("tokensText", normalizedQuery)
                .addValue("prefixPattern", normalizer.prefixPattern(normalizedQuery))
                .addValue("compactPrefixPattern", normalizer.prefixPattern(compactQuery))
                .addValue("initialPattern", normalizer.prefixPattern(compactQuery))
                .addValue("wholeWordPattern", normalizer.wholeWordPattern(normalizedQuery))
                .addValue("searchPattern", normalizer.likePattern(normalizedQuery))
                .addValue("compactSearchPattern", normalizer.likePattern(compactQuery));
    }

    private String orderBy(SearchSort sort) {
        return switch (sort) {
            case RELEVANCE -> """

                    ORDER BY
                        relevance_score DESC,
                        created_at DESC,
                        search_type ASC,
                        id ASC
                    """;
            case NEWEST -> """

                    ORDER BY
                        created_at DESC,
                        search_type ASC,
                        id ASC
                    """;
            case OLDEST -> """

                    ORDER BY
                        created_at ASC,
                        search_type ASC,
                        id ASC
                    """;
        };
    }

    private Instant instant(
            ResultSet resultSet,
            String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
