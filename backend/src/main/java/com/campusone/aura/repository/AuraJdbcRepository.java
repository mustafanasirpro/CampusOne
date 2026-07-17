package com.campusone.aura.repository;

import com.campusone.aura.dto.AuraDtos;
import com.campusone.aura.dto.AuraDtos.AvailabilityResponse;
import com.campusone.aura.dto.AuraDtos.BatchResponse;
import com.campusone.aura.dto.AuraDtos.CalendarExceptionResponse;
import com.campusone.aura.dto.AuraDtos.ClashResponse;
import com.campusone.aura.dto.AuraDtos.GenerationRunResponse;
import com.campusone.aura.dto.AuraDtos.InstructorResponse;
import com.campusone.aura.dto.AuraDtos.MeetingRequirementResponse;
import com.campusone.aura.dto.AuraDtos.OfferingResponse;
import com.campusone.aura.dto.AuraDtos.ProgramResponse;
import com.campusone.aura.dto.AuraDtos.RoomResponse;
import com.campusone.aura.dto.AuraDtos.SectionResponse;
import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.dto.AuraDtos.SetupReferenceOption;
import com.campusone.aura.dto.AuraDtos.TermResponse;
import com.campusone.aura.dto.AuraDtos.TimetableVersionResponse;
import com.campusone.aura.dto.AuraDtos.TimeslotResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AuraJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID insertTerm(
            UUID id,
            UUID userId,
            AuraDtos.CreateTermRequest request) {
        jdbc.update("""
                INSERT INTO aura_academic_terms (
                    id, university_id, code, name, starts_on, ends_on,
                    created_by_user_id
                ) VALUES (
                    :id, :universityId, :code, :name, :startsOn, :endsOn,
                    :userId
                )
                """, params()
                .addValue("id", id)
                .addValue("universityId", request.universityId())
                .addValue("code", request.code().trim())
                .addValue("name", request.name().trim())
                .addValue("startsOn", request.startsOn())
                .addValue("endsOn", request.endsOn())
                .addValue("userId", userId));
        return id;
    }

    public List<TermResponse> listTerms(
            UUID universityId,
            int page,
            int size) {
        return jdbc.query("""
                SELECT * FROM aura_academic_terms
                WHERE university_id = :universityId
                ORDER BY created_at DESC
                LIMIT :size OFFSET :offset
                """, pageParams(page, size)
                .addValue("universityId", universityId), termMapper());
    }

    public long countTerms(UUID universityId) {
        return count("""
                SELECT COUNT(*)
                FROM aura_academic_terms
                WHERE university_id = :universityId
                """, params().addValue("universityId", universityId));
    }

    public List<TermResponse> listPublishedTerms(UUID universityId) {
        return jdbc.query("""
                SELECT term_row.id, term_row.university_id, term_row.code,
                    term_row.name, term_row.starts_on, term_row.ends_on,
                    term_row.status, term_row.created_at, term_row.updated_at
                FROM aura_academic_terms term_row
                WHERE term_row.university_id = :universityId
                  AND EXISTS (
                      SELECT 1 FROM aura_timetable_versions timetable_version
                      WHERE timetable_version.term_id = term_row.id
                        AND timetable_version.status = 'PUBLISHED'
                  )
                ORDER BY term_row.starts_on DESC, term_row.code
                """, params().addValue("universityId", universityId),
                termMapper());
    }

    public Optional<TermResponse> findTerm(UUID termId) {
        return optional("""
                SELECT * FROM aura_academic_terms WHERE id = :id
                """, params().addValue("id", termId), termMapper());
    }

    public String termTimezone(UUID termId) {
        return jdbc.queryForObject("""
                SELECT timezone FROM aura_academic_terms WHERE id = :id
                """, params().addValue("id", termId), String.class);
    }

    public boolean resourceBelongsToUniversity(
            ScopedResource resource,
            UUID resourceId,
            UUID universityId) {
        String sql = switch (resource) {
            case TERM -> "SELECT university_id FROM aura_academic_terms WHERE id = :resourceId";
            case PROGRAM -> "SELECT university_id FROM aura_programs WHERE id = :resourceId";
            case BATCH -> """
                    SELECT p.university_id
                    FROM aura_batches b
                    JOIN aura_programs p ON p.id = b.program_id
                    WHERE b.id = :resourceId
                    """;
            case SECTION -> """
                    SELECT p.university_id
                    FROM aura_sections s
                    JOIN aura_batches b ON b.id = s.batch_id
                    JOIN aura_programs p ON p.id = b.program_id
                    WHERE s.id = :resourceId
                    """;
            case INSTRUCTOR -> "SELECT university_id FROM aura_instructors WHERE id = :resourceId";
            case ROOM -> "SELECT university_id FROM aura_rooms WHERE id = :resourceId";
            case TIMESLOT -> "SELECT university_id FROM aura_timeslots WHERE id = :resourceId";
            case OFFERING -> """
                    SELECT t.university_id
                    FROM aura_course_offerings o
                    JOIN aura_academic_terms t ON t.id = o.term_id
                    WHERE o.id = :resourceId
                    """;
            case REQUIREMENT -> """
                    SELECT t.university_id
                    FROM aura_meeting_requirements r
                    JOIN aura_course_offerings o ON o.id = r.offering_id
                    JOIN aura_academic_terms t ON t.id = o.term_id
                    WHERE r.id = :resourceId
                    """;
            case RUN -> """
                    SELECT t.university_id
                    FROM aura_generation_runs r
                    JOIN aura_academic_terms t ON t.id = r.term_id
                    WHERE r.id = :resourceId
                    """;
            case VERSION -> """
                    SELECT t.university_id
                    FROM aura_timetable_versions v
                    JOIN aura_academic_terms t ON t.id = v.term_id
                    WHERE v.id = :resourceId
                    """;
            case SESSION -> """
                    SELECT t.university_id
                    FROM aura_scheduled_sessions s
                    JOIN aura_timetable_versions v ON v.id = s.version_id
                    JOIN aura_academic_terms t ON t.id = v.term_id
                    WHERE s.id = :resourceId
                    """;
            case CALENDAR_EXCEPTION -> """
                    SELECT t.university_id
                    FROM aura_calendar_exceptions e
                    JOIN aura_academic_terms t ON t.id = e.term_id
                    WHERE e.id = :resourceId
                    """;
        };
        return optionalUuid(
                sql,
                params().addValue("resourceId", resourceId))
                .filter(universityId::equals)
                .isPresent();
    }

    public boolean courseBelongsToUniversity(
            UUID courseId,
            UUID universityId) {
        return optionalUuid("""
                SELECT d.university_id
                FROM courses c
                JOIN departments d ON d.id = c.department_id
                WHERE c.id = :courseId
                  AND c.active = TRUE
                """, params().addValue("courseId", courseId))
                .filter(universityId::equals)
                .isPresent();
    }

    public boolean departmentBelongsToUniversity(
            UUID departmentId,
            UUID universityId) {
        return optionalUuid("""
                SELECT university_id
                FROM departments
                WHERE id = :departmentId
                  AND active = TRUE
                """, params().addValue("departmentId", departmentId))
                .filter(universityId::equals)
                .isPresent();
    }

    public boolean userBelongsToUniversity(
            UUID userId,
            UUID universityId) {
        return optionalUuid("""
                SELECT university_id
                FROM student_profiles
                WHERE user_id = :userId
                """, params().addValue("userId", userId))
                .filter(universityId::equals)
                .isPresent();
    }

    public List<SetupReferenceOption> listDepartmentReferences(
            UUID universityId) {
        return jdbc.query("""
                SELECT id, university_id AS parent_id, code, name
                FROM departments
                WHERE university_id = :universityId
                  AND active = TRUE
                ORDER BY name ASC
                """, params().addValue("universityId", universityId),
                setupReferenceMapper());
    }

    public List<SetupReferenceOption> listCourseReferences(UUID universityId) {
        return jdbc.query("""
                SELECT c.id, c.department_id AS parent_id,
                    c.course_code AS code, c.title AS name
                FROM courses c
                JOIN departments d ON d.id = c.department_id
                WHERE d.university_id = :universityId
                  AND d.active = TRUE
                  AND c.active = TRUE
                ORDER BY c.course_code ASC, c.title ASC
                """, params().addValue("universityId", universityId),
                setupReferenceMapper());
    }

    public List<SetupReferenceOption> listStudentReferences(UUID universityId) {
        return jdbc.query("""
                SELECT profile.user_id AS id, profile.department_id AS parent_id,
                    'Semester ' || profile.semester AS code,
                    profile.full_name AS name
                FROM student_profiles profile
                JOIN users user_row ON user_row.id = profile.user_id
                WHERE profile.university_id = :universityId
                  AND user_row.account_status = 'ACTIVE'
                ORDER BY LOWER(profile.full_name), profile.user_id
                """, params().addValue("universityId", universityId),
                (rs, rowNum) -> new SetupReferenceOption(
                        uuid(rs, "id"),
                        uuid(rs, "parent_id"),
                        rs.getString("code"),
                        rs.getString("name")));
    }

    public UUID insertProgram(
            UUID id,
            AuraDtos.CreateProgramRequest request) {
        jdbc.update("""
                INSERT INTO aura_programs (
                    id, university_id, department_id, code, name
                ) VALUES (:id, :universityId, :departmentId, :code, :name)
                """, params()
                .addValue("id", id)
                .addValue("universityId", request.universityId())
                .addValue("departmentId", request.departmentId())
                .addValue("code", request.code().trim())
                .addValue("name", request.name().trim()));
        return id;
    }

    public List<ProgramResponse> listPrograms(UUID universityId) {
        return jdbc.query("""
                SELECT * FROM aura_programs
                WHERE (:universityId IS NULL OR university_id = :universityId)
                ORDER BY name ASC
                """, params().addValue("universityId", universityId),
                programMapper());
    }

    public UUID insertBatch(UUID id, AuraDtos.CreateBatchRequest request) {
        jdbc.update("""
                INSERT INTO aura_batches (
                    id, program_id, code, admission_year
                ) VALUES (:id, :programId, :code, :admissionYear)
                """, params()
                .addValue("id", id)
                .addValue("programId", request.programId())
                .addValue("code", request.code().trim())
                .addValue("admissionYear", request.admissionYear()));
        return id;
    }

    public List<BatchResponse> listBatches(
            UUID universityId,
            UUID programId) {
        return jdbc.query("""
                SELECT b.*
                FROM aura_batches b
                JOIN aura_programs p ON p.id = b.program_id
                WHERE p.university_id = :universityId
                  AND (:programId IS NULL OR b.program_id = :programId)
                ORDER BY b.admission_year DESC, b.code ASC
                """, params()
                .addValue("universityId", universityId)
                .addValue("programId", programId), batchMapper());
    }

    public UUID insertSection(UUID id, AuraDtos.CreateSectionRequest request) {
        jdbc.update("""
                INSERT INTO aura_sections (
                    id, batch_id, code, display_name, student_count
                ) VALUES (:id, :batchId, :code, :displayName, :studentCount)
                """, params()
                .addValue("id", id)
                .addValue("batchId", request.batchId())
                .addValue("code", request.code().trim())
                .addValue("displayName", request.displayName().trim())
                .addValue("studentCount", request.studentCount()));
        return id;
    }

    public List<SectionResponse> listSections(
            UUID universityId,
            UUID batchId) {
        return jdbc.query("""
                SELECT s.*
                FROM aura_sections s
                JOIN aura_batches b ON b.id = s.batch_id
                JOIN aura_programs p ON p.id = b.program_id
                WHERE p.university_id = :universityId
                  AND (:batchId IS NULL OR s.batch_id = :batchId)
                ORDER BY s.display_name ASC
                """, params()
                .addValue("universityId", universityId)
                .addValue("batchId", batchId), sectionMapper());
    }

    public UUID insertInstructor(
            UUID id,
            AuraDtos.CreateInstructorRequest request) {
        jdbc.update("""
                INSERT INTO aura_instructors (
                    id, university_id, user_id, display_name, email,
                    max_hours_per_week
                ) VALUES (
                    :id, :universityId, :userId, :displayName, :email,
                    :maxHoursPerWeek
                )
                """, params()
                .addValue("id", id)
                .addValue("universityId", request.universityId())
                .addValue("userId", request.userId())
                .addValue("displayName", request.displayName().trim())
                .addValue("email", blankToNull(request.email()))
                .addValue("maxHoursPerWeek",
                        request.maxHoursPerWeek() <= 0
                                ? 18
                                : request.maxHoursPerWeek()));
        return id;
    }

    public List<InstructorResponse> listInstructors(UUID universityId) {
        return jdbc.query("""
                SELECT * FROM aura_instructors
                WHERE (:universityId IS NULL OR university_id = :universityId)
                ORDER BY display_name ASC
                """, params().addValue("universityId", universityId),
                instructorMapper());
    }

    public UUID insertRoom(UUID id, AuraDtos.CreateRoomRequest request) {
        jdbc.update("""
                INSERT INTO aura_rooms (
                    id, university_id, building, name, display_name,
                    capacity, room_type
                ) VALUES (
                    :id, :universityId, :building, :name, :name,
                    :capacity, :roomType
                )
                """, params()
                .addValue("id", id)
                .addValue("universityId", request.universityId())
                .addValue("building", blankToNull(request.building()))
                .addValue("name", request.name().trim())
                .addValue("capacity", request.capacity())
                .addValue("roomType", normalizeEnum(request.roomType())));
        return id;
    }

    public void replaceRoomFacilities(UUID roomId, Set<String> facilities) {
        jdbc.update("DELETE FROM aura_room_facilities WHERE room_id = :roomId",
                params().addValue("roomId", roomId));
        facilities.forEach(facility -> jdbc.update("""
                INSERT INTO aura_room_facilities (room_id, facility)
                VALUES (:roomId, :facility)
                """, params()
                .addValue("roomId", roomId)
                .addValue("facility", facility)));
    }

    public List<String> listRoomFacilities(UUID roomId) {
        return jdbc.queryForList("""
                SELECT facility
                FROM aura_room_facilities
                WHERE room_id = :roomId
                ORDER BY facility ASC
                """, params().addValue("roomId", roomId), String.class);
    }

    public List<RoomResponse> listRooms(UUID universityId) {
        return jdbc.query("""
                SELECT r.*,
                    COALESCE((
                        SELECT STRING_AGG(rf.facility, ',' ORDER BY rf.facility)
                        FROM aura_room_facilities rf
                        WHERE rf.room_id = r.id
                    ), '') AS facilities
                FROM aura_rooms r
                WHERE (:universityId IS NULL OR r.university_id = :universityId)
                ORDER BY r.name ASC
                """, params().addValue("universityId", universityId),
                roomMapper());
    }

    public Optional<RoomResponse> findRoom(UUID roomId) {
        return optional("""
                SELECT r.*,
                    COALESCE((
                        SELECT STRING_AGG(rf.facility, ',' ORDER BY rf.facility)
                        FROM aura_room_facilities rf
                        WHERE rf.room_id = r.id
                    ), '') AS facilities
                FROM aura_rooms r
                WHERE r.id = :roomId
                """, params().addValue("roomId", roomId), roomMapper());
    }

    public UUID insertTimeslot(
            UUID id,
            AuraDtos.CreateTimeslotRequest request) {
        jdbc.update("""
                INSERT INTO aura_timeslots (
                    id, university_id, day_of_week, starts_at, ends_at, label
                ) VALUES (
                    :id, :universityId, :dayOfWeek, :startsAt, :endsAt, :label
                )
                """, params()
                .addValue("id", id)
                .addValue("universityId", request.universityId())
                .addValue("dayOfWeek", request.dayOfWeek())
                .addValue("startsAt", request.startsAt())
                .addValue("endsAt", request.endsAt())
                .addValue("label", request.label().trim()));
        return id;
    }

    public List<TimeslotResponse> listTimeslots(UUID universityId) {
        return jdbc.query("""
                SELECT * FROM aura_timeslots
                WHERE (:universityId IS NULL OR university_id = :universityId)
                ORDER BY day_of_week ASC, starts_at ASC
                """, params().addValue("universityId", universityId),
                timeslotMapper());
    }

    public Optional<TimeslotResponse> findTimeslot(UUID timeslotId) {
        return optional("""
                SELECT *
                FROM aura_timeslots
                WHERE id = :timeslotId
                """, params().addValue("timeslotId", timeslotId),
                timeslotMapper());
    }

    public boolean roomMeetsRequirement(UUID roomId, UUID requirementId) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM aura_rooms room
                JOIN aura_meeting_requirements requirement
                    ON requirement.id = :requirementId
                WHERE room.id = :roomId
                  AND room.active = TRUE
                  AND room.capacity >= requirement.required_capacity
                  AND room.room_type = requirement.room_type
                  AND NOT EXISTS (
                    SELECT 1
                    FROM aura_meeting_requirement_facilities required
                    WHERE required.meeting_requirement_id = requirement.id
                      AND NOT EXISTS (
                        SELECT 1
                        FROM aura_room_facilities available
                        WHERE available.room_id = room.id
                          AND available.facility = required.facility
                      )
                  )
                """, params()
                .addValue("roomId", roomId)
                .addValue("requirementId", requirementId), Long.class);
        return count != null && count == 1;
    }

    public boolean assignmentMeetsHardRestrictions(
            UUID sessionId,
            UUID roomId,
            UUID timeslotId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM aura_scheduled_sessions session_row
                    JOIN aura_timetable_versions version_row
                      ON version_row.id = session_row.version_id
                    JOIN aura_meeting_requirements requirement
                      ON requirement.id = session_row.meeting_requirement_id
                    JOIN aura_course_offerings offering
                      ON offering.id = session_row.offering_id
                    JOIN aura_instructors instructor
                      ON instructor.id = session_row.instructor_id
                    JOIN aura_sections section_row
                      ON section_row.id = session_row.section_id
                    JOIN aura_rooms room ON room.id = :roomId
                    JOIN aura_timeslots timeslot ON timeslot.id = :timeslotId
                    JOIN aura_academic_terms term_row
                      ON term_row.id = version_row.term_id
                    WHERE session_row.id = :sessionId
                      AND version_row.status = 'DRAFT'
                      AND room.active = TRUE AND instructor.active = TRUE
                      AND section_row.active = TRUE AND timeslot.active = TRUE
                      AND room.university_id = term_row.university_id
                      AND timeslot.university_id = term_row.university_id
                      AND (timeslot.term_id IS NULL
                        OR timeslot.term_id = version_row.term_id)
                      AND room.capacity >= requirement.required_capacity
                      AND room.room_type = requirement.room_type
                      AND (requirement.fixed_room_id IS NULL
                        OR requirement.fixed_room_id = room.id)
                      AND (requirement.fixed_timeslot_id IS NULL
                        OR requirement.fixed_timeslot_id = timeslot.id)
                      AND timeslot.slot_type = 'INSTRUCTIONAL'
                      AND (requirement.duration_slots = 1 OR (
                        timeslot.slot_order IS NOT NULL AND (
                          SELECT COUNT(*)
                          FROM aura_timeslots block
                          WHERE block.university_id = timeslot.university_id
                            AND block.term_id IS NOT DISTINCT FROM timeslot.term_id
                            AND block.day_of_week = timeslot.day_of_week
                            AND block.slot_order BETWEEN timeslot.slot_order
                              AND timeslot.slot_order + requirement.duration_slots - 1
                            AND block.active = TRUE
                            AND block.slot_type = 'INSTRUCTIONAL'
                        ) = requirement.duration_slots))
                      AND NOT EXISTS (
                        SELECT 1
                        FROM aura_timeslots block
                        JOIN aura_timeslots previous
                          ON previous.university_id = block.university_id
                         AND previous.term_id IS NOT DISTINCT FROM block.term_id
                         AND previous.day_of_week = block.day_of_week
                         AND previous.slot_order = block.slot_order - 1
                        WHERE block.university_id = timeslot.university_id
                          AND block.term_id IS NOT DISTINCT FROM timeslot.term_id
                          AND block.day_of_week = timeslot.day_of_week
                          AND block.slot_order BETWEEN timeslot.slot_order + 1
                            AND timeslot.slot_order + requirement.duration_slots - 1
                          AND previous.ends_at <> block.starts_at)
                      AND NOT EXISTS (
                        SELECT 1 FROM aura_meeting_requirement_facilities required
                        WHERE required.meeting_requirement_id = requirement.id
                          AND NOT EXISTS (
                            SELECT 1 FROM aura_room_facilities available
                            WHERE available.room_id = room.id
                              AND available.facility = required.facility))
                      AND NOT EXISTS (
                        SELECT 1 FROM aura_instructor_availability availability
                        JOIN aura_timeslots blocked
                          ON blocked.id = availability.timeslot_id
                        WHERE availability.instructor_id = instructor.id
                          AND availability.availability = 'UNAVAILABLE'
                          AND blocked.day_of_week = timeslot.day_of_week
                          AND blocked.slot_order BETWEEN timeslot.slot_order
                            AND timeslot.slot_order + requirement.duration_slots - 1)
                      AND NOT EXISTS (
                        SELECT 1 FROM aura_room_availability availability
                        JOIN aura_timeslots blocked
                          ON blocked.id = availability.timeslot_id
                        WHERE availability.room_id = room.id
                          AND availability.availability = 'UNAVAILABLE'
                          AND blocked.day_of_week = timeslot.day_of_week
                          AND blocked.slot_order BETWEEN timeslot.slot_order
                            AND timeslot.slot_order + requirement.duration_slots - 1)
                      AND NOT EXISTS (
                        SELECT 1 FROM aura_section_availability availability
                        JOIN aura_timeslots blocked
                          ON blocked.id = availability.timeslot_id
                        WHERE availability.section_id = section_row.id
                          AND availability.availability = 'UNAVAILABLE'
                          AND blocked.day_of_week = timeslot.day_of_week
                          AND blocked.slot_order BETWEEN timeslot.slot_order
                            AND timeslot.slot_order + requirement.duration_slots - 1)
                      AND NOT EXISTS (
                        SELECT 1
                        FROM aura_calendar_exceptions exception_row
                        WHERE exception_row.term_id = version_row.term_id
                          AND exception_row.active = TRUE
                          AND (
                            exception_row.exception_type IN (
                              'HOLIDAY', 'NON_TEACHING_DAY', 'UNIVERSITY_EVENT')
                            OR exception_row.instructor_id = instructor.id
                            OR exception_row.room_id = room.id
                            OR exception_row.section_id = section_row.id
                            OR exception_row.timeslot_id = timeslot.id
                            OR EXISTS (
                              SELECT 1 FROM aura_timeslots blocked_slot
                              WHERE blocked_slot.id = exception_row.timeslot_id
                                AND blocked_slot.day_of_week = timeslot.day_of_week
                                AND blocked_slot.slot_order BETWEEN
                                  timeslot.slot_order
                                  AND timeslot.slot_order
                                    + requirement.duration_slots - 1)
                            OR (exception_row.exception_type = 'FACILITY_OUTAGE'
                              AND EXISTS (
                                SELECT 1
                                FROM aura_room_facilities room_facility
                                WHERE room_facility.room_id = room.id
                                  AND room_facility.facility =
                                    exception_row.facility))
                          )
                          AND EXISTS (
                            SELECT 1
                            FROM GENERATE_SERIES(
                              exception_row.starts_on,
                              exception_row.ends_on,
                              INTERVAL '1 day') AS affected_date
                            WHERE EXTRACT(ISODOW FROM affected_date) =
                              timeslot.day_of_week))
                )
                """, params().addValue("sessionId", sessionId)
                .addValue("roomId", roomId).addValue("timeslotId", timeslotId),
                Boolean.class));
    }

    public LocalTime assignmentEndTime(UUID timeslotId, int durationSlots) {
        LocalTime value = jdbc.queryForObject("""
                SELECT COALESCE((
                    SELECT block.ends_at
                    FROM aura_timeslots block
                    WHERE block.university_id = start_slot.university_id
                      AND block.term_id IS NOT DISTINCT FROM start_slot.term_id
                      AND block.day_of_week = start_slot.day_of_week
                      AND block.slot_order = start_slot.slot_order + :duration - 1
                      AND block.active = TRUE
                      AND block.slot_type = 'INSTRUCTIONAL'
                ), start_slot.ends_at)
                FROM aura_timeslots start_slot WHERE start_slot.id = :timeslotId
                """, params().addValue("timeslotId", timeslotId)
                .addValue("duration", Math.max(1, durationSlots)), LocalTime.class);
        return value;
    }

    public UUID upsertInstructorAvailability(
            UUID id,
            AuraDtos.CreateInstructorAvailabilityRequest request,
            String availability) {
        Optional<UUID> existing = optionalUuid("""
                SELECT id
                FROM aura_instructor_availability
                WHERE instructor_id = :instructorId
                  AND timeslot_id = :timeslotId
                """, params()
                .addValue("instructorId", request.instructorId())
                .addValue("timeslotId", request.timeslotId()));
        if (existing.isPresent()) {
            jdbc.update("""
                    UPDATE aura_instructor_availability
                    SET availability = :availability, reason = :reason
                    WHERE id = :id
                    """, params()
                    .addValue("id", existing.get())
                    .addValue("availability", availability)
                    .addValue("reason", blankToNull(request.reason())));
            return existing.get();
        }
        jdbc.update("""
                INSERT INTO aura_instructor_availability (
                    id, instructor_id, timeslot_id, availability, reason
                ) VALUES (
                    :id, :instructorId, :timeslotId, :availability, :reason
                )
                """, params()
                .addValue("id", id)
                .addValue("instructorId", request.instructorId())
                .addValue("timeslotId", request.timeslotId())
                .addValue("availability", availability)
                .addValue("reason", blankToNull(request.reason())));
        return id;
    }

    public List<AvailabilityResponse> listInstructorAvailability(
            UUID instructorId) {
        return jdbc.query("""
                SELECT a.id, a.instructor_id AS target_id, a.timeslot_id,
                    ts.day_of_week, ts.starts_at, ts.ends_at, ts.label,
                    a.availability, a.reason
                FROM aura_instructor_availability a
                JOIN aura_timeslots ts ON ts.id = a.timeslot_id
                WHERE a.instructor_id = :instructorId
                ORDER BY ts.day_of_week ASC, ts.starts_at ASC
                """, params().addValue("instructorId", instructorId),
                availabilityMapper());
    }

    public UUID upsertRoomAvailability(
            UUID id,
            AuraDtos.CreateRoomAvailabilityRequest request,
            String availability) {
        Optional<UUID> existing = optionalUuid("""
                SELECT id
                FROM aura_room_availability
                WHERE room_id = :roomId
                  AND timeslot_id = :timeslotId
                """, params()
                .addValue("roomId", request.roomId())
                .addValue("timeslotId", request.timeslotId()));
        if (existing.isPresent()) {
            jdbc.update("""
                    UPDATE aura_room_availability
                    SET availability = :availability, reason = :reason
                    WHERE id = :id
                    """, params()
                    .addValue("id", existing.get())
                    .addValue("availability", availability)
                    .addValue("reason", blankToNull(request.reason())));
            return existing.get();
        }
        jdbc.update("""
                INSERT INTO aura_room_availability (
                    id, room_id, timeslot_id, availability, reason
                ) VALUES (
                    :id, :roomId, :timeslotId, :availability, :reason
                )
                """, params()
                .addValue("id", id)
                .addValue("roomId", request.roomId())
                .addValue("timeslotId", request.timeslotId())
                .addValue("availability", availability)
                .addValue("reason", blankToNull(request.reason())));
        return id;
    }

    public List<AvailabilityResponse> listRoomAvailability(UUID roomId) {
        return jdbc.query("""
                SELECT a.id, a.room_id AS target_id, a.timeslot_id,
                    ts.day_of_week, ts.starts_at, ts.ends_at, ts.label,
                    a.availability, a.reason
                FROM aura_room_availability a
                JOIN aura_timeslots ts ON ts.id = a.timeslot_id
                WHERE a.room_id = :roomId
                ORDER BY ts.day_of_week ASC, ts.starts_at ASC
                """, params().addValue("roomId", roomId),
                availabilityMapper());
    }

    public boolean sectionAndTimeslotShareUniversity(
            UUID sectionId,
            UUID timeslotId) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM aura_sections s
                JOIN aura_batches b ON b.id = s.batch_id
                JOIN aura_programs p ON p.id = b.program_id
                JOIN aura_timeslots ts ON ts.university_id = p.university_id
                WHERE s.id = :sectionId
                  AND ts.id = :timeslotId
                """, params()
                .addValue("sectionId", sectionId)
                .addValue("timeslotId", timeslotId), Long.class);
        return count != null && count > 0;
    }

    public UUID upsertSectionAvailability(
            UUID id,
            AuraDtos.CreateSectionAvailabilityRequest request,
            String availability) {
        Optional<UUID> existing = optionalUuid("""
                SELECT id
                FROM aura_section_availability
                WHERE section_id = :sectionId
                  AND timeslot_id = :timeslotId
                """, params()
                .addValue("sectionId", request.sectionId())
                .addValue("timeslotId", request.timeslotId()));
        if (existing.isPresent()) {
            jdbc.update("""
                    UPDATE aura_section_availability
                    SET availability = :availability,
                        reason = :reason,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = :id
                    """, params()
                    .addValue("id", existing.get())
                    .addValue("availability", availability)
                    .addValue("reason", blankToNull(request.reason())));
            return existing.get();
        }
        jdbc.update("""
                INSERT INTO aura_section_availability (
                    id, section_id, timeslot_id, availability, reason
                ) VALUES (
                    :id, :sectionId, :timeslotId, :availability, :reason
                )
                """, params()
                .addValue("id", id)
                .addValue("sectionId", request.sectionId())
                .addValue("timeslotId", request.timeslotId())
                .addValue("availability", availability)
                .addValue("reason", blankToNull(request.reason())));
        return id;
    }

    public List<AvailabilityResponse> listSectionAvailability(UUID sectionId) {
        return jdbc.query("""
                SELECT a.id, a.section_id AS target_id, a.timeslot_id,
                    ts.day_of_week, ts.starts_at, ts.ends_at, ts.label,
                    a.availability, a.reason
                FROM aura_section_availability a
                JOIN aura_timeslots ts ON ts.id = a.timeslot_id
                WHERE a.section_id = :sectionId
                ORDER BY ts.day_of_week ASC, ts.starts_at ASC
                """, params().addValue("sectionId", sectionId),
                availabilityMapper());
    }

    public UUID insertCalendarException(
            UUID id,
            UUID userId,
            AuraDtos.CreateCalendarExceptionRequest request,
            String exceptionType,
            String facility) {
        jdbc.update("""
                INSERT INTO aura_calendar_exceptions (
                    id, term_id, exception_type, starts_on, ends_on,
                    instructor_id, room_id, section_id, timeslot_id,
                    facility, reason, created_by_user_id
                ) VALUES (
                    :id, :termId, :exceptionType, :startsOn, :endsOn,
                    :instructorId, :roomId, :sectionId, :timeslotId,
                    :facility, :reason, :userId
                )
                """, params()
                .addValue("id", id)
                .addValue("termId", request.termId())
                .addValue("exceptionType", exceptionType)
                .addValue("startsOn", request.startsOn())
                .addValue("endsOn", request.endsOn())
                .addValue("instructorId", request.instructorId())
                .addValue("roomId", request.roomId())
                .addValue("sectionId", request.sectionId())
                .addValue("timeslotId", request.timeslotId())
                .addValue("facility", facility)
                .addValue("reason", request.reason().trim())
                .addValue("userId", userId));
        return id;
    }

    public Optional<CalendarExceptionResponse> findCalendarException(UUID id) {
        return optional("""
                SELECT *
                FROM aura_calendar_exceptions
                WHERE id = :id
                """, params().addValue("id", id), calendarExceptionMapper());
    }

    public List<CalendarExceptionResponse> listCalendarExceptions(UUID termId) {
        return jdbc.query("""
                SELECT *
                FROM aura_calendar_exceptions
                WHERE term_id = :termId
                ORDER BY active DESC, starts_on ASC, created_at ASC
                """, params().addValue("termId", termId),
                calendarExceptionMapper());
    }

    public boolean updateCalendarException(
            UUID id,
            AuraDtos.UpdateCalendarExceptionRequest request,
            String exceptionType,
            String facility) {
        int updated = jdbc.update("""
                UPDATE aura_calendar_exceptions
                SET exception_type = :exceptionType,
                    starts_on = :startsOn,
                    ends_on = :endsOn,
                    instructor_id = :instructorId,
                    room_id = :roomId,
                    section_id = :sectionId,
                    timeslot_id = :timeslotId,
                    facility = :facility,
                    reason = :reason,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :id
                  AND version = :version
                  AND active = TRUE
                """, params()
                .addValue("id", id)
                .addValue("exceptionType", exceptionType)
                .addValue("startsOn", request.startsOn())
                .addValue("endsOn", request.endsOn())
                .addValue("instructorId", request.instructorId())
                .addValue("roomId", request.roomId())
                .addValue("sectionId", request.sectionId())
                .addValue("timeslotId", request.timeslotId())
                .addValue("facility", facility)
                .addValue("reason", request.reason().trim())
                .addValue("version", request.version()));
        return updated == 1;
    }

    public boolean deactivateCalendarException(UUID id, long version) {
        return jdbc.update("""
                UPDATE aura_calendar_exceptions
                SET active = FALSE,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :id
                  AND version = :version
                  AND active = TRUE
                """, params()
                .addValue("id", id)
                .addValue("version", version)) == 1;
    }

    public UUID insertOffering(
            UUID id,
            AuraDtos.CreateOfferingRequest request) {
        jdbc.update("""
                INSERT INTO aura_course_offerings (
                    id, term_id, course_id, section_id, instructor_id,
                    expected_students
                ) VALUES (
                    :id, :termId, :courseId, :sectionId, :instructorId,
                    :expectedStudents
                )
                """, params()
                .addValue("id", id)
                .addValue("termId", request.termId())
                .addValue("courseId", request.courseId())
                .addValue("sectionId", request.sectionId())
                .addValue("instructorId", request.instructorId())
                .addValue("expectedStudents", request.expectedStudents()));
        return id;
    }

    public List<OfferingResponse> listOfferings(UUID termId) {
        return jdbc.query("""
                SELECT o.*, c.course_code, c.title AS course_title,
                    s.display_name AS section_name,
                    i.display_name AS instructor_name
                FROM aura_course_offerings o
                JOIN courses c ON c.id = o.course_id
                JOIN aura_sections s ON s.id = o.section_id
                JOIN aura_instructors i ON i.id = o.instructor_id
                WHERE o.term_id = :termId
                ORDER BY c.course_code ASC, s.display_name ASC
                """, params().addValue("termId", termId), offeringMapper());
    }

    public UUID insertMeetingRequirement(
            UUID id,
            AuraDtos.CreateMeetingRequirementRequest request) {
        jdbc.update("""
                INSERT INTO aura_meeting_requirements (
                    id, offering_id, meeting_type, sessions_per_week,
                    duration_slots, room_type, required_capacity, notes
                ) VALUES (
                    :id, :offeringId, :meetingType, :sessionsPerWeek,
                    :durationSlots, :roomType, :requiredCapacity, :notes
                )
                """, params()
                .addValue("id", id)
                .addValue("offeringId", request.offeringId())
                .addValue("meetingType", normalizeEnum(request.meetingType()))
                .addValue("sessionsPerWeek", request.sessionsPerWeek())
                .addValue("durationSlots", request.durationSlots())
                .addValue("roomType", normalizeEnum(request.roomType()))
                .addValue("requiredCapacity", request.requiredCapacity())
                .addValue("notes", blankToNull(request.notes())));
        return id;
    }

    public void replaceRequirementFacilities(
            UUID requirementId,
            Set<String> facilities) {
        jdbc.update("""
                DELETE FROM aura_meeting_requirement_facilities
                WHERE meeting_requirement_id = :requirementId
                """, params().addValue("requirementId", requirementId));
        facilities.forEach(facility -> jdbc.update("""
                INSERT INTO aura_meeting_requirement_facilities (
                    meeting_requirement_id, facility
                ) VALUES (:requirementId, :facility)
                """, params()
                .addValue("requirementId", requirementId)
                .addValue("facility", facility)));
    }

    public List<String> listRequirementFacilities(UUID requirementId) {
        return jdbc.queryForList("""
                SELECT facility
                FROM aura_meeting_requirement_facilities
                WHERE meeting_requirement_id = :requirementId
                ORDER BY facility ASC
                """, params().addValue("requirementId", requirementId),
                String.class);
    }

    public List<MeetingRequirementResponse> listMeetingRequirements(
            UUID offeringId) {
        return jdbc.query("""
                SELECT r.*,
                    COALESCE((
                        SELECT STRING_AGG(rf.facility, ',' ORDER BY rf.facility)
                        FROM aura_meeting_requirement_facilities rf
                        WHERE rf.meeting_requirement_id = r.id
                    ), '') AS required_facilities
                FROM aura_meeting_requirements r
                WHERE r.offering_id = :offeringId
                ORDER BY r.meeting_type ASC
                """, params().addValue("offeringId", offeringId),
                requirementMapper());
    }

    public TermCounts countsForTerm(UUID termId) {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM aura_rooms r
                        JOIN aura_academic_terms t ON t.university_id = r.university_id
                        WHERE t.id = :termId AND r.active = TRUE) AS rooms,
                    (SELECT COUNT(*) FROM aura_timeslots ts
                        JOIN aura_academic_terms t ON t.university_id = ts.university_id
                        WHERE t.id = :termId AND ts.active = TRUE) AS timeslots,
                    (SELECT COUNT(*) FROM aura_instructors i
                        JOIN aura_academic_terms t ON t.university_id = i.university_id
                        WHERE t.id = :termId AND i.active = TRUE) AS instructors,
                    (SELECT COUNT(*) FROM aura_course_offerings o
                        WHERE o.term_id = :termId AND o.status = 'ACTIVE') AS offerings,
                    (SELECT COUNT(*) FROM aura_meeting_requirements r
                        JOIN aura_course_offerings o ON o.id = r.offering_id
                        WHERE o.term_id = :termId AND o.status = 'ACTIVE') AS requirements
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new TermCounts(
                        rs.getInt("rooms"),
                        rs.getInt("timeslots"),
                        rs.getInt("instructors"),
                        rs.getInt("offerings"),
                        rs.getInt("requirements")));
    }

    public List<RequirementCandidateIssue> requirementsWithoutCandidates(
            UUID termId) {
        return jdbc.query("""
                SELECT r.id AS requirement_id,
                    c.course_code,
                    c.title AS course_title
                FROM aura_meeting_requirements r
                JOIN aura_course_offerings o ON o.id = r.offering_id
                JOIN courses c ON c.id = o.course_id
                WHERE o.term_id = :termId
                  AND o.status = 'ACTIVE'
                  AND NOT EXISTS (
                    SELECT 1
                    FROM aura_rooms room
                    JOIN aura_academic_terms term
                        ON term.university_id = room.university_id
                    JOIN aura_timeslots slot
                        ON slot.university_id = term.university_id
                    WHERE term.id = :termId
                      AND room.active = TRUE
                      AND slot.active = TRUE
                      AND room.capacity >= r.required_capacity
                      AND room.room_type = r.room_type
                      AND NOT EXISTS (
                        SELECT 1
                        FROM aura_meeting_requirement_facilities required
                        WHERE required.meeting_requirement_id = r.id
                          AND NOT EXISTS (
                            SELECT 1
                            FROM aura_room_facilities available
                            WHERE available.room_id = room.id
                              AND available.facility = required.facility
                          )
                      )
                      AND NOT EXISTS (
                        SELECT 1
                        FROM aura_instructor_availability ia
                        WHERE ia.instructor_id = o.instructor_id
                          AND ia.timeslot_id = slot.id
                          AND ia.availability = 'UNAVAILABLE'
                      )
                      AND NOT EXISTS (
                        SELECT 1
                        FROM aura_room_availability ra
                        WHERE ra.room_id = room.id
                          AND ra.timeslot_id = slot.id
                          AND ra.availability = 'UNAVAILABLE'
                      )
                      AND NOT EXISTS (
                        SELECT 1
                        FROM aura_section_availability sa
                        WHERE sa.section_id = o.section_id
                          AND sa.timeslot_id = slot.id
                          AND sa.availability = 'UNAVAILABLE'
                      )
                  )
                ORDER BY c.course_code ASC, r.id ASC
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new RequirementCandidateIssue(
                        uuid(rs, "requirement_id"),
                        rs.getString("course_code"),
                        rs.getString("course_title")));
    }

    public List<SolverRoom> solverRooms(UUID termId) {
        return jdbc.query("""
                SELECT r.id, r.capacity, r.room_type, r.building,
                    COALESCE((
                        SELECT STRING_AGG(rf.facility, ',' ORDER BY rf.facility)
                        FROM aura_room_facilities rf
                        WHERE rf.room_id = r.id
                    ), '') AS facilities
                FROM aura_rooms r
                JOIN aura_academic_terms t ON t.university_id = r.university_id
                WHERE t.id = :termId AND r.active = TRUE
                ORDER BY r.capacity ASC, r.name ASC
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new SolverRoom(
                        uuid(rs, "id"),
                        rs.getInt("capacity"),
                        rs.getString("room_type"),
                        splitCsv(rs.getString("facilities")),
                        rs.getString("building")));
    }

    public List<SolverTimeslot> solverTimeslots(UUID termId) {
        return jdbc.query("""
                SELECT ts.id, ts.day_of_week, ts.starts_at, ts.ends_at,
                    COALESCE(ts.slot_order, 100) AS slot_order, ts.slot_type
                FROM aura_timeslots ts
                JOIN aura_academic_terms t ON t.university_id = ts.university_id
                WHERE t.id = :termId AND ts.active = TRUE
                ORDER BY ts.day_of_week ASC, ts.starts_at ASC
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new SolverTimeslot(
                        uuid(rs, "id"),
                        rs.getInt("day_of_week"),
                        rs.getObject("starts_at", LocalTime.class),
                        rs.getObject("ends_at", LocalTime.class),
                        rs.getInt("slot_order"),
                        rs.getString("slot_type")));
    }

    public List<SolverInstructorAvailability> solverInstructorAvailability(
            UUID termId) {
        return jdbc.query("""
                SELECT ia.instructor_id, ia.timeslot_id, ia.availability
                FROM aura_instructor_availability ia
                JOIN aura_instructors i ON i.id = ia.instructor_id
                JOIN aura_timeslots ts ON ts.id = ia.timeslot_id
                JOIN aura_academic_terms t
                    ON t.university_id = i.university_id
                    AND t.university_id = ts.university_id
                WHERE t.id = :termId
                  AND ia.availability IN ('UNAVAILABLE', 'AVOID', 'PREFERRED')
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new SolverInstructorAvailability(
                        uuid(rs, "instructor_id"),
                        uuid(rs, "timeslot_id"),
                        rs.getString("availability")));
    }

    public List<SolverRoomAvailability> solverRoomAvailability(UUID termId) {
        return jdbc.query("""
                SELECT ra.room_id, ra.timeslot_id, ra.availability
                FROM aura_room_availability ra
                JOIN aura_rooms r ON r.id = ra.room_id
                JOIN aura_timeslots ts ON ts.id = ra.timeslot_id
                JOIN aura_academic_terms t
                    ON t.university_id = r.university_id
                    AND t.university_id = ts.university_id
                WHERE t.id = :termId
                  AND ra.availability IN ('UNAVAILABLE', 'AVOID', 'PREFERRED')
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new SolverRoomAvailability(
                        uuid(rs, "room_id"),
                        uuid(rs, "timeslot_id"),
                        rs.getString("availability")));
    }

    public List<SolverSectionAvailability> solverSectionAvailability(
            UUID termId) {
        return jdbc.query("""
                SELECT sa.section_id, sa.timeslot_id, sa.availability
                FROM aura_section_availability sa
                JOIN aura_sections s ON s.id = sa.section_id
                JOIN aura_batches b ON b.id = s.batch_id
                JOIN aura_programs p ON p.id = b.program_id
                JOIN aura_timeslots ts ON ts.id = sa.timeslot_id
                JOIN aura_academic_terms t
                    ON t.university_id = p.university_id
                    AND t.university_id = ts.university_id
                WHERE t.id = :termId
                  AND sa.availability IN ('UNAVAILABLE', 'AVOID', 'PREFERRED')
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new SolverSectionAvailability(
                        uuid(rs, "section_id"),
                        uuid(rs, "timeslot_id"),
                        rs.getString("availability")));
    }

    public List<SolverStudentAvailability> solverStudentAvailability(
            UUID termId) {
        return jdbc.query("""
                SELECT registration.offering_id,
                    availability.student_user_id,
                    availability.timeslot_id,
                    availability.availability
                FROM aura_student_course_registrations registration
                JOIN aura_student_availability availability
                  ON availability.term_id = registration.term_id
                 AND availability.student_user_id = registration.student_user_id
                JOIN aura_course_offerings offering
                  ON offering.id = registration.offering_id
                WHERE registration.term_id = :termId
                  AND registration.status = 'ACTIVE'
                  AND offering.status = 'ACTIVE'
                  AND availability.availability IN (
                    'UNAVAILABLE', 'AVOID', 'PREFERRED')
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new SolverStudentAvailability(
                        uuid(rs, "offering_id"),
                        uuid(rs, "student_user_id"),
                        uuid(rs, "timeslot_id"),
                        rs.getString("availability")));
    }

    public List<SolverTravelRuleInput> solverTravelRules(UUID termId) {
        return jdbc.query("""
                SELECT travel.from_building, travel.to_building,
                    travel.minutes, travel.difficulty
                FROM aura_building_travel_times travel
                JOIN aura_academic_terms term
                  ON term.university_id = travel.university_id
                WHERE term.id = :termId
                  AND travel.active = TRUE
                ORDER BY LOWER(travel.from_building),
                    LOWER(travel.to_building)
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new SolverTravelRuleInput(
                        rs.getString("from_building"),
                        rs.getString("to_building"),
                        rs.getInt("minutes"),
                        rs.getString("difficulty")));
    }

    public List<SolverRequirement> solverRequirements(UUID termId) {
        return jdbc.query("""
                SELECT r.id AS requirement_id, o.id AS offering_id,
                    o.section_id, o.instructor_id, r.sessions_per_week,
                    r.room_type, r.required_capacity,
                    r.duration_slots, r.allowed_days, r.prohibited_days,
                    r.preferred_days, r.preferred_start_time,
                    r.preferred_end_time, r.minimum_day_separation,
                    r.maximum_occurrences_per_day, r.same_room_preferred,
                    r.fixed_room_id, r.fixed_timeslot_id, r.pinned,
                    r.meeting_type, r.teaching_group,
                    r.linked_requirement_id, r.lecture_before_linked,
                    r.week_pattern, r.custom_weeks,
                    i.max_hours_per_week, i.hard_daily_load,
                    i.preferred_weekly_load, i.preferred_daily_load,
                    i.maximum_consecutive_slots,
                    s.hard_daily_load AS section_hard_daily_load,
                    s.preferred_daily_load AS section_preferred_daily_load,
                    COALESCE((
                        SELECT STRING_AGG(rf.facility, ',' ORDER BY rf.facility)
                        FROM aura_meeting_requirement_facilities rf
                        WHERE rf.meeting_requirement_id = r.id
                    ), '') AS required_facilities,
                    COALESCE((
                        SELECT STRING_AGG(
                            section_link.section_id::text,
                            ',' ORDER BY section_link.section_id::text)
                        FROM aura_offering_sections section_link
                        WHERE section_link.offering_id = o.id
                    ), o.section_id::text) AS participating_section_ids,
                    COALESCE((
                        SELECT STRING_AGG(
                            registration.student_user_id::text,
                            ',' ORDER BY registration.student_user_id::text)
                        FROM aura_student_course_registrations registration
                        WHERE registration.offering_id = o.id
                          AND registration.term_id = :termId
                          AND registration.status = 'ACTIVE'
                    ), '') AS student_user_ids,
                    COALESCE((
                        SELECT STRING_AGG(
                            CASE
                              WHEN conflict.left_offering_id = o.id
                                THEN conflict.right_offering_id::text
                              ELSE conflict.left_offering_id::text
                            END,
                            ',' ORDER BY conflict.id::text)
                        FROM aura_cross_offering_conflicts conflict
                        WHERE conflict.term_id = :termId
                          AND conflict.active = TRUE
                          AND conflict.severity = 'HARD'
                          AND (conflict.left_offering_id = o.id
                            OR conflict.right_offering_id = o.id)
                    ), '') AS hard_conflict_offering_ids
                FROM aura_meeting_requirements r
                JOIN aura_course_offerings o ON o.id = r.offering_id
                JOIN aura_instructors i ON i.id = o.instructor_id
                JOIN aura_sections s ON s.id = o.section_id
                WHERE o.term_id = :termId AND o.status = 'ACTIVE'
                  AND r.active = TRUE AND i.active = TRUE AND s.active = TRUE
                ORDER BY o.id ASC, r.id ASC
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new SolverRequirement(
                        uuid(rs, "requirement_id"),
                        uuid(rs, "offering_id"),
                        uuid(rs, "section_id"),
                        uuid(rs, "instructor_id"),
                        rs.getInt("sessions_per_week"),
                        rs.getString("room_type"),
                        rs.getInt("required_capacity"),
                        splitCsv(rs.getString("required_facilities")),
                        rs.getInt("duration_slots"),
                        intArray(rs, "allowed_days"),
                        intArray(rs, "prohibited_days"),
                        intArray(rs, "preferred_days"),
                        rs.getObject("preferred_start_time", LocalTime.class),
                        rs.getObject("preferred_end_time", LocalTime.class),
                        rs.getInt("minimum_day_separation"),
                        rs.getInt("maximum_occurrences_per_day"),
                        rs.getBoolean("same_room_preferred"),
                        nullableUuid(rs, "fixed_room_id"),
                        nullableUuid(rs, "fixed_timeslot_id"),
                        rs.getBoolean("pinned"),
                        rs.getString("meeting_type"),
                        rs.getString("teaching_group"),
                        nullableUuid(rs, "linked_requirement_id"),
                        rs.getBoolean("lecture_before_linked"),
                        rs.getString("week_pattern"),
                        intArray(rs, "custom_weeks"),
                        splitUuidCsv(rs.getString("participating_section_ids")),
                        splitUuidCsv(rs.getString("student_user_ids")),
                        splitUuidCsv(rs.getString("hard_conflict_offering_ids")),
                        rs.getInt("max_hours_per_week"),
                        rs.getInt("hard_daily_load"),
                        rs.getInt("preferred_weekly_load"),
                        rs.getInt("preferred_daily_load"),
                        rs.getInt("maximum_consecutive_slots"),
                        rs.getInt("section_hard_daily_load"),
                        rs.getInt("section_preferred_daily_load")));
    }

    public UUID insertRevision(
            UUID id,
            UUID termId,
            int revisionNumber,
            String checksum,
            String summary,
            UUID userId) {
        jdbc.update("""
                INSERT INTO aura_scheduling_data_revisions (
                    id, term_id, revision_number, checksum_sha256, summary,
                    created_by_user_id
                ) VALUES (
                    :id, :termId, :revisionNumber, :checksum, :summary,
                    :userId
                )
                """, params()
                .addValue("id", id)
                .addValue("termId", termId)
                .addValue("revisionNumber", revisionNumber)
                .addValue("checksum", checksum)
                .addValue("summary", summary)
                .addValue("userId", userId));
        return id;
    }

    public int nextRevisionNumber(UUID termId) {
        Integer value = jdbc.queryForObject("""
                SELECT COALESCE(MAX(revision_number), 0) + 1
                FROM aura_scheduling_data_revisions
                WHERE term_id = :termId
                """, params().addValue("termId", termId), Integer.class);
        return value == null ? 1 : value;
    }

    public UUID insertRun(
            UUID id,
            UUID termId,
            UUID revisionId,
            UUID userId,
            int terminationSeconds) {
        jdbc.update("""
                INSERT INTO aura_generation_runs (
                    id, term_id, revision_id, requested_by_user_id,
                    termination_seconds, input_revision
                ) VALUES (
                    :id, :termId, :revisionId, :userId, :terminationSeconds,
                    (SELECT data_revision FROM aura_academic_terms
                     WHERE id = :termId)
                )
                """, params()
                .addValue("id", id)
                .addValue("termId", termId)
                .addValue("revisionId", revisionId)
                .addValue("userId", userId)
                .addValue("terminationSeconds", terminationSeconds));
        return id;
    }

    public boolean hasActiveGenerationRun(UUID termId) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM aura_generation_runs
                WHERE term_id = :termId
                  AND status IN ('QUEUED', 'RUNNING')
                """, params().addValue("termId", termId), Long.class);
        return count != null && count > 0;
    }

    public void markRunRunning(UUID runId, Instant startedAt) {
        jdbc.update("""
                UPDATE aura_generation_runs
                SET status = 'RUNNING', started_at = :startedAt
                WHERE id = :runId AND status = 'QUEUED'
                """, params()
                .addValue("runId", runId)
                .addValue("startedAt", Timestamp.from(startedAt)));
    }

    public void markRunCompleted(UUID runId, String score, String message) {
        jdbc.update("""
                UPDATE aura_generation_runs
                SET status = 'COMPLETED', score = :score, message = :message,
                    completed_at = CURRENT_TIMESTAMP
                WHERE id = :runId
                """, params()
                .addValue("runId", runId)
                .addValue("score", score)
                .addValue("message", message));
    }

    public void markRunFailed(UUID runId, String message) {
        jdbc.update("""
                UPDATE aura_generation_runs
                SET status = 'FAILED', message = :message,
                    completed_at = CURRENT_TIMESTAMP
                WHERE id = :runId
                """, params()
                .addValue("runId", runId)
                .addValue("message", message));
    }

    public void cancelRun(UUID runId) {
        jdbc.update("""
                UPDATE aura_generation_runs
                SET status = 'CANCELLED', cancelled_at = CURRENT_TIMESTAMP,
                    message = 'Generation was cancelled.'
                WHERE id = :runId AND status IN ('QUEUED', 'RUNNING')
                """, params().addValue("runId", runId));
    }

    public Optional<GenerationRunResponse> findRun(UUID runId) {
        return optional("""
                SELECT * FROM aura_generation_runs WHERE id = :runId
                """, params().addValue("runId", runId), runMapper());
    }

    public UUID insertVersion(
            UUID id,
            UUID termId,
            UUID runId,
            int versionNumber,
            String score,
            String notes,
            UUID userId) {
        jdbc.update("""
                INSERT INTO aura_timetable_versions (
                    id, term_id, generation_run_id, version_number, score,
                    notes, created_by_user_id, revision_id, input_revision,
                    source
                )
                SELECT :id, :termId, :runId, :versionNumber, :score, :notes,
                    :userId, generation_run.revision_id,
                    generation_run.input_revision, 'GENERATED'
                FROM aura_generation_runs generation_run
                WHERE generation_run.id = :runId
                """, params()
                .addValue("id", id)
                .addValue("termId", termId)
                .addValue("runId", runId)
                .addValue("versionNumber", versionNumber)
                .addValue("score", score)
                .addValue("notes", notes)
                .addValue("userId", userId));
        return id;
    }

    public int nextVersionNumber(UUID termId) {
        Integer value = jdbc.queryForObject("""
                SELECT COALESCE(MAX(version_number), 0) + 1
                FROM aura_timetable_versions
                WHERE term_id = :termId
                """, params().addValue("termId", termId), Integer.class);
        return value == null ? 1 : value;
    }

    public void insertSession(
            UUID id,
            UUID versionId,
            SolverAssignment assignment,
            String source) {
        jdbc.update("""
                INSERT INTO aura_scheduled_sessions (
                    id, version_id, meeting_requirement_id, offering_id,
                    section_id, instructor_id, room_id, timeslot_id, source,
                    occurrence_index
                ) VALUES (
                    :id, :versionId, :requirementId, :offeringId,
                    :sectionId, :instructorId, :roomId, :timeslotId, :source,
                    (SELECT COALESCE(MAX(existing.occurrence_index), 0) + 1
                     FROM aura_scheduled_sessions existing
                     WHERE existing.version_id = :versionId
                       AND existing.meeting_requirement_id = :requirementId)
                )
                """, params()
                .addValue("id", id)
                .addValue("versionId", versionId)
                .addValue("requirementId", assignment.requirementId())
                .addValue("offeringId", assignment.offeringId())
                .addValue("sectionId", assignment.sectionId())
                .addValue("instructorId", assignment.instructorId())
                .addValue("roomId", assignment.roomId())
                .addValue("timeslotId", assignment.timeslotId())
                .addValue("source", source));
    }

    public List<TimetableVersionResponse> listVersions(UUID termId) {
        return jdbc.query("""
                SELECT * FROM aura_timetable_versions
                WHERE term_id = :termId
                ORDER BY version_number DESC
                """, params().addValue("termId", termId), versionMapper());
    }

    public Optional<TimetableVersionResponse> findVersion(UUID versionId) {
        return optional("""
                SELECT * FROM aura_timetable_versions WHERE id = :versionId
                """, params().addValue("versionId", versionId), versionMapper());
    }

    public UUID cloneVersion(
            UUID id,
            UUID sourceVersionId,
            int versionNumber,
            String notes,
            UUID userId,
            String versionSource) {
        jdbc.update("""
                INSERT INTO aura_timetable_versions (
                    id, term_id, version_number, status, score, notes,
                    created_by_user_id, revision_id, parent_version_id,
                    source, metrics, input_revision
                )
                SELECT :id, source_version.term_id, :versionNumber, 'DRAFT',
                    source_version.score, :notes, :userId,
                    source_version.revision_id, source_version.id, :source,
                    source_version.metrics, term_row.data_revision
                FROM aura_timetable_versions source_version
                JOIN aura_academic_terms term_row
                  ON term_row.id = source_version.term_id
                WHERE source_version.id = :sourceVersionId
                """, params()
                .addValue("id", id)
                .addValue("sourceVersionId", sourceVersionId)
                .addValue("versionNumber", versionNumber)
                .addValue("notes", blankToNull(notes))
                .addValue("userId", userId)
                .addValue("source", versionSource));
        List<CloneSessionRow> sourceSessions = jdbc.query("""
                SELECT meeting_requirement_id, offering_id, section_id,
                    instructor_id, room_id, timeslot_id, locked, source,
                    occurrence_index, pinned, lock_reason
                FROM aura_scheduled_sessions
                WHERE version_id = :sourceVersionId
                ORDER BY meeting_requirement_id, occurrence_index
                """, params().addValue("sourceVersionId", sourceVersionId),
                (rs, rowNum) -> new CloneSessionRow(
                        uuid(rs, "meeting_requirement_id"),
                        uuid(rs, "offering_id"),
                        uuid(rs, "section_id"),
                        uuid(rs, "instructor_id"),
                        uuid(rs, "room_id"),
                        uuid(rs, "timeslot_id"),
                        rs.getBoolean("locked"),
                        rs.getString("source"),
                        rs.getInt("occurrence_index"),
                        rs.getBoolean("pinned"),
                        rs.getString("lock_reason")));
        sourceSessions.forEach(source -> jdbc.update("""
                INSERT INTO aura_scheduled_sessions (
                    id, version_id, meeting_requirement_id, offering_id,
                    section_id, instructor_id, room_id, timeslot_id, locked,
                    source, occurrence_index, pinned, lock_reason
                ) VALUES (
                    :sessionId, :versionId, :requirementId, :offeringId,
                    :sectionId, :instructorId, :roomId, :timeslotId, :locked,
                    :source, :occurrenceIndex, :pinned, :lockReason
                )
                """, params()
                .addValue("sessionId", UUID.randomUUID())
                .addValue("versionId", id)
                .addValue("requirementId", source.meetingRequirementId())
                .addValue("offeringId", source.offeringId())
                .addValue("sectionId", source.sectionId())
                .addValue("instructorId", source.instructorId())
                .addValue("roomId", source.roomId())
                .addValue("timeslotId", source.timeslotId())
                .addValue("locked", source.locked())
                .addValue("source", source.source())
                .addValue("occurrenceIndex", source.occurrenceIndex())
                .addValue("pinned", source.pinned())
                .addValue("lockReason", source.lockReason())));
        return id;
    }

    public List<VersionSessionSnapshot> versionSessionSnapshots(UUID versionId) {
        return jdbc.query("""
                SELECT id, meeting_requirement_id, occurrence_index,
                    room_id, timeslot_id, instructor_id, section_id
                FROM aura_scheduled_sessions
                WHERE version_id = :versionId
                ORDER BY meeting_requirement_id, occurrence_index
                """, params().addValue("versionId", versionId),
                (rs, rowNum) -> new VersionSessionSnapshot(
                        uuid(rs, "id"),
                        uuid(rs, "meeting_requirement_id"),
                        rs.getInt("occurrence_index"),
                        uuid(rs, "room_id"),
                        uuid(rs, "timeslot_id"),
                        uuid(rs, "instructor_id"),
                        uuid(rs, "section_id")));
    }

    public void swapSessionAssignments(
            UUID firstSessionId,
            UUID secondSessionId,
            UUID userId,
            String reason) {
        SessionResponse first = findSession(firstSessionId).orElseThrow();
        SessionResponse second = findSession(secondSessionId).orElseThrow();
        jdbc.update("""
                UPDATE aura_scheduled_sessions
                SET room_id = CASE
                        WHEN id = :firstId THEN :secondRoomId
                        ELSE :firstRoomId END,
                    timeslot_id = CASE
                        WHEN id = :firstId THEN :secondTimeslotId
                        ELSE :firstTimeslotId END,
                    source = 'MANUAL', updated_at = CURRENT_TIMESTAMP
                WHERE id IN (:firstId, :secondId)
                """, params()
                .addValue("firstId", firstSessionId)
                .addValue("secondId", secondSessionId)
                .addValue("firstRoomId", first.roomId())
                .addValue("firstTimeslotId", first.timeslotId())
                .addValue("secondRoomId", second.roomId())
                .addValue("secondTimeslotId", second.timeslotId()));
        insertManualMove(first, second.roomId(), second.timeslotId(), userId, reason);
        insertManualMove(second, first.roomId(), first.timeslotId(), userId, reason);
    }

    public void setSessionPinned(UUID sessionId, boolean pinned, String reason) {
        jdbc.update("""
                UPDATE aura_scheduled_sessions
                SET pinned = :pinned, locked = :pinned,
                    lock_reason = CASE WHEN :pinned THEN :reason ELSE NULL END,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :sessionId
                """, params()
                .addValue("sessionId", sessionId)
                .addValue("pinned", pinned)
                .addValue("reason", blankToNull(reason)));
    }

    public void archiveVersion(UUID versionId) {
        jdbc.update("""
                UPDATE aura_timetable_versions
                SET status = 'ARCHIVED', version = version + 1
                WHERE id = :versionId AND status = 'DRAFT'
                """, params().addValue("versionId", versionId));
    }

    public void insertVersionAudit(
            UUID versionId,
            UUID userId,
            String action,
            String summary) {
        jdbc.update("""
                INSERT INTO aura_audit_events (
                    id, university_id, term_id, actor_user_id, action,
                    target_type, target_id, summary
                )
                SELECT :id, term_row.university_id, timetable_version.term_id,
                    :userId, :action, 'TIMETABLE_VERSION',
                    timetable_version.id, :summary
                FROM aura_timetable_versions timetable_version
                JOIN aura_academic_terms term_row
                  ON term_row.id = timetable_version.term_id
                WHERE timetable_version.id = :versionId
                """, params()
                .addValue("id", UUID.randomUUID())
                .addValue("versionId", versionId)
                .addValue("userId", userId)
                .addValue("action", action)
                .addValue("summary", summary));
    }

    public void publishVersion(UUID versionId, UUID termId) {
        jdbc.update("""
                UPDATE aura_timetable_versions
                SET status = 'SUPERSEDED'
                WHERE term_id = :termId AND status = 'PUBLISHED'
                """, params().addValue("termId", termId));
        jdbc.update("""
                UPDATE aura_timetable_versions
                SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP
                WHERE id = :versionId
                """, params().addValue("versionId", versionId));
        jdbc.update("""
                UPDATE aura_academic_terms
                SET status = 'PUBLISHED', updated_at = CURRENT_TIMESTAMP
                WHERE id = :termId
                """, params().addValue("termId", termId));
    }

    public boolean isVersionStale(UUID versionId) {
        Boolean stale = jdbc.queryForObject("""
                SELECT timetable_version.input_revision <> term_row.data_revision
                FROM aura_timetable_versions timetable_version
                JOIN aura_academic_terms term_row
                  ON term_row.id = timetable_version.term_id
                WHERE timetable_version.id = :versionId
                """, params().addValue("versionId", versionId), Boolean.class);
        return Boolean.TRUE.equals(stale);
    }

    public long countExpectedOccurrences(UUID termId) {
        return count("""
                SELECT COALESCE(SUM(requirement.sessions_per_week), 0)
                FROM aura_meeting_requirements requirement
                JOIN aura_course_offerings offering
                  ON offering.id = requirement.offering_id
                WHERE offering.term_id = :termId
                  AND offering.status = 'ACTIVE'
                  AND requirement.active = TRUE
                """, params().addValue("termId", termId));
    }

    public long countScheduledSessions(UUID versionId) {
        return count("""
                SELECT COUNT(*) FROM aura_scheduled_sessions
                WHERE version_id = :versionId
                """, params().addValue("versionId", versionId));
    }

    public long countOpenHardClashes(UUID versionId) {
        return count("""
                SELECT COUNT(*)
                FROM aura_clashes
                WHERE version_id = :versionId
                  AND severity = 'HARD'
                  AND resolved_at IS NULL
                """, params().addValue("versionId", versionId));
    }

    public List<SessionResponse> listSessions(UUID versionId) {
        return jdbc.query(sessionSql("""
                WHERE s.version_id = :versionId
                ORDER BY ts.day_of_week ASC, ts.starts_at ASC, c.course_code ASC
                """), params().addValue("versionId", versionId),
                sessionMapper());
    }

    public Optional<SessionResponse> findSession(UUID sessionId) {
        return optional(sessionSql("""
                WHERE s.id = :sessionId
                """), params().addValue("sessionId", sessionId),
                sessionMapper());
    }

    public void moveSession(
            UUID sessionId,
            UUID roomId,
            UUID timeslotId,
            UUID userId,
            String reason) {
        SessionResponse before = findSession(sessionId)
                .orElseThrow();
        jdbc.update("""
                UPDATE aura_scheduled_sessions
                SET room_id = :roomId, timeslot_id = :timeslotId,
                    source = 'MANUAL', updated_at = CURRENT_TIMESTAMP
                WHERE id = :sessionId
                """, params()
                .addValue("sessionId", sessionId)
                .addValue("roomId", roomId)
                .addValue("timeslotId", timeslotId));
        insertManualMove(before, roomId, timeslotId, userId, reason);
    }

    private void insertManualMove(
            SessionResponse before,
            UUID roomId,
            UUID timeslotId,
            UUID userId,
            String reason) {
        jdbc.update("""
                INSERT INTO aura_manual_moves (
                    id, session_id, previous_room_id, previous_timeslot_id,
                    new_room_id, new_timeslot_id, reason, moved_by_user_id
                ) VALUES (
                    :id, :sessionId, :previousRoomId, :previousTimeslotId,
                    :newRoomId, :newTimeslotId, :reason, :userId
                )
                """, params()
                .addValue("id", UUID.randomUUID())
                .addValue("sessionId", before.id())
                .addValue("previousRoomId", before.roomId())
                .addValue("previousTimeslotId", before.timeslotId())
                .addValue("newRoomId", roomId)
                .addValue("newTimeslotId", timeslotId)
                .addValue("reason", reason.trim())
                .addValue("userId", userId));
    }

    public void replaceClashes(UUID versionId, Collection<DetectedClash> clashes) {
        jdbc.update("""
                DELETE FROM aura_clashes WHERE version_id = :versionId
                """, params().addValue("versionId", versionId));
        clashes.forEach(clash -> jdbc.update("""
                INSERT INTO aura_clashes (
                    id, version_id, clash_type, severity, message,
                    primary_session_id, secondary_session_id
                ) VALUES (
                    :id, :versionId, :clashType, :severity, :message,
                    :primarySessionId, :secondarySessionId
                )
                """, params()
                .addValue("id", UUID.randomUUID())
                .addValue("versionId", versionId)
                .addValue("clashType", clash.clashType())
                .addValue("severity", clash.severity())
                .addValue("message", clash.message())
                .addValue("primarySessionId", clash.primarySessionId())
                .addValue("secondarySessionId", clash.secondarySessionId())));
    }

    public List<ClashResponse> listClashes(UUID versionId) {
        return jdbc.query("""
                SELECT * FROM aura_clashes
                WHERE version_id = :versionId AND resolved_at IS NULL
                ORDER BY detected_at DESC
                """, params().addValue("versionId", versionId), clashMapper());
    }

    public AuraDtos.AuraMetricsResponse metrics(UUID termId) {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM aura_timetable_versions WHERE term_id = :termId) AS versions,
                    (SELECT COUNT(*) FROM aura_timetable_versions WHERE term_id = :termId AND status = 'PUBLISHED') AS published_versions,
                    (SELECT COUNT(*) FROM aura_scheduled_sessions s
                        JOIN aura_timetable_versions v ON v.id = s.version_id
                        WHERE v.term_id = :termId) AS scheduled_sessions,
                    (SELECT COUNT(*) FROM aura_clashes c
                        JOIN aura_timetable_versions v ON v.id = c.version_id
                        WHERE v.term_id = :termId AND c.resolved_at IS NULL) AS unresolved_clashes,
                    (SELECT COUNT(*) FROM aura_rooms r
                        JOIN aura_academic_terms t ON t.university_id = r.university_id
                        WHERE t.id = :termId AND r.active = TRUE) AS rooms,
                    (SELECT COUNT(*) FROM aura_timeslots ts
                        JOIN aura_academic_terms t ON t.university_id = ts.university_id
                        WHERE t.id = :termId AND ts.active = TRUE) AS timeslots,
                    (SELECT COUNT(*) FROM aura_course_offerings WHERE term_id = :termId) AS offerings
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new AuraDtos.AuraMetricsResponse(
                        termId,
                        rs.getLong("versions"),
                        rs.getLong("published_versions"),
                        rs.getLong("scheduled_sessions"),
                        rs.getLong("unresolved_clashes"),
                        rs.getLong("rooms"),
                        rs.getLong("timeslots"),
                        rs.getLong("offerings")));
    }

    private String sessionSql(String suffix) {
        return """
                SELECT s.*, c.course_code, c.title AS course_title,
                    sec.display_name AS section_name,
                    ins.display_name AS instructor_name,
                    r.name AS room_name, r.room_type, r.capacity AS room_capacity,
                    r.active AS room_active, ins.active AS instructor_active,
                    sec.active AS section_active,
                    requirement.sessions_per_week, requirement.duration_slots,
                    requirement.required_capacity,
                    requirement.room_type AS required_room_type,
                    requirement.fixed_room_id,
                    requirement.fixed_timeslot_id, requirement.week_pattern,
                    requirement.custom_weeks, ts.slot_type,
                    ts.day_of_week, ts.starts_at,
                    COALESCE((
                        SELECT block.ends_at
                        FROM aura_timeslots block
                        WHERE block.university_id = ts.university_id
                          AND block.term_id IS NOT DISTINCT FROM ts.term_id
                          AND block.day_of_week = ts.day_of_week
                          AND block.slot_order = ts.slot_order
                            + requirement.duration_slots - 1
                          AND block.active = TRUE
                          AND block.slot_type = 'INSTRUCTIONAL'
                    ), ts.ends_at) AS ends_at,
                    CASE
                      WHEN requirement.duration_slots = 1
                           AND ts.slot_type = 'INSTRUCTIONAL' THEN 1
                      WHEN ts.slot_order IS NULL OR ts.slot_type <> 'INSTRUCTIONAL'
                           THEN 0
                      WHEN (
                        SELECT COUNT(*)
                        FROM aura_timeslots block
                        WHERE block.university_id = ts.university_id
                          AND block.term_id IS NOT DISTINCT FROM ts.term_id
                          AND block.day_of_week = ts.day_of_week
                          AND block.slot_order BETWEEN ts.slot_order
                            AND ts.slot_order + requirement.duration_slots - 1
                          AND block.active = TRUE
                          AND block.slot_type = 'INSTRUCTIONAL'
                      ) = requirement.duration_slots
                      AND NOT EXISTS (
                        SELECT 1
                        FROM aura_timeslots block
                        JOIN aura_timeslots previous
                          ON previous.university_id = block.university_id
                         AND previous.term_id IS NOT DISTINCT FROM block.term_id
                         AND previous.day_of_week = block.day_of_week
                         AND previous.slot_order = block.slot_order - 1
                        WHERE block.university_id = ts.university_id
                          AND block.term_id IS NOT DISTINCT FROM ts.term_id
                          AND block.day_of_week = ts.day_of_week
                          AND block.slot_order BETWEEN ts.slot_order + 1
                            AND ts.slot_order + requirement.duration_slots - 1
                          AND previous.ends_at <> block.starts_at
                      ) THEN requirement.duration_slots
                      ELSE 0
                    END AS contiguous_slots_available,
                    COALESCE((
                        SELECT STRING_AGG(facility.facility, ',' ORDER BY facility.facility)
                        FROM aura_meeting_requirement_facilities facility
                        WHERE facility.meeting_requirement_id = requirement.id
                    ), '') AS required_facilities,
                    COALESCE((
                        SELECT STRING_AGG(facility.facility, ',' ORDER BY facility.facility)
                        FROM aura_room_facilities facility
                        WHERE facility.room_id = r.id
                    ), '') AS room_facilities,
                    COALESCE((
                        SELECT STRING_AGG(
                            CASE WHEN conflict.left_offering_id = s.offering_id
                                THEN conflict.right_offering_id::TEXT
                                ELSE conflict.left_offering_id::TEXT END, ',')
                        FROM aura_cross_offering_conflicts conflict
                        WHERE conflict.active = TRUE AND conflict.severity = 'HARD'
                          AND (conflict.left_offering_id = s.offering_id
                            OR conflict.right_offering_id = s.offering_id)
                    ), '') AS hard_conflict_offering_ids,
                    EXISTS (
                        SELECT 1 FROM aura_instructor_availability availability
                        JOIN aura_timeslots blocked
                          ON blocked.id = availability.timeslot_id
                        WHERE availability.instructor_id = s.instructor_id
                          AND availability.availability = 'UNAVAILABLE'
                          AND blocked.day_of_week = ts.day_of_week
                          AND (blocked.id = ts.id OR (
                            ts.slot_order IS NOT NULL
                            AND blocked.slot_order BETWEEN ts.slot_order
                              AND ts.slot_order + requirement.duration_slots - 1))
                    ) AS instructor_unavailable,
                    EXISTS (
                        SELECT 1 FROM aura_room_availability availability
                        JOIN aura_timeslots blocked
                          ON blocked.id = availability.timeslot_id
                        WHERE availability.room_id = s.room_id
                          AND availability.availability = 'UNAVAILABLE'
                          AND blocked.day_of_week = ts.day_of_week
                          AND (blocked.id = ts.id OR (
                            ts.slot_order IS NOT NULL
                            AND blocked.slot_order BETWEEN ts.slot_order
                              AND ts.slot_order + requirement.duration_slots - 1))
                    ) AS room_unavailable,
                    EXISTS (
                        SELECT 1 FROM aura_section_availability availability
                        JOIN aura_timeslots blocked
                          ON blocked.id = availability.timeslot_id
                        WHERE availability.section_id = s.section_id
                          AND availability.availability = 'UNAVAILABLE'
                          AND blocked.day_of_week = ts.day_of_week
                          AND (blocked.id = ts.id OR (
                            ts.slot_order IS NOT NULL
                            AND blocked.slot_order BETWEEN ts.slot_order
                              AND ts.slot_order + requirement.duration_slots - 1))
                    ) AS section_unavailable,
                    EXISTS (
                        SELECT 1 FROM aura_calendar_exceptions exception_row
                        WHERE exception_row.term_id = version_row.term_id
                          AND exception_row.active = TRUE
                          AND (
                            exception_row.exception_type IN (
                              'HOLIDAY', 'NON_TEACHING_DAY', 'UNIVERSITY_EVENT')
                            OR exception_row.instructor_id = s.instructor_id
                            OR exception_row.room_id = s.room_id
                            OR exception_row.section_id = s.section_id
                            OR exception_row.timeslot_id = s.timeslot_id
                            OR (exception_row.exception_type = 'FACILITY_OUTAGE'
                              AND EXISTS (
                                SELECT 1 FROM aura_room_facilities room_facility
                                WHERE room_facility.room_id = s.room_id
                                  AND room_facility.facility = exception_row.facility))
                          )
                          AND EXISTS (
                            SELECT 1
                            FROM GENERATE_SERIES(
                              exception_row.starts_on,
                              exception_row.ends_on,
                              INTERVAL '1 day') AS affected_date
                            WHERE EXTRACT(ISODOW FROM affected_date) = ts.day_of_week)
                    ) AS calendar_exception
                FROM aura_scheduled_sessions s
                JOIN aura_timetable_versions version_row ON version_row.id = s.version_id
                JOIN aura_course_offerings o ON o.id = s.offering_id
                JOIN courses c ON c.id = o.course_id
                JOIN aura_sections sec ON sec.id = s.section_id
                JOIN aura_instructors ins ON ins.id = s.instructor_id
                JOIN aura_rooms r ON r.id = s.room_id
                JOIN aura_timeslots ts ON ts.id = s.timeslot_id
                JOIN aura_meeting_requirements requirement
                  ON requirement.id = s.meeting_requirement_id
                """ + suffix;
    }

    private long count(String sql, MapSqlParameterSource params) {
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count == null ? 0 : count;
    }

    private <T> Optional<T> optional(
            String sql,
            MapSqlParameterSource params,
            RowMapper<T> mapper) {
        List<T> results = jdbc.query(sql, params, mapper);
        return results.stream().findFirst();
    }

    private Optional<UUID> optionalUuid(
            String sql,
            MapSqlParameterSource params) {
        List<UUID> results = jdbc.query(
                sql,
                params,
                (rs, rowNum) -> rs.getObject(1, UUID.class));
        return results.stream().findFirst();
    }

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource();
    }

    private MapSqlParameterSource pageParams(int page, int size) {
        return params()
                .addValue("size", size)
                .addValue("offset", (long) page * size);
    }

    private String normalizeEnum(String value) {
        return value == null ? null : value.trim().toUpperCase().replace('-', '_');
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<String> splitCsv(String value) {
        return value == null || value.isBlank()
                ? List.of()
                : List.of(value.split(","));
    }

    private UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private UUID nullableUuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private List<Integer> intArray(ResultSet rs, String column)
            throws SQLException {
        java.sql.Array sqlArray = rs.getArray(column);
        if (sqlArray == null) {
            return List.of();
        }
        Object raw = sqlArray.getArray();
        if (!(raw instanceof Object[] values)) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>(values.length);
        for (Object value : values) {
            if (value instanceof Number number) {
                result.add(number.intValue());
            }
        }
        return List.copyOf(result);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private RowMapper<TermResponse> termMapper() {
        return (rs, rowNum) -> new TermResponse(
                uuid(rs, "id"),
                uuid(rs, "university_id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getObject("starts_on", LocalDate.class),
                rs.getObject("ends_on", LocalDate.class),
                rs.getString("status"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private RowMapper<ProgramResponse> programMapper() {
        return (rs, rowNum) -> new ProgramResponse(
                uuid(rs, "id"),
                uuid(rs, "university_id"),
                uuid(rs, "department_id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getBoolean("active"));
    }

    private RowMapper<BatchResponse> batchMapper() {
        return (rs, rowNum) -> new BatchResponse(
                uuid(rs, "id"),
                uuid(rs, "program_id"),
                rs.getString("code"),
                rs.getInt("admission_year"),
                rs.getBoolean("active"));
    }

    private RowMapper<SectionResponse> sectionMapper() {
        return (rs, rowNum) -> new SectionResponse(
                uuid(rs, "id"),
                uuid(rs, "batch_id"),
                rs.getString("code"),
                rs.getString("display_name"),
                rs.getInt("student_count"),
                rs.getBoolean("active"));
    }

    private RowMapper<InstructorResponse> instructorMapper() {
        return (rs, rowNum) -> new InstructorResponse(
                uuid(rs, "id"),
                uuid(rs, "university_id"),
                uuid(rs, "user_id"),
                rs.getString("display_name"),
                rs.getString("email"),
                rs.getInt("max_hours_per_week"),
                rs.getBoolean("active"));
    }

    private RowMapper<RoomResponse> roomMapper() {
        return (rs, rowNum) -> new RoomResponse(
                uuid(rs, "id"),
                uuid(rs, "university_id"),
                rs.getString("building"),
                rs.getString("name"),
                rs.getInt("capacity"),
                rs.getString("room_type"),
                splitCsv(rs.getString("facilities")),
                rs.getBoolean("active"));
    }

    private RowMapper<TimeslotResponse> timeslotMapper() {
        return (rs, rowNum) -> new TimeslotResponse(
                uuid(rs, "id"),
                uuid(rs, "university_id"),
                rs.getInt("day_of_week"),
                rs.getObject("starts_at", LocalTime.class),
                rs.getObject("ends_at", LocalTime.class),
                rs.getString("label"),
                rs.getBoolean("active"));
    }

    private RowMapper<AvailabilityResponse> availabilityMapper() {
        return (rs, rowNum) -> new AvailabilityResponse(
                uuid(rs, "id"),
                uuid(rs, "target_id"),
                uuid(rs, "timeslot_id"),
                rs.getInt("day_of_week"),
                rs.getObject("starts_at", LocalTime.class),
                rs.getObject("ends_at", LocalTime.class),
                rs.getString("label"),
                rs.getString("availability"),
                rs.getString("reason"));
    }

    private RowMapper<OfferingResponse> offeringMapper() {
        return (rs, rowNum) -> new OfferingResponse(
                uuid(rs, "id"),
                uuid(rs, "term_id"),
                uuid(rs, "course_id"),
                rs.getString("course_code"),
                rs.getString("course_title"),
                uuid(rs, "section_id"),
                rs.getString("section_name"),
                uuid(rs, "instructor_id"),
                rs.getString("instructor_name"),
                rs.getInt("expected_students"),
                rs.getString("status"));
    }

    private RowMapper<MeetingRequirementResponse> requirementMapper() {
        return (rs, rowNum) -> new MeetingRequirementResponse(
                uuid(rs, "id"),
                uuid(rs, "offering_id"),
                rs.getString("meeting_type"),
                rs.getInt("sessions_per_week"),
                rs.getInt("duration_slots"),
                rs.getString("room_type"),
                rs.getInt("required_capacity"),
                rs.getString("notes"),
                splitCsv(rs.getString("required_facilities")));
    }

    private RowMapper<GenerationRunResponse> runMapper() {
        return (rs, rowNum) -> new GenerationRunResponse(
                uuid(rs, "id"),
                uuid(rs, "term_id"),
                uuid(rs, "revision_id"),
                rs.getString("status"),
                rs.getString("score"),
                rs.getInt("termination_seconds"),
                rs.getString("message"),
                instant(rs, "started_at"),
                instant(rs, "completed_at"),
                instant(rs, "cancelled_at"),
                instant(rs, "created_at"));
    }

    private RowMapper<TimetableVersionResponse> versionMapper() {
        return (rs, rowNum) -> new TimetableVersionResponse(
                uuid(rs, "id"),
                uuid(rs, "term_id"),
                uuid(rs, "generation_run_id"),
                rs.getInt("version_number"),
                rs.getString("status"),
                rs.getString("score"),
                rs.getString("notes"),
                instant(rs, "created_at"),
                instant(rs, "published_at"));
    }

    private RowMapper<SessionResponse> sessionMapper() {
        return (rs, rowNum) -> new SessionResponse(
                uuid(rs, "id"),
                uuid(rs, "version_id"),
                uuid(rs, "offering_id"),
                uuid(rs, "meeting_requirement_id"),
                rs.getString("course_code"),
                rs.getString("course_title"),
                uuid(rs, "section_id"),
                rs.getString("section_name"),
                uuid(rs, "instructor_id"),
                rs.getString("instructor_name"),
                uuid(rs, "room_id"),
                rs.getString("room_name"),
                rs.getString("room_type"),
                uuid(rs, "timeslot_id"),
                rs.getInt("day_of_week"),
                rs.getObject("starts_at", LocalTime.class),
                rs.getObject("ends_at", LocalTime.class),
                rs.getBoolean("locked"),
                rs.getString("source"),
                rs.getInt("occurrence_index"),
                rs.getInt("sessions_per_week"),
                rs.getInt("duration_slots"),
                rs.getInt("contiguous_slots_available"),
                rs.getInt("required_capacity"),
                rs.getString("required_room_type"),
                rs.getInt("room_capacity"),
                splitCsv(rs.getString("required_facilities")),
                splitCsv(rs.getString("room_facilities")),
                rs.getString("slot_type"),
                rs.getBoolean("room_active"),
                rs.getBoolean("instructor_active"),
                rs.getBoolean("section_active"),
                nullableUuid(rs, "fixed_room_id"),
                nullableUuid(rs, "fixed_timeslot_id"),
                rs.getString("week_pattern"),
                intArray(rs, "custom_weeks"),
                splitUuidCsv(rs.getString("hard_conflict_offering_ids")),
                rs.getBoolean("instructor_unavailable"),
                rs.getBoolean("room_unavailable"),
                rs.getBoolean("section_unavailable"),
                rs.getBoolean("calendar_exception"));
    }

    private List<UUID> splitUuidCsv(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(UUID::fromString)
                .distinct()
                .toList();
    }

    private RowMapper<ClashResponse> clashMapper() {
        return (rs, rowNum) -> new ClashResponse(
                uuid(rs, "id"),
                uuid(rs, "version_id"),
                rs.getString("clash_type"),
                rs.getString("severity"),
                rs.getString("message"),
                uuid(rs, "primary_session_id"),
                uuid(rs, "secondary_session_id"),
                instant(rs, "detected_at"),
                instant(rs, "resolved_at"));
    }

    private RowMapper<SetupReferenceOption> setupReferenceMapper() {
        return (rs, rowNum) -> new SetupReferenceOption(
                uuid(rs, "id"),
                uuid(rs, "parent_id"),
                rs.getString("code"),
                rs.getString("name"));
    }

    private RowMapper<CalendarExceptionResponse> calendarExceptionMapper() {
        return (rs, rowNum) -> new CalendarExceptionResponse(
                uuid(rs, "id"),
                uuid(rs, "term_id"),
                rs.getString("exception_type"),
                rs.getObject("starts_on", LocalDate.class),
                rs.getObject("ends_on", LocalDate.class),
                uuid(rs, "instructor_id"),
                uuid(rs, "room_id"),
                uuid(rs, "section_id"),
                uuid(rs, "timeslot_id"),
                rs.getString("facility"),
                rs.getString("reason"),
                rs.getBoolean("active"),
                rs.getLong("version"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    public record TermCounts(
            int rooms,
            int timeslots,
            int instructors,
            int offerings,
            int requirements) {
    }

    public record RequirementCandidateIssue(
            UUID requirementId,
            String courseCode,
            String courseTitle) {
    }

    public record SolverRoom(
            UUID id,
            int capacity,
            String roomType,
            List<String> facilities,
            String building) {

        public SolverRoom(
                UUID id,
                int capacity,
                String roomType,
                List<String> facilities) {
            this(id, capacity, roomType, facilities, null);
        }
    }

    public record SolverTimeslot(
            UUID id,
            int dayOfWeek,
            LocalTime startsAt,
            LocalTime endsAt,
            int slotOrder,
            String slotType) {

        public SolverTimeslot(
                UUID id,
                int dayOfWeek,
                LocalTime startsAt,
                LocalTime endsAt) {
            this(id, dayOfWeek, startsAt, endsAt, 100, "INSTRUCTIONAL");
        }
    }

    public record SolverInstructorAvailability(
            UUID instructorId,
            UUID timeslotId,
            String availability) {
    }

    public record SolverRoomAvailability(
            UUID roomId,
            UUID timeslotId,
            String availability) {
    }

    public record SolverSectionAvailability(
            UUID sectionId,
            UUID timeslotId,
            String availability) {
    }

    public record SolverStudentAvailability(
            UUID offeringId,
            UUID studentUserId,
            UUID timeslotId,
            String availability) {
    }

    public record SolverTravelRuleInput(
            String fromBuilding,
            String toBuilding,
            int minutes,
            String difficulty) {
    }

    public record SolverRequirement(
            UUID requirementId,
            UUID offeringId,
            UUID sectionId,
            UUID instructorId,
            int sessionsPerWeek,
            String roomType,
            int requiredCapacity,
            List<String> requiredFacilities,
            int durationSlots,
            List<Integer> allowedDays,
            List<Integer> prohibitedDays,
            List<Integer> preferredDays,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime,
            int minimumDaySeparation,
            int maximumOccurrencesPerDay,
            boolean sameRoomPreferred,
            UUID fixedRoomId,
            UUID fixedTimeslotId,
            boolean pinned,
            String meetingType,
            String teachingGroup,
            UUID linkedRequirementId,
            boolean lectureBeforeLinked,
            String weekPattern,
            List<Integer> customWeeks,
            List<UUID> participatingSectionIds,
            List<UUID> studentUserIds,
            List<UUID> hardConflictOfferingIds,
            int instructorHardWeeklyLoad,
            int instructorHardDailyLoad,
            int instructorPreferredWeeklyLoad,
            int instructorPreferredDailyLoad,
            int maximumConsecutiveSlots,
            int sectionHardDailyLoad,
            int sectionPreferredDailyLoad) {

        public SolverRequirement(
                UUID requirementId,
                UUID offeringId,
                UUID sectionId,
                UUID instructorId,
                int sessionsPerWeek,
                String roomType,
                int requiredCapacity,
                List<String> requiredFacilities) {
            this(
                    requirementId, offeringId, sectionId, instructorId,
                    sessionsPerWeek, roomType, requiredCapacity,
                    requiredFacilities, 1, List.of(), List.of(), List.of(),
                    null, null, 0, 1, true, null, null, false,
                    "LECTURE", null, null, false, "EVERY_WEEK", List.of(),
                    List.of(sectionId), List.of(), List.of(),
                    60, 24, 60, 24, 12, 24, 24);
        }

        public SolverRequirement(
                UUID requirementId,
                UUID offeringId,
                UUID sectionId,
                UUID instructorId,
                int sessionsPerWeek,
                String roomType,
                int requiredCapacity,
                List<String> requiredFacilities,
                int durationSlots,
                List<Integer> allowedDays,
                List<Integer> prohibitedDays,
                List<Integer> preferredDays,
                LocalTime preferredStartTime,
                LocalTime preferredEndTime,
                int minimumDaySeparation,
                int maximumOccurrencesPerDay,
                boolean sameRoomPreferred,
                UUID fixedRoomId,
                UUID fixedTimeslotId,
                boolean pinned,
                String meetingType,
                String weekPattern,
                int instructorHardWeeklyLoad,
                int instructorHardDailyLoad,
                int instructorPreferredWeeklyLoad,
                int instructorPreferredDailyLoad,
                int maximumConsecutiveSlots,
                int sectionHardDailyLoad,
                int sectionPreferredDailyLoad) {
            this(
                    requirementId, offeringId, sectionId, instructorId,
                    sessionsPerWeek, roomType, requiredCapacity,
                    requiredFacilities, durationSlots, allowedDays,
                    prohibitedDays, preferredDays, preferredStartTime,
                    preferredEndTime, minimumDaySeparation,
                    maximumOccurrencesPerDay, sameRoomPreferred, fixedRoomId,
                    fixedTimeslotId, pinned, meetingType, null, null, false,
                    weekPattern, List.of(), List.of(sectionId), List.of(),
                    List.of(), instructorHardWeeklyLoad,
                    instructorHardDailyLoad, instructorPreferredWeeklyLoad,
                    instructorPreferredDailyLoad, maximumConsecutiveSlots,
                    sectionHardDailyLoad, sectionPreferredDailyLoad);
        }
    }

    public record SolverAssignment(
            UUID requirementId,
            UUID offeringId,
            UUID sectionId,
            UUID instructorId,
            UUID roomId,
            UUID timeslotId) {
    }

    public record DetectedClash(
            String clashType,
            String severity,
            String message,
            UUID primarySessionId,
            UUID secondarySessionId) {
    }

    public record VersionSessionSnapshot(
            UUID sessionId,
            UUID meetingRequirementId,
            int occurrenceIndex,
            UUID roomId,
            UUID timeslotId,
            UUID instructorId,
            UUID sectionId) {
    }

    private record CloneSessionRow(
            UUID meetingRequirementId,
            UUID offeringId,
            UUID sectionId,
            UUID instructorId,
            UUID roomId,
            UUID timeslotId,
            boolean locked,
            String source,
            int occurrenceIndex,
            boolean pinned,
            String lockReason) {
    }

    public enum ScopedResource {
        TERM,
        PROGRAM,
        BATCH,
        SECTION,
        INSTRUCTOR,
        ROOM,
        TIMESLOT,
        OFFERING,
        REQUIREMENT,
        RUN,
        VERSION,
        SESSION,
        CALENDAR_EXCEPTION
    }
}
