package com.campusone.aura.repository;

import com.campusone.aura.dto.AuraScenarioDtos.EmergencyRepairResponse;
import com.campusone.aura.dto.AuraScenarioDtos.WhatIfResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraScenarioRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AuraScenarioRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public boolean isPublishedVersionForTerm(UUID versionId, UUID termId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM aura_timetable_versions
                    WHERE id = :versionId AND term_id = :termId
                      AND status = 'PUBLISHED')
                """, params().addValue("versionId", versionId)
                .addValue("termId", termId), Boolean.class));
    }

    public int countAffectedSessions(
            UUID versionId,
            String scenarioType,
            UUID resourceId) {
        String column = resourceColumn(scenarioType);
        if (column == null || resourceId == null) return 0;
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM aura_scheduled_sessions
                WHERE version_id = :versionId AND """ + column + " = :resourceId",
                params().addValue("versionId", versionId)
                        .addValue("resourceId", resourceId), Integer.class);
        return count == null ? 0 : count;
    }

    public UUID insertCompletedWhatIf(
            UUID id,
            UUID universityId,
            UUID termId,
            UUID versionId,
            UUID userId,
            String scenarioType,
            Map<String, Object> input,
            int affectedSessions,
            String recommendation) {
        jdbc.update("""
                INSERT INTO aura_what_if_runs (
                    id, university_id, term_id, source_version_id,
                    requested_by_user_id, scenario_type, scenario_input,
                    status, result_metrics, recommendation,
                    started_at, completed_at
                ) VALUES (
                    :id, :universityId, :termId, :versionId,
                    :userId, :scenarioType, CAST(:scenarioInput AS JSONB),
                    'COMPLETED', CAST(:metrics AS JSONB), :recommendation,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, params().addValue("id", id)
                .addValue("universityId", universityId)
                .addValue("termId", termId)
                .addValue("versionId", versionId)
                .addValue("userId", userId)
                .addValue("scenarioType", scenarioType)
                .addValue("scenarioInput", json(input))
                .addValue("metrics", json(Map.of(
                        "affectedSessions", affectedSessions,
                        "clashesAdded", 0,
                        "clashesRemoved", 0)))
                .addValue("recommendation", recommendation));
        return id;
    }

    public Optional<WhatIfResponse> findWhatIf(UUID id) {
        return jdbc.query("""
                SELECT id, term_id, source_version_id, scenario_type, status,
                    COALESCE((result_metrics->>'affectedSessions')::INTEGER, 0)
                        AS affected_sessions,
                    COALESCE((result_metrics->>'clashesAdded')::INTEGER, 0)
                        AS clashes_added,
                    COALESCE((result_metrics->>'clashesRemoved')::INTEGER, 0)
                        AS clashes_removed,
                    recommendation, created_at, completed_at
                FROM aura_what_if_runs WHERE id = :id
                """, params().addValue("id", id), this::mapWhatIf)
                .stream().findFirst();
    }

    public List<WhatIfResponse> listWhatIf(UUID termId) {
        return jdbc.query("""
                SELECT id, term_id, source_version_id, scenario_type, status,
                    COALESCE((result_metrics->>'affectedSessions')::INTEGER, 0)
                        AS affected_sessions,
                    COALESCE((result_metrics->>'clashesAdded')::INTEGER, 0)
                        AS clashes_added,
                    COALESCE((result_metrics->>'clashesRemoved')::INTEGER, 0)
                        AS clashes_removed,
                    recommendation, created_at, completed_at
                FROM aura_what_if_runs WHERE term_id = :termId
                ORDER BY created_at DESC LIMIT 100
                """, params().addValue("termId", termId), this::mapWhatIf);
    }

    public UUID insertEmergency(
            UUID id,
            UUID universityId,
            UUID termId,
            UUID sourceVersionId,
            UUID userId,
            String emergencyType,
            UUID affectedResourceId,
            String reason,
            int affectedSessions) {
        jdbc.update("""
                INSERT INTO aura_emergency_repair_requests (
                    id, university_id, term_id, source_version_id,
                    requested_by_user_id, emergency_type,
                    affected_resource_id, reason, status, impact)
                VALUES (
                    :id, :universityId, :termId, :sourceVersionId,
                    :userId, :emergencyType, :affectedResourceId, :reason,
                    'ANALYZING', CAST(:impact AS JSONB))
                """, params().addValue("id", id)
                .addValue("universityId", universityId)
                .addValue("termId", termId)
                .addValue("sourceVersionId", sourceVersionId)
                .addValue("userId", userId)
                .addValue("emergencyType", emergencyType)
                .addValue("affectedResourceId", affectedResourceId)
                .addValue("reason", reason.trim())
                .addValue("impact", json(Map.of(
                        "affectedSessions", affectedSessions))));
        return id;
    }

    public void completeEmergencyDraft(
            UUID requestId,
            UUID draftVersionId,
            String emergencyType,
            UUID affectedResourceId) {
        String column = resourceColumn(emergencyType);
        if (column != null) {
            jdbc.update("""
                    UPDATE aura_scheduled_sessions
                    SET pinned = TRUE, locked = TRUE,
                        lock_reason = 'Pinned outside the emergency repair scope',
                        updated_at = CURRENT_TIMESTAMP,
                        version = version + 1
                    WHERE version_id = :draftVersionId
                      AND """ + column + " <> :affectedResourceId",
                    params().addValue("draftVersionId", draftVersionId)
                            .addValue("affectedResourceId", affectedResourceId));
        }
        jdbc.update("""
                UPDATE aura_emergency_repair_requests
                SET draft_version_id = :draftVersionId,
                    status = 'DRAFT_READY', updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :requestId AND status = 'ANALYZING'
                """, params().addValue("requestId", requestId)
                .addValue("draftVersionId", draftVersionId));
    }

    public Optional<EmergencyRepairResponse> findEmergency(UUID id) {
        return jdbc.query("""
                SELECT id, term_id, source_version_id, draft_version_id,
                    emergency_type, affected_resource_id, reason, status,
                    COALESCE((impact->>'affectedSessions')::INTEGER, 0)
                        AS affected_sessions,
                    created_at
                FROM aura_emergency_repair_requests WHERE id = :id
                """, params().addValue("id", id), this::mapEmergency)
                .stream().findFirst();
    }

    public List<EmergencyRepairResponse> listEmergencies(UUID termId) {
        return jdbc.query("""
                SELECT id, term_id, source_version_id, draft_version_id,
                    emergency_type, affected_resource_id, reason, status,
                    COALESCE((impact->>'affectedSessions')::INTEGER, 0)
                        AS affected_sessions,
                    created_at
                FROM aura_emergency_repair_requests WHERE term_id = :termId
                ORDER BY created_at DESC LIMIT 100
                """, params().addValue("termId", termId), this::mapEmergency);
    }

    private WhatIfResponse mapWhatIf(ResultSet rs, int rowNum) throws SQLException {
        return new WhatIfResponse(
                uuid(rs, "id"), uuid(rs, "term_id"),
                uuid(rs, "source_version_id"), rs.getString("scenario_type"),
                rs.getString("status"), rs.getInt("affected_sessions"),
                rs.getInt("clashes_added"), rs.getInt("clashes_removed"),
                rs.getString("recommendation"), instant(rs, "created_at"),
                instant(rs, "completed_at"));
    }

    private EmergencyRepairResponse mapEmergency(ResultSet rs, int rowNum)
            throws SQLException {
        return new EmergencyRepairResponse(
                uuid(rs, "id"), uuid(rs, "term_id"),
                uuid(rs, "source_version_id"), nullableUuid(rs, "draft_version_id"),
                rs.getString("emergency_type"), uuid(rs, "affected_resource_id"),
                rs.getString("reason"), rs.getString("status"),
                rs.getInt("affected_sessions"), instant(rs, "created_at"));
    }

    private String resourceColumn(String type) {
        return switch (type) {
            case "ROOM_UNAVAILABLE", "ROOM_CLOSURE", "FACILITY_OUTAGE" -> "room_id";
            case "INSTRUCTOR_UNAVAILABLE", "INSTRUCTOR_ABSENCE" -> "instructor_id";
            case "TIMESLOT_REMOVED", "TIMESLOT_CANCELLATION", "UNIVERSITY_EVENT" -> "timeslot_id";
            case "SECTION_RESTRICTION" -> "section_id";
            default -> null;
        };
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Scenario input could not be processed.", exception);
        }
    }

    private UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private UUID nullableUuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource();
    }
}
