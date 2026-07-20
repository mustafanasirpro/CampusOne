package com.campusone.aura.repository;

import com.campusone.aura.dto.AuraRegistrationDtos;
import com.campusone.aura.dto.AuraRegistrationDtos.PersonalTimetableEntry;
import com.campusone.aura.dto.AuraRegistrationDtos.StudentRegistrationResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
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
public class AuraRegistrationRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AuraRegistrationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean studentBelongsToUniversity(UUID studentUserId, UUID universityId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM student_profiles profile
                    WHERE profile.user_id = :studentUserId
                      AND profile.university_id = :universityId
                )
                """, params()
                .addValue("studentUserId", studentUserId)
                .addValue("universityId", universityId), Boolean.class));
    }

    public boolean offeringBelongsToTerm(UUID offeringId, UUID termId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM aura_course_offerings
                    WHERE id = :offeringId AND term_id = :termId
                )
                """, params()
                .addValue("offeringId", offeringId)
                .addValue("termId", termId), Boolean.class));
    }

    public boolean sectionBelongsToTermUniversity(
            UUID sectionId,
            UUID termId,
            UUID universityId) {
        if (sectionId == null) {
            return true;
        }
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM aura_sections section_row
                    JOIN aura_batches batch ON batch.id = section_row.batch_id
                    JOIN aura_programs program ON program.id = batch.program_id
                    WHERE section_row.id = :sectionId
                      AND program.university_id = :universityId
                      AND (section_row.term_id IS NULL
                          OR section_row.term_id = :termId)
                )
                """, params()
                .addValue("sectionId", sectionId)
                .addValue("universityId", universityId)
                .addValue("termId", termId), Boolean.class));
    }

    public boolean groupMatchesOfferingAndType(
            UUID groupId,
            UUID offeringId,
            String groupType) {
        if (groupId == null) {
            return true;
        }
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM aura_teaching_groups
                    WHERE id = :groupId
                      AND offering_id = :offeringId
                      AND group_type = :groupType
                      AND active = TRUE
                )
                """, params()
                .addValue("groupId", groupId)
                .addValue("offeringId", offeringId)
                .addValue("groupType", groupType), Boolean.class));
    }

    public UUID insertRegistration(
            UUID id,
            UUID universityId,
            UUID actorUserId,
            AuraRegistrationDtos.CreateRegistrationRequest request,
            String registrationType) {
        jdbc.update("""
                INSERT INTO aura_student_course_registrations (
                    id, university_id, term_id, student_user_id, offering_id,
                    registration_type, status, home_section_id,
                    teaching_section_id, lecture_group_id, lab_group_id,
                    tutorial_group_id, equivalent_offering_id,
                    created_by_user_id
                ) VALUES (
                    :id, :universityId, :termId, :studentUserId, :offeringId,
                    :registrationType, 'ACTIVE', :homeSectionId,
                    :teachingSectionId, :lectureGroupId, :labGroupId,
                    :tutorialGroupId, :equivalentOfferingId, :actorUserId
                )
                """, params()
                .addValue("id", id)
                .addValue("universityId", universityId)
                .addValue("termId", request.termId())
                .addValue("studentUserId", request.studentUserId())
                .addValue("offeringId", request.offeringId())
                .addValue("registrationType", registrationType)
                .addValue("homeSectionId", request.homeSectionId())
                .addValue("teachingSectionId", request.teachingSectionId())
                .addValue("lectureGroupId", request.lectureGroupId())
                .addValue("labGroupId", request.labGroupId())
                .addValue("tutorialGroupId", request.tutorialGroupId())
                .addValue("equivalentOfferingId", request.equivalentOfferingId())
                .addValue("actorUserId", actorUserId));
        return id;
    }

    public Optional<StudentRegistrationResponse> findRegistration(UUID id) {
        List<StudentRegistrationResponse> rows = jdbc.query(
                registrationSelect() + " WHERE registration.id = :id",
                params().addValue("id", id),
                this::mapRegistration);
        return rows.stream().findFirst();
    }

    public List<StudentRegistrationResponse> listRegistrations(
            UUID termId,
            UUID studentUserId) {
        String studentFilter = studentUserId == null
                ? ""
                : " AND registration.student_user_id = :studentUserId\n";
        return jdbc.query(
                registrationSelect() + """
                        WHERE registration.term_id = :termId
                        """ + studentFilter + """
                        ORDER BY profile.full_name, course.course_code,
                            registration.created_at
                        """,
                params()
                        .addValue("termId", termId)
                        .addValue("studentUserId", studentUserId),
                this::mapRegistration);
    }

    public boolean updateRegistration(
            UUID id,
            AuraRegistrationDtos.UpdateRegistrationRequest request,
            String status) {
        return jdbc.update("""
                UPDATE aura_student_course_registrations
                SET status = :status,
                    teaching_section_id = :teachingSectionId,
                    lecture_group_id = :lectureGroupId,
                    lab_group_id = :labGroupId,
                    tutorial_group_id = :tutorialGroupId,
                    equivalent_offering_id = :equivalentOfferingId,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :id AND version = :version
                """, params()
                .addValue("id", id)
                .addValue("status", status)
                .addValue("teachingSectionId", request.teachingSectionId())
                .addValue("lectureGroupId", request.lectureGroupId())
                .addValue("labGroupId", request.labGroupId())
                .addValue("tutorialGroupId", request.tutorialGroupId())
                .addValue("equivalentOfferingId", request.equivalentOfferingId())
                .addValue("version", request.version())) == 1;
    }

    public List<PersonalTimetableEntry> personalTimetable(
            UUID termId,
            UUID studentUserId) {
        return jdbc.query("""
                SELECT session.id AS session_id,
                    offering.id AS offering_id,
                    course.course_code,
                    course.title AS course_title,
                    instructor.display_name AS instructor_name,
                    section_row.display_name AS section_name,
                    room.display_name AS room_name,
                    slot.day_of_week, slot.starts_at,
                    COALESCE((
                        SELECT block_slot.ends_at
                        FROM aura_timeslots block_slot
                        WHERE block_slot.term_id = registration.term_id
                          AND block_slot.day_of_week = slot.day_of_week
                          AND block_slot.slot_order = slot.slot_order
                              + requirement.duration_slots - 1
                          AND block_slot.active = TRUE
                    ), slot.ends_at) AS ends_at,
                    registration.registration_type,
                    requirement.week_pattern, requirement.custom_weeks
                FROM aura_student_course_registrations registration
                JOIN aura_course_offerings offering
                    ON offering.id = registration.offering_id
                JOIN courses course ON course.id = offering.course_id
                JOIN aura_timetable_versions timetable_version
                    ON timetable_version.term_id = registration.term_id
                    AND timetable_version.status = 'PUBLISHED'
                JOIN aura_scheduled_sessions session
                    ON session.version_id = timetable_version.id
                    AND session.offering_id = offering.id
                JOIN aura_meeting_requirements requirement
                    ON requirement.id = session.meeting_requirement_id
                JOIN aura_instructors instructor
                    ON instructor.id = session.instructor_id
                JOIN aura_sections section_row ON section_row.id = session.section_id
                JOIN aura_rooms room ON room.id = session.room_id
                JOIN aura_timeslots slot ON slot.id = session.timeslot_id
                WHERE registration.term_id = :termId
                  AND registration.student_user_id = :studentUserId
                  AND registration.status = 'ACTIVE'
                  AND (registration.teaching_section_id IS NULL
                      OR registration.teaching_section_id = session.section_id)
                  AND (registration.lecture_group_id IS NULL
                      OR requirement.meeting_type <> 'LECTURE'
                      OR requirement.teaching_group IS NULL
                      OR requirement.teaching_group = (
                          SELECT lecture_group.code
                          FROM aura_teaching_groups lecture_group
                          WHERE lecture_group.id = registration.lecture_group_id))
                  AND (registration.lab_group_id IS NULL
                      OR requirement.meeting_type <> 'LAB'
                      OR requirement.teaching_group IS NULL
                      OR requirement.teaching_group = (
                          SELECT lab_group.code
                          FROM aura_teaching_groups lab_group
                          WHERE lab_group.id = registration.lab_group_id))
                  AND (registration.tutorial_group_id IS NULL
                      OR requirement.meeting_type <> 'TUTORIAL'
                      OR requirement.teaching_group IS NULL
                      OR requirement.teaching_group = (
                          SELECT tutorial_group.code
                          FROM aura_teaching_groups tutorial_group
                          WHERE tutorial_group.id = registration.tutorial_group_id))
                ORDER BY slot.day_of_week, slot.starts_at, course.course_code
                """, params()
                .addValue("termId", termId)
                .addValue("studentUserId", studentUserId), (rs, rowNum) ->
                new PersonalTimetableEntry(
                        uuid(rs, "session_id"),
                        uuid(rs, "offering_id"),
                        rs.getString("course_code"),
                        rs.getString("course_title"),
                        rs.getString("instructor_name"),
                        rs.getString("section_name"),
                        rs.getString("room_name"),
                        rs.getInt("day_of_week"),
                        rs.getObject("starts_at", LocalTime.class),
                        rs.getObject("ends_at", LocalTime.class),
                        rs.getString("registration_type"),
                        rs.getString("week_pattern"),
                        intArray(rs, "custom_weeks"),
                        false));
    }

    private String registrationSelect() {
        return """
                SELECT registration.id, registration.term_id,
                    registration.student_user_id, profile.full_name AS student_name,
                    registration.offering_id, course.course_code,
                    course.title AS course_title, registration.registration_type,
                    registration.status, registration.home_section_id,
                    registration.teaching_section_id,
                    registration.lecture_group_id, registration.lab_group_id,
                    registration.tutorial_group_id,
                    registration.equivalent_offering_id,
                    registration.created_at, registration.updated_at,
                    registration.version
                FROM aura_student_course_registrations registration
                JOIN student_profiles profile
                    ON profile.user_id = registration.student_user_id
                JOIN aura_course_offerings offering
                    ON offering.id = registration.offering_id
                JOIN courses course ON course.id = offering.course_id
                """;
    }

    private StudentRegistrationResponse mapRegistration(ResultSet rs, int rowNum)
            throws SQLException {
        return new StudentRegistrationResponse(
                uuid(rs, "id"),
                uuid(rs, "term_id"),
                uuid(rs, "student_user_id"),
                rs.getString("student_name"),
                uuid(rs, "offering_id"),
                rs.getString("course_code"),
                rs.getString("course_title"),
                rs.getString("registration_type"),
                rs.getString("status"),
                nullableUuid(rs, "home_section_id"),
                nullableUuid(rs, "teaching_section_id"),
                nullableUuid(rs, "lecture_group_id"),
                nullableUuid(rs, "lab_group_id"),
                nullableUuid(rs, "tutorial_group_id"),
                nullableUuid(rs, "equivalent_offering_id"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"),
                rs.getLong("version"));
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

    private List<Integer> intArray(ResultSet rs, String column)
            throws SQLException {
        java.sql.Array sqlArray = rs.getArray(column);
        if (sqlArray == null || !(sqlArray.getArray() instanceof Object[] values)) {
            return List.of();
        }
        return java.util.Arrays.stream(values)
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::intValue)
                .toList();
    }

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource();
    }
}
