package com.campusone.aura.repository;

import com.campusone.aura.dto.AuraResolutionDtos.ResolutionCaseResponse;
import com.campusone.aura.dto.AuraResolutionDtos.ResolutionSuggestionResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
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
public class AuraResolutionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AuraResolutionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<RegistrationScope> findRegistrationScope(UUID registrationId) {
        return jdbc.query("""
                SELECT registration.id, registration.university_id,
                    registration.term_id, registration.student_user_id,
                    registration.offering_id, offering.course_id,
                    registration.registration_type,
                    registration.equivalent_offering_id,
                    registration.lecture_group_id, registration.lab_group_id,
                    registration.tutorial_group_id,
                    registration.version
                FROM aura_student_course_registrations registration
                JOIN aura_course_offerings offering
                  ON offering.id = registration.offering_id
                WHERE registration.id = :registrationId
                  AND registration.status = 'ACTIVE'
                """, params().addValue("registrationId", registrationId),
                (rs, rowNum) -> new RegistrationScope(
                        uuid(rs, "id"), uuid(rs, "university_id"),
                        uuid(rs, "term_id"), uuid(rs, "student_user_id"),
                        uuid(rs, "offering_id"), uuid(rs, "course_id"),
                        rs.getString("registration_type"),
                        nullableUuid(rs, "equivalent_offering_id"),
                        nullableUuid(rs, "lecture_group_id"),
                        nullableUuid(rs, "lab_group_id"),
                        nullableUuid(rs, "tutorial_group_id"),
                        rs.getLong("version"))).stream().findFirst();
    }

    public UUID insertCase(
            UUID id,
            RegistrationScope registration,
            String caseType,
            String summary,
            UUID requesterUserId) {
        jdbc.update("""
                INSERT INTO aura_resolution_cases (
                    id, university_id, term_id, student_user_id,
                    registration_id, status, case_type, summary,
                    requested_by_user_id
                ) VALUES (
                    :id, :universityId, :termId, :studentUserId,
                    :registrationId, 'OPEN', :caseType, :summary,
                    :requesterUserId)
                """, params()
                .addValue("id", id)
                .addValue("universityId", registration.universityId())
                .addValue("termId", registration.termId())
                .addValue("studentUserId", registration.studentUserId())
                .addValue("registrationId", registration.id())
                .addValue("caseType", caseType)
                .addValue("summary", summary.trim())
                .addValue("requesterUserId", requesterUserId));
        insertAction(id, null, requesterUserId, "REQUESTED", summary);
        return id;
    }

    public Optional<ResolutionCaseResponse> findCase(UUID caseId) {
        List<ResolutionCaseResponse> cases = jdbc.query(
                caseSelect() + " WHERE resolution_case.id = :caseId",
                params().addValue("caseId", caseId), this::mapCase);
        return cases.stream().findFirst().map(value -> withSuggestions(value, caseId));
    }

    public List<ResolutionCaseResponse> listCases(UUID termId, UUID studentUserId) {
        String studentFilter = studentUserId == null
                ? ""
                : " AND resolution_case.student_user_id = :studentUserId\n";
        return jdbc.query(caseSelect() + """
                WHERE resolution_case.term_id = :termId
                """ + studentFilter + """
                ORDER BY resolution_case.created_at DESC
                """, params()
                .addValue("termId", termId)
                .addValue("studentUserId", studentUserId), this::mapCase).stream()
                .map(value -> withSuggestions(value, value.id()))
                .toList();
    }

    public List<OfferingCandidate> parallelOfferingCandidates(
            RegistrationScope registration) {
        List<OfferingCandidate> candidates = jdbc.query("""
                SELECT candidate.id AS offering_id,
                    CASE
                        WHEN :registrationType = 'MAKEUP' THEN 'MAKEUP_SESSION'
                        WHEN candidate.course_id = source.course_id
                          AND candidate.section_id <> source.section_id
                          THEN 'SECTION_TRANSFER'
                        WHEN source.elective_group IS NOT NULL
                          AND LOWER(source.elective_group) =
                            LOWER(candidate.elective_group)
                          THEN 'EQUIVALENT_OFFERING'
                        WHEN source.parallel_group IS NOT NULL
                          AND LOWER(source.parallel_group) =
                            LOWER(candidate.parallel_group)
                          THEN 'PARALLEL_OFFERING_TRANSFER'
                        ELSE 'REGISTRATION_ADJUSTMENT'
                    END AS suggestion_type,
                    COALESCE(candidate.offering_code, course.course_code)
                        AS offering_code,
                    course.title AS course_title,
                    candidate.section_id,
                    section_row.display_name AS section_name,
                    GREATEST(COALESCE(candidate.maximum_enrollment,
                        candidate.expected_students) - (
                            SELECT COUNT(*)
                            FROM aura_student_course_registrations enrolled
                            WHERE enrolled.offering_id = candidate.id
                              AND enrolled.status IN ('ACTIVE', 'PENDING')
                        ), 0) AS remaining_capacity,
                    TRUE AS registration_compatible,
                    NOT EXISTS (
                        SELECT 1
                        FROM aura_timetable_versions published
                        JOIN aura_scheduled_sessions candidate_session
                          ON candidate_session.version_id = published.id
                         AND candidate_session.offering_id = candidate.id
                        JOIN aura_timeslots candidate_slot
                          ON candidate_slot.id = candidate_session.timeslot_id
                        JOIN aura_meeting_requirements candidate_requirement
                          ON candidate_requirement.id =
                            candidate_session.meeting_requirement_id
                        JOIN aura_student_course_registrations other_registration
                          ON other_registration.student_user_id = :studentUserId
                         AND other_registration.term_id = :termId
                         AND other_registration.status = 'ACTIVE'
                         AND other_registration.id <> :registrationId
                        JOIN aura_scheduled_sessions other_session
                          ON other_session.version_id = published.id
                         AND other_session.offering_id = other_registration.offering_id
                        JOIN aura_timeslots other_slot
                          ON other_slot.id = other_session.timeslot_id
                        WHERE published.term_id = :termId
                          AND published.status = 'PUBLISHED'
                          AND candidate_requirement.teaching_group_id IS NULL
                          AND candidate_slot.day_of_week = other_slot.day_of_week
                          AND candidate_slot.starts_at < other_slot.ends_at
                          AND other_slot.starts_at < candidate_slot.ends_at
                    ) AS clash_free
                FROM aura_course_offerings candidate
                JOIN aura_course_offerings source
                  ON source.id = :offeringId
                JOIN courses course ON course.id = candidate.course_id
                JOIN aura_sections section_row ON section_row.id = candidate.section_id
                WHERE candidate.term_id = :termId
                  AND candidate.id <> :offeringId
                  AND candidate.status = 'ACTIVE'
                  AND section_row.active = TRUE
                  AND (
                      candidate.course_id = source.course_id
                      OR source.parallel_group IS NOT NULL
                        AND candidate.parallel_group IS NOT NULL
                        AND LOWER(source.parallel_group) =
                          LOWER(candidate.parallel_group)
                      OR source.elective_group IS NOT NULL
                        AND candidate.elective_group IS NOT NULL
                        AND LOWER(source.elective_group) =
                          LOWER(candidate.elective_group)
                      OR candidate.id = CAST(:equivalentOfferingId AS UUID)
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM aura_student_course_registrations duplicate
                      WHERE duplicate.term_id = :termId
                        AND duplicate.student_user_id = :studentUserId
                        AND duplicate.offering_id = candidate.id
                        AND duplicate.status IN ('ACTIVE', 'PENDING'))
                ORDER BY clash_free DESC,
                    CASE
                        WHEN candidate.course_id = source.course_id
                          AND candidate.section_id <> source.section_id THEN 1
                        WHEN source.parallel_group IS NOT NULL
                          AND LOWER(source.parallel_group) =
                            LOWER(candidate.parallel_group) THEN 2
                        WHEN source.elective_group IS NOT NULL
                          AND LOWER(source.elective_group) =
                            LOWER(candidate.elective_group) THEN 3
                        WHEN :registrationType = 'MAKEUP' THEN 4
                        ELSE 5
                    END,
                    remaining_capacity DESC,
                    offering_code, candidate.id
                LIMIT 20
                """, params()
                .addValue("registrationId", registration.id())
                .addValue("studentUserId", registration.studentUserId())
                .addValue("termId", registration.termId())
                .addValue("courseId", registration.courseId())
                .addValue("offeringId", registration.offeringId())
                .addValue("registrationType", registration.registrationType())
                .addValue("equivalentOfferingId", registration.equivalentOfferingId()),
                (rs, rowNum) -> new OfferingCandidate(
                        uuid(rs, "offering_id"), rs.getString("suggestion_type"),
                        rs.getString("offering_code"),
                        rs.getString("course_title"), uuid(rs, "section_id"),
                        rs.getString("section_name"),
                        rs.getInt("remaining_capacity"),
                        rs.getBoolean("clash_free"),
                        rs.getBoolean("registration_compatible")));
        if (candidates.isEmpty()) {
            return candidates;
        }
        Map<UUID, GroupAssignment> assignments = compatibleGroupAssignments(
                registration,
                candidates.stream().map(OfferingCandidate::offeringId).toList());
        return candidates.stream()
                .map(candidate -> candidate.withGroupAssignment(
                        assignments.getOrDefault(
                                candidate.offeringId(),
                                GroupAssignment.noneRequired())))
                .toList();
    }

    private Map<UUID, GroupAssignment> compatibleGroupAssignments(
            RegistrationScope registration,
            List<UUID> offeringIds) {
        List<GroupOption> options = jdbc.query("""
                WITH required_types AS (
                  SELECT DISTINCT requirement.offering_id,
                    requirement.meeting_type AS group_type
                  FROM aura_meeting_requirements requirement
                  WHERE requirement.offering_id IN (:offeringIds)
                    AND requirement.active = TRUE
                    AND requirement.teaching_group_id IS NOT NULL
                    AND requirement.meeting_type IN (
                      'LECTURE', 'LAB', 'TUTORIAL')
                ),
                evaluated AS (
                  SELECT required.offering_id, required.group_type,
                    teaching_group.id AS group_id,
                    teaching_group.code AS group_code,
                    CASE WHEN teaching_group.id IS NULL THEN 0
                      ELSE GREATEST(COALESCE(
                        teaching_group.capacity, 2147483647) - (
                          SELECT COUNT(*)
                          FROM aura_student_course_registrations enrolled
                          WHERE enrolled.status IN ('ACTIVE', 'PENDING')
                            AND (enrolled.lecture_group_id = teaching_group.id
                              OR enrolled.lab_group_id = teaching_group.id
                              OR enrolled.tutorial_group_id = teaching_group.id)
                        ), 0)
                    END AS remaining_capacity,
                    teaching_group.id IS NOT NULL AND NOT EXISTS (
                      SELECT 1
                      FROM aura_timetable_versions published
                      JOIN aura_scheduled_sessions candidate_session
                        ON candidate_session.version_id = published.id
                       AND candidate_session.offering_id =
                         required.offering_id
                      JOIN aura_meeting_requirements candidate_requirement
                        ON candidate_requirement.id =
                          candidate_session.meeting_requirement_id
                       AND candidate_requirement.teaching_group_id =
                         teaching_group.id
                      JOIN aura_timeslots candidate_slot
                        ON candidate_slot.id =
                          candidate_session.timeslot_id
                      JOIN aura_student_course_registrations other_registration
                        ON other_registration.student_user_id = :studentUserId
                       AND other_registration.term_id = :termId
                       AND other_registration.status = 'ACTIVE'
                       AND other_registration.id <> :registrationId
                      JOIN aura_scheduled_sessions other_session
                        ON other_session.version_id = published.id
                       AND other_session.offering_id =
                         other_registration.offering_id
                      JOIN aura_timeslots other_slot
                        ON other_slot.id = other_session.timeslot_id
                      WHERE published.term_id = :termId
                        AND published.status = 'PUBLISHED'
                        AND candidate_slot.day_of_week =
                          other_slot.day_of_week
                        AND candidate_slot.starts_at < other_slot.ends_at
                        AND other_slot.starts_at < candidate_slot.ends_at
                    ) AS clash_free
                  FROM required_types required
                  LEFT JOIN aura_teaching_groups teaching_group
                    ON teaching_group.offering_id = required.offering_id
                   AND teaching_group.group_type = required.group_type
                   AND teaching_group.active = TRUE
                ),
                ranked AS (
                  SELECT evaluated.*,
                    ROW_NUMBER() OVER (
                      PARTITION BY offering_id, group_type
                      ORDER BY clash_free DESC, remaining_capacity DESC,
                        group_code NULLS LAST, group_id NULLS LAST) AS rank
                  FROM evaluated
                )
                SELECT offering_id, group_type, group_id,
                  remaining_capacity, clash_free
                FROM ranked
                WHERE rank = 1
                ORDER BY offering_id, group_type
                """, params()
                .addValue("offeringIds", offeringIds)
                .addValue("registrationId", registration.id())
                .addValue("studentUserId", registration.studentUserId())
                .addValue("termId", registration.termId()),
                (rs, rowNum) -> new GroupOption(
                        uuid(rs, "offering_id"),
                        rs.getString("group_type"),
                        nullableUuid(rs, "group_id"),
                        rs.getInt("remaining_capacity"),
                        rs.getBoolean("clash_free")));
        Map<UUID, GroupAssignmentBuilder> builders = new HashMap<>();
        for (GroupOption option : options) {
            builders.computeIfAbsent(
                    option.offeringId(), ignored -> new GroupAssignmentBuilder())
                    .accept(option);
        }
        Map<UUID, GroupAssignment> assignments = new HashMap<>();
        builders.forEach((offeringId, builder) ->
                assignments.put(offeringId, builder.build()));
        return Map.copyOf(assignments);
    }

    public List<GroupCandidate> alternateGroupCandidates(
            RegistrationScope registration) {
        return jdbc.query("""
                SELECT candidate.id AS group_id, candidate.group_type,
                    candidate.code AS group_code, candidate.display_name,
                    GREATEST(COALESCE(candidate.capacity, 2147483647) - (
                        SELECT COUNT(*)
                        FROM aura_student_course_registrations enrolled
                        WHERE enrolled.status IN ('ACTIVE', 'PENDING')
                          AND ((candidate.group_type = 'LAB'
                                AND enrolled.lab_group_id = candidate.id)
                            OR (candidate.group_type = 'TUTORIAL'
                                AND enrolled.tutorial_group_id = candidate.id))
                    ), 0) AS remaining_capacity,
                    NOT EXISTS (
                        SELECT 1
                        FROM aura_timetable_versions published
                        JOIN aura_scheduled_sessions candidate_session
                          ON candidate_session.version_id = published.id
                         AND candidate_session.offering_id = :offeringId
                        JOIN aura_meeting_requirements candidate_requirement
                          ON candidate_requirement.id = candidate_session.meeting_requirement_id
                         AND candidate_requirement.meeting_type = candidate.group_type
                         AND candidate_requirement.teaching_group = candidate.code
                        JOIN aura_timeslots candidate_slot
                          ON candidate_slot.id = candidate_session.timeslot_id
                        JOIN aura_student_course_registrations other_registration
                          ON other_registration.student_user_id = :studentUserId
                         AND other_registration.term_id = :termId
                         AND other_registration.status = 'ACTIVE'
                         AND other_registration.id <> :registrationId
                        JOIN aura_scheduled_sessions other_session
                          ON other_session.version_id = published.id
                         AND other_session.offering_id = other_registration.offering_id
                        JOIN aura_timeslots other_slot
                          ON other_slot.id = other_session.timeslot_id
                        WHERE published.term_id = :termId
                          AND published.status = 'PUBLISHED'
                          AND candidate_slot.day_of_week = other_slot.day_of_week
                          AND candidate_slot.starts_at < other_slot.ends_at
                          AND other_slot.starts_at < candidate_slot.ends_at
                    ) AS clash_free
                FROM aura_teaching_groups candidate
                WHERE candidate.offering_id = :offeringId
                  AND candidate.active = TRUE
                  AND candidate.group_type IN ('LAB', 'TUTORIAL')
                  AND candidate.id <> COALESCE(
                      CASE candidate.group_type
                        WHEN 'LAB' THEN :labGroupId
                        WHEN 'TUTORIAL' THEN :tutorialGroupId
                      END,
                      '00000000-0000-0000-0000-000000000000'::UUID)
                ORDER BY candidate.group_type, clash_free DESC,
                    remaining_capacity DESC, candidate.code, candidate.id
                LIMIT 40
                """, params()
                .addValue("registrationId", registration.id())
                .addValue("studentUserId", registration.studentUserId())
                .addValue("termId", registration.termId())
                .addValue("offeringId", registration.offeringId())
                .addValue("labGroupId", registration.labGroupId())
                .addValue("tutorialGroupId", registration.tutorialGroupId()),
                (rs, rowNum) -> new GroupCandidate(
                        uuid(rs, "group_id"), rs.getString("group_type"),
                        rs.getString("group_code"), rs.getString("display_name"),
                        rs.getInt("remaining_capacity"),
                        rs.getBoolean("clash_free")));
    }

    public void replaceSuggestions(
            UUID caseId,
            List<OfferingCandidate> candidates,
            List<GroupCandidate> groupCandidates) {
        jdbc.update("DELETE FROM aura_ranked_resolution_suggestions WHERE case_id = :caseId",
                params().addValue("caseId", caseId));
        int rank = 1;
        for (OfferingCandidate candidate : candidates) {
            boolean safe = candidate.clashFree()
                    && candidate.registrationCompatible()
                    && candidate.remainingCapacity() > 0;
            jdbc.update("""
                    INSERT INTO aura_ranked_resolution_suggestions (
                        id, case_id, suggestion_type, target_offering_id,
                        target_section_id, target_lecture_group_id,
                        target_lab_group_id, target_tutorial_group_id,
                        rank_order, safe,
                        hard_clashes_removed, hard_clashes_added,
                        affected_students, changed_sessions, explanation
                    ) VALUES (
                        :id, :caseId, :suggestionType,
                        :offeringId, :sectionId, :lectureGroupId,
                        :labGroupId, :tutorialGroupId, :rank, :safe,
                        1, :hardAdded, 1, 0, :explanation)
                    """, params()
                    .addValue("id", UUID.randomUUID())
                    .addValue("caseId", caseId)
                    .addValue("suggestionType", candidate.suggestionType())
                    .addValue("offeringId", candidate.offeringId())
                    .addValue("sectionId", candidate.sectionId())
                    .addValue("lectureGroupId", candidate.lectureGroupId())
                    .addValue("labGroupId", candidate.labGroupId())
                    .addValue("tutorialGroupId", candidate.tutorialGroupId())
                    .addValue("rank", rank++)
                    .addValue("safe", safe)
                    .addValue("hardAdded",
                            candidate.clashFree() && candidate.registrationCompatible()
                                    ? 0 : 1)
                    .addValue("explanation", safe
                            ? safeOfferingExplanation(candidate)
                            : candidate.remainingCapacity() <= 0
                                    ? "This offering has no remaining capacity."
                                    : !candidate.registrationCompatible()
                                            ? "This offering requires a compatible teaching-group assignment."
                                            : "This transfer would create another personal timetable clash."));
        }
        for (GroupCandidate candidate : groupCandidates) {
            boolean safe = candidate.clashFree() && candidate.remainingCapacity() > 0;
            jdbc.update("""
                    INSERT INTO aura_ranked_resolution_suggestions (
                        id, case_id, suggestion_type, target_group_id,
                        rank_order, safe, hard_clashes_removed,
                        hard_clashes_added, affected_students,
                        changed_sessions, explanation
                    ) VALUES (
                        :id, :caseId, :suggestionType, :groupId,
                        :rank, :safe, 1, :hardAdded, 1, 0, :explanation)
                    """, params()
                    .addValue("id", UUID.randomUUID())
                    .addValue("caseId", caseId)
                    .addValue("suggestionType", "ALTERNATE_"
                            + candidate.groupType())
                    .addValue("groupId", candidate.groupId())
                    .addValue("rank", rank++)
                    .addValue("safe", safe)
                    .addValue("hardAdded", candidate.clashFree() ? 0 : 1)
                    .addValue("explanation", safe
                            ? "Move only this student to " + candidate.displayName()
                                    + " without changing the university timetable."
                            : candidate.remainingCapacity() <= 0
                                    ? candidate.displayName() + " is full."
                                    : candidate.displayName()
                                            + " would create another personal clash."));
        }
    }

    public void replaceSuggestions(UUID caseId, List<OfferingCandidate> candidates) {
        replaceSuggestions(caseId, candidates, List.of());
    }

    public List<ResolutionSuggestionResponse> listSuggestions(UUID caseId) {
        return jdbc.query("""
                SELECT suggestion.id, suggestion.suggestion_type,
                    suggestion.target_offering_id,
                    COALESCE(offering.offering_code, course.course_code)
                        AS target_offering_code,
                    course.title AS target_course_title,
                    suggestion.target_section_id,
                    section_row.display_name AS target_section_name,
                    suggestion.target_group_id,
                    teaching_group.group_type AS target_group_type,
                    teaching_group.code AS target_group_code,
                    suggestion.target_lecture_group_id,
                    suggestion.target_lab_group_id,
                    suggestion.target_tutorial_group_id,
                    suggestion.rank_order,
                    suggestion.safe, suggestion.hard_clashes_removed,
                    suggestion.hard_clashes_added,
                    suggestion.affected_students, suggestion.changed_sessions,
                    suggestion.explanation, suggestion.applied_at
                FROM aura_ranked_resolution_suggestions suggestion
                LEFT JOIN aura_course_offerings offering
                  ON offering.id = suggestion.target_offering_id
                LEFT JOIN courses course ON course.id = offering.course_id
                LEFT JOIN aura_sections section_row
                  ON section_row.id = suggestion.target_section_id
                LEFT JOIN aura_teaching_groups teaching_group
                  ON teaching_group.id = suggestion.target_group_id
                WHERE suggestion.case_id = :caseId
                ORDER BY suggestion.rank_order
                """, params().addValue("caseId", caseId),
                (rs, rowNum) -> new ResolutionSuggestionResponse(
                        uuid(rs, "id"), rs.getString("suggestion_type"),
                        nullableUuid(rs, "target_offering_id"),
                        rs.getString("target_offering_code"),
                        rs.getString("target_course_title"),
                        nullableUuid(rs, "target_section_id"),
                        rs.getString("target_section_name"),
                        nullableUuid(rs, "target_group_id"),
                        rs.getString("target_group_type"),
                        rs.getString("target_group_code"),
                        nullableUuid(rs, "target_lecture_group_id"),
                        nullableUuid(rs, "target_lab_group_id"),
                        nullableUuid(rs, "target_tutorial_group_id"),
                        rs.getInt("rank_order"), rs.getBoolean("safe"),
                        rs.getInt("hard_clashes_removed"),
                        rs.getInt("hard_clashes_added"),
                        rs.getInt("affected_students"),
                        rs.getInt("changed_sessions"), rs.getString("explanation"),
                        instant(rs, "applied_at")));
    }

    public boolean updateCaseStatus(
            UUID caseId,
            long version,
            String fromStatus,
            String toStatus,
            UUID reviewerUserId,
            String reason) {
        return jdbc.update("""
                UPDATE aura_resolution_cases
                SET status = :toStatus, reviewed_by_user_id = :reviewerUserId,
                    review_reason = :reason, updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :caseId AND version = :version
                  AND status = :fromStatus
                """, params()
                .addValue("caseId", caseId)
                .addValue("version", version)
                .addValue("fromStatus", fromStatus)
                .addValue("toStatus", toStatus)
                .addValue("reviewerUserId", reviewerUserId)
                .addValue("reason", reason)) == 1;
    }

    public Optional<SuggestionTarget> findSuggestion(UUID caseId, UUID suggestionId) {
        return jdbc.query("""
                SELECT id, suggestion_type, target_offering_id,
                    target_section_id, target_group_id,
                    target_lecture_group_id, target_lab_group_id,
                    target_tutorial_group_id, safe
                FROM aura_ranked_resolution_suggestions
                WHERE id = :suggestionId AND case_id = :caseId
                """, params().addValue("suggestionId", suggestionId)
                .addValue("caseId", caseId), (rs, rowNum) -> new SuggestionTarget(
                        uuid(rs, "id"), rs.getString("suggestion_type"),
                        nullableUuid(rs, "target_offering_id"),
                        nullableUuid(rs, "target_section_id"),
                        nullableUuid(rs, "target_group_id"),
                        nullableUuid(rs, "target_lecture_group_id"),
                        nullableUuid(rs, "target_lab_group_id"),
                        nullableUuid(rs, "target_tutorial_group_id"),
                        rs.getBoolean("safe")))
                .stream().findFirst();
    }

    public void lockResolutionCapacity(
            UUID offeringId,
            Collection<UUID> teachingGroupIds) {
        jdbc.queryForObject("""
                SELECT id
                FROM aura_course_offerings
                WHERE id = :offeringId
                FOR UPDATE
                """, params().addValue("offeringId", offeringId), UUID.class);
        if (!teachingGroupIds.isEmpty()) {
            jdbc.query("""
                    SELECT id
                    FROM aura_teaching_groups
                    WHERE id IN (:teachingGroupIds)
                    ORDER BY id
                    FOR UPDATE
                    """, params().addValue("teachingGroupIds", teachingGroupIds),
                    (rs, rowNum) -> uuid(rs, "id"));
        }
    }

    public void applyOfferingTransfer(
            UUID caseId,
            UUID registrationId,
            SuggestionTarget suggestion,
            long caseVersion,
            long registrationVersion,
            UUID actorUserId,
            String reason) {
        markSuggestionAndCaseApplied(
                caseId, suggestion.id(), caseVersion);
        int updated = jdbc.update("""
                UPDATE aura_student_course_registrations
                SET offering_id = :offeringId,
                    teaching_section_id = :sectionId,
                    lecture_group_id = :lectureGroupId,
                    lab_group_id = :labGroupId,
                    tutorial_group_id = :tutorialGroupId,
                    equivalent_offering_id = NULL,
                    registration_type = 'TRANSFERRED',
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :registrationId AND status = 'ACTIVE'
                  AND version = :registrationVersion
                """, params().addValue("registrationId", registrationId)
                .addValue("offeringId", suggestion.offeringId())
                .addValue("sectionId", suggestion.sectionId())
                .addValue("lectureGroupId", suggestion.lectureGroupId())
                .addValue("labGroupId", suggestion.labGroupId())
                .addValue("tutorialGroupId", suggestion.tutorialGroupId())
                .addValue("registrationVersion", registrationVersion));
        if (updated != 1) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Registration changed before the approved resolution was applied");
        }
        insertAction(caseId, suggestion.id(), actorUserId, "APPLIED", reason);
    }

    public void insertAction(
            UUID caseId,
            UUID suggestionId,
            UUID actorUserId,
            String action,
            String reason) {
        jdbc.update("""
                INSERT INTO aura_resolution_actions (
                    id, case_id, suggestion_id, actor_user_id, action, reason)
                VALUES (:id, :caseId, :suggestionId, :actorUserId, :action, :reason)
                """, params().addValue("id", UUID.randomUUID())
                .addValue("caseId", caseId)
                .addValue("suggestionId", suggestionId)
                .addValue("actorUserId", actorUserId)
                .addValue("action", action)
                .addValue("reason", reason));
    }

    public void applyGroupTransfer(
            UUID caseId,
            UUID registrationId,
            SuggestionTarget suggestion,
            String groupType,
            long caseVersion,
            long registrationVersion,
            UUID actorUserId,
            String reason) {
        String column = switch (groupType) {
            case "LAB" -> "lab_group_id";
            case "TUTORIAL" -> "tutorial_group_id";
            default -> throw new IllegalArgumentException("Unsupported teaching group type");
        };
        int updated = jdbc.update("""
                UPDATE aura_student_course_registrations
                SET %s = :groupId, updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :registrationId AND status = 'ACTIVE'
                  AND version = :registrationVersion
                """.formatted(column), params()
                .addValue("registrationId", registrationId)
                .addValue("groupId", suggestion.groupId())
                .addValue("registrationVersion", registrationVersion));
        if (updated != 1) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Registration is no longer active");
        }
        markSuggestionAndCaseApplied(caseId, suggestion.id(), caseVersion);
        insertAction(caseId, suggestion.id(), actorUserId, "APPLIED", reason);
    }

    private void markSuggestionAndCaseApplied(
            UUID caseId,
            UUID suggestionId,
            long caseVersion) {
        int caseUpdated = jdbc.update("""
                UPDATE aura_resolution_cases
                SET status = 'APPLIED', updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :caseId AND status = 'APPROVED'
                  AND version = :caseVersion
                """, params().addValue("caseId", caseId)
                .addValue("caseVersion", caseVersion));
        if (caseUpdated != 1) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Resolution case changed before it was applied");
        }
        int suggestionUpdated = jdbc.update("""
                UPDATE aura_ranked_resolution_suggestions
                SET applied_at = CURRENT_TIMESTAMP
                WHERE id = :suggestionId AND case_id = :caseId
                  AND applied_at IS NULL
                """, params().addValue("suggestionId", suggestionId)
                .addValue("caseId", caseId));
        if (suggestionUpdated != 1) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Resolution suggestion changed before it was applied");
        }
    }

    private ResolutionCaseResponse withSuggestions(
            ResolutionCaseResponse value,
            UUID caseId) {
        return new ResolutionCaseResponse(
                value.id(), value.termId(), value.studentUserId(),
                value.studentName(), value.registrationId(), value.status(),
                value.caseType(), value.summary(), value.reviewReason(),
                value.version(), value.createdAt(), value.updatedAt(),
                listSuggestions(caseId));
    }

    private String caseSelect() {
        return """
                SELECT resolution_case.id, resolution_case.term_id,
                    resolution_case.student_user_id,
                    profile.full_name AS student_name,
                    resolution_case.registration_id, resolution_case.status,
                    resolution_case.case_type, resolution_case.summary,
                    resolution_case.review_reason, resolution_case.version,
                    resolution_case.created_at, resolution_case.updated_at
                FROM aura_resolution_cases resolution_case
                JOIN student_profiles profile
                  ON profile.user_id = resolution_case.student_user_id
                """;
    }

    private ResolutionCaseResponse mapCase(ResultSet rs, int rowNum)
            throws SQLException {
        return new ResolutionCaseResponse(
                uuid(rs, "id"), uuid(rs, "term_id"),
                uuid(rs, "student_user_id"), rs.getString("student_name"),
                nullableUuid(rs, "registration_id"), rs.getString("status"),
                rs.getString("case_type"), rs.getString("summary"),
                rs.getString("review_reason"), rs.getLong("version"),
                instant(rs, "created_at"), instant(rs, "updated_at"), List.of());
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

    private String safeOfferingExplanation(OfferingCandidate candidate) {
        return switch (candidate.suggestionType()) {
            case "SECTION_TRANSFER" ->
                "Move only this student to a compatible section with available capacity.";
            case "EQUIVALENT_OFFERING" ->
                "Use a configured equivalent offering without changing the institutional timetable.";
            case "MAKEUP_SESSION" ->
                "Assign this student to a compatible makeup offering with available capacity.";
            case "REGISTRATION_ADJUSTMENT" ->
                "Adjust this registration to the configured compatible offering.";
            default ->
                "Transfer only this student to a clash-free parallel offering with available capacity.";
        };
    }

    public record RegistrationScope(
            UUID id,
            UUID universityId,
            UUID termId,
            UUID studentUserId,
            UUID offeringId,
            UUID courseId,
            String registrationType,
            UUID equivalentOfferingId,
            UUID lectureGroupId,
            UUID labGroupId,
            UUID tutorialGroupId,
            long version) {

        public RegistrationScope(
                UUID id,
                UUID universityId,
                UUID termId,
                UUID studentUserId,
                UUID offeringId,
                UUID courseId,
                long version) {
            this(
                    id, universityId, termId, studentUserId, offeringId,
                    courseId, "PRIMARY_SECTION", null, null, null, null,
                    version);
        }
    }

    public record GroupCandidate(
            UUID groupId,
            String groupType,
            String groupCode,
            String displayName,
            int remainingCapacity,
            boolean clashFree) {
    }

    public record OfferingCandidate(
            UUID offeringId,
            String suggestionType,
            String offeringCode,
            String courseTitle,
            UUID sectionId,
            String sectionName,
            int remainingCapacity,
            boolean clashFree,
            boolean registrationCompatible,
            UUID lectureGroupId,
            UUID labGroupId,
            UUID tutorialGroupId) {

        public OfferingCandidate(
                UUID offeringId,
                String suggestionType,
                String offeringCode,
                String courseTitle,
                UUID sectionId,
                String sectionName,
                int remainingCapacity,
                boolean clashFree,
                boolean registrationCompatible) {
            this(
                    offeringId, suggestionType, offeringCode, courseTitle,
                    sectionId, sectionName, remainingCapacity, clashFree,
                    registrationCompatible, null, null, null);
        }

        public OfferingCandidate(
                UUID offeringId,
                String offeringCode,
                String courseTitle,
                UUID sectionId,
                String sectionName,
                int remainingCapacity,
                boolean clashFree) {
            this(
                    offeringId, "PARALLEL_OFFERING_TRANSFER", offeringCode,
                    courseTitle, sectionId, sectionName, remainingCapacity,
                    clashFree, true, null, null, null);
        }

        private OfferingCandidate withGroupAssignment(
                GroupAssignment assignment) {
            return new OfferingCandidate(
                    offeringId, suggestionType, offeringCode, courseTitle,
                    sectionId, sectionName, remainingCapacity, clashFree,
                    assignment.compatible(), assignment.lectureGroupId(),
                    assignment.labGroupId(), assignment.tutorialGroupId());
        }
    }

    public record SuggestionTarget(
            UUID id,
            String suggestionType,
            UUID offeringId,
            UUID sectionId,
            UUID groupId,
            UUID lectureGroupId,
            UUID labGroupId,
            UUID tutorialGroupId,
            boolean safe) {

        public SuggestionTarget(
                UUID id,
                String suggestionType,
                UUID offeringId,
                UUID sectionId,
                UUID groupId,
                boolean safe) {
            this(
                    id, suggestionType, offeringId, sectionId, groupId,
                    null, null, null, safe);
        }

        public SuggestionTarget(
                UUID id,
                UUID offeringId,
                UUID sectionId,
                boolean safe) {
            this(
                    id, "PARALLEL_OFFERING_TRANSFER", offeringId, sectionId,
                    null, null, null, null, safe);
        }
    }

    private record GroupOption(
            UUID offeringId,
            String groupType,
            UUID groupId,
            int remainingCapacity,
            boolean clashFree) {
    }

    private record GroupAssignment(
            UUID lectureGroupId,
            UUID labGroupId,
            UUID tutorialGroupId,
            boolean compatible) {

        private static GroupAssignment noneRequired() {
            return new GroupAssignment(null, null, null, true);
        }
    }

    private static final class GroupAssignmentBuilder {

        private UUID lectureGroupId;
        private UUID labGroupId;
        private UUID tutorialGroupId;
        private boolean compatible = true;

        private void accept(GroupOption option) {
            if (option.groupId() == null
                    || option.remainingCapacity() <= 0
                    || !option.clashFree()) {
                compatible = false;
                return;
            }
            switch (option.groupType()) {
                case "LECTURE" -> lectureGroupId = option.groupId();
                case "LAB" -> labGroupId = option.groupId();
                case "TUTORIAL" -> tutorialGroupId = option.groupId();
                default -> compatible = false;
            }
        }

        private GroupAssignment build() {
            return new GroupAssignment(
                    lectureGroupId, labGroupId, tutorialGroupId, compatible);
        }
    }
}
