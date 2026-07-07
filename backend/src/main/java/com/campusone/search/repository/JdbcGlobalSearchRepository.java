package com.campusone.search.repository;

import com.campusone.search.dto.SearchSort;
import com.campusone.search.dto.SearchType;
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
                course.title AS category,
                NULL::VARCHAR AS location,
                NULL::VARCHAR AS company_name,
                NULL::NUMERIC AS price,
                NULL::VARCHAR AS currency,
                note.moderation_status AS status,
                note.published_at AS relevant_date,
                note.created_at,
                note.updated_at,
                CASE
                    WHEN LOWER(note.title) = :exactQuery THEN 100
                    WHEN LOWER(note.title) LIKE :prefixPattern ESCAPE '\\'
                        THEN 80
                    WHEN LOWER(note.title) LIKE :searchPattern ESCAPE '\\'
                        THEN 60
                    WHEN LOWER(course.title) = :exactQuery THEN 50
                    WHEN LOWER(course.title) LIKE :prefixPattern ESCAPE '\\'
                        THEN 40
                    ELSE 20
                END AS relevance_score
            FROM notes note
            JOIN courses course ON course.id = note.course_id
            LEFT JOIN student_profiles profile
                ON profile.user_id = note.uploader_id
            WHERE note.deleted_at IS NULL
              AND note.moderation_status = 'APPROVED'
              AND note.visibility = 'PUBLIC'
              AND (
                    LOWER(note.title) LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(note.description)
                        LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(note.teacher_name)
                        LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(course.title)
                        LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(course.course_code)
                        LIKE :searchPattern ESCAPE '\\'
              )

            UNION ALL

            SELECT
                listing.id,
                'MARKETPLACE'::VARCHAR AS search_type,
                listing.title,
                listing.description AS snippet_source,
                profile.full_name AS owner_name,
                listing.category AS category,
                NULL::VARCHAR AS location,
                NULL::VARCHAR AS company_name,
                listing.price,
                listing.currency,
                listing.status,
                NULL::TIMESTAMPTZ AS relevant_date,
                listing.created_at,
                listing.updated_at,
                CASE
                    WHEN LOWER(listing.title) = :exactQuery THEN 100
                    WHEN LOWER(listing.title)
                        LIKE :prefixPattern ESCAPE '\\'
                        THEN 80
                    WHEN LOWER(listing.title)
                        LIKE :searchPattern ESCAPE '\\'
                        THEN 60
                    WHEN LOWER(REPLACE(listing.category, '_', ' '))
                        = :exactQuery THEN 40
                    ELSE 20
                END AS relevance_score
            FROM marketplace_listings listing
            LEFT JOIN student_profiles profile
                ON profile.user_id = listing.seller_id
            WHERE listing.deleted_at IS NULL
              AND listing.status = 'ACTIVE'
              AND (
                    LOWER(listing.title)
                        LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(listing.description)
                        LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(REPLACE(listing.category, '_', ' '))
                        LIKE :searchPattern ESCAPE '\\'
              )

            UNION ALL

            SELECT
                question.id,
                'DISCUSSION'::VARCHAR AS search_type,
                question.title,
                question.body AS snippet_source,
                profile.full_name AS owner_name,
                question.category AS category,
                NULL::VARCHAR AS location,
                NULL::VARCHAR AS company_name,
                NULL::NUMERIC AS price,
                NULL::VARCHAR AS currency,
                question.status,
                NULL::TIMESTAMPTZ AS relevant_date,
                question.created_at,
                question.updated_at,
                CASE
                    WHEN LOWER(question.title) = :exactQuery THEN 100
                    WHEN LOWER(question.title)
                        LIKE :prefixPattern ESCAPE '\\'
                        THEN 80
                    WHEN LOWER(question.title)
                        LIKE :searchPattern ESCAPE '\\'
                        THEN 60
                    WHEN LOWER(REPLACE(question.category, '_', ' '))
                        = :exactQuery THEN 40
                    ELSE 20
                END AS relevance_score
            FROM discussion_questions question
            LEFT JOIN student_profiles profile
                ON profile.user_id = question.author_user_id
            WHERE question.deleted = FALSE
              AND question.status IN ('OPEN', 'RESOLVED', 'CLOSED')
              AND (
                    LOWER(question.title)
                        LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(question.body)
                        LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(REPLACE(question.category, '_', ' '))
                        LIKE :searchPattern ESCAPE '\\'
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
                    WHEN LOWER(event.title) = :exactQuery THEN 100
                    WHEN LOWER(event.title)
                        LIKE :prefixPattern ESCAPE '\\'
                        THEN 80
                    WHEN LOWER(event.title)
                        LIKE :searchPattern ESCAPE '\\'
                        THEN 60
                    WHEN LOWER(event.location) = :exactQuery THEN 40
                    ELSE 20
                END AS relevance_score
            FROM events event
            LEFT JOIN student_profiles profile
                ON profile.user_id = event.organizer_user_id
            WHERE event.deleted = FALSE
              AND event.visibility = 'PUBLIC'
              AND event.status IN ('UPCOMING', 'CANCELLED', 'COMPLETED')
              AND (
                    LOWER(event.title)
                        LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(event.description)
                        LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(event.location)
                        LIKE :searchPattern ESCAPE '\\'
              )

            UNION ALL

            SELECT
                internship.id,
                'INTERNSHIP'::VARCHAR AS search_type,
                internship.title,
                internship.description AS snippet_source,
                profile.full_name AS owner_name,
                internship.internship_type AS category,
                internship.location,
                internship.company_name,
                NULL::NUMERIC AS price,
                internship.currency,
                internship.status,
                internship.deadline AS relevant_date,
                internship.created_at,
                internship.updated_at,
                CASE
                    WHEN LOWER(internship.title) = :exactQuery THEN 100
                    WHEN LOWER(internship.title)
                        LIKE :prefixPattern ESCAPE '\\'
                        THEN 80
                    WHEN LOWER(internship.title)
                        LIKE :searchPattern ESCAPE '\\'
                        THEN 60
                    WHEN LOWER(internship.company_name) = :exactQuery
                        THEN 50
                    WHEN LOWER(internship.company_name)
                        LIKE :prefixPattern ESCAPE '\\'
                        THEN 40
                    ELSE 20
                END AS relevance_score
            FROM internships internship
            LEFT JOIN student_profiles profile
                ON profile.user_id = internship.poster_user_id
            WHERE internship.deleted = FALSE
              AND internship.status IN ('OPEN', 'CLOSED', 'EXPIRED')
              AND (
                    LOWER(internship.title)
                        LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(internship.company_name)
                        LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(internship.description)
                        LIKE :searchPattern ESCAPE '\\'
                    OR LOWER(internship.location)
                        LIKE :searchPattern ESCAPE '\\'
              )
            """;

    private static final String INTERNSHIP_COMPANY_SUGGESTIONS = """
            SELECT
                internship.company_name AS suggestion,
                CASE
                    WHEN LOWER(internship.company_name) = :exactQuery
                        THEN 100
                    WHEN LOWER(internship.company_name)
                        LIKE :prefixPattern ESCAPE '\\'
                        THEN 80
                    ELSE 60
                END AS relevance_score,
                internship.created_at
            FROM internships internship
            WHERE internship.deleted = FALSE
              AND internship.status IN ('OPEN', 'CLOSED', 'EXPIRED')
              AND LOWER(internship.company_name)
                    LIKE :searchPattern ESCAPE '\\'
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcGlobalSearchRepository(
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

                %s
                ),
                deduplicated AS (
                    SELECT DISTINCT ON (LOWER(suggestion))
                        suggestion,
                        relevance_score,
                        created_at
                    FROM suggestion_candidates
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
        String escapedQuery = normalizedQuery
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return new MapSqlParameterSource()
                .addValue("exactQuery", normalizedQuery)
                .addValue("prefixPattern", escapedQuery + "%")
                .addValue("searchPattern", "%" + escapedQuery + "%");
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
