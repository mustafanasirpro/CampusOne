package com.campusone.aura.repository;

import com.campusone.aura.dto.AuraDtos;
import com.campusone.aura.dto.AuraDtos.AvailabilityResponse;
import com.campusone.aura.dto.AuraDtos.BatchResponse;
import com.campusone.aura.dto.AuraDtos.ClashResponse;
import com.campusone.aura.dto.AuraDtos.GenerationRunResponse;
import com.campusone.aura.dto.AuraDtos.InstructorResponse;
import com.campusone.aura.dto.AuraDtos.MeetingRequirementResponse;
import com.campusone.aura.dto.AuraDtos.OfferingResponse;
import com.campusone.aura.dto.AuraDtos.ProgramResponse;
import com.campusone.aura.dto.AuraDtos.RoomResponse;
import com.campusone.aura.dto.AuraDtos.SectionResponse;
import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.dto.AuraDtos.TermResponse;
import com.campusone.aura.dto.AuraDtos.TimetableVersionResponse;
import com.campusone.aura.dto.AuraDtos.TimeslotResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    public List<TermResponse> listTerms(int page, int size) {
        return jdbc.query("""
                SELECT * FROM aura_academic_terms
                ORDER BY created_at DESC
                LIMIT :size OFFSET :offset
                """, pageParams(page, size), termMapper());
    }

    public long countTerms() {
        return count("SELECT COUNT(*) FROM aura_academic_terms", params());
    }

    public Optional<TermResponse> findTerm(UUID termId) {
        return optional("""
                SELECT * FROM aura_academic_terms WHERE id = :id
                """, params().addValue("id", termId), termMapper());
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

    public List<BatchResponse> listBatches(UUID programId) {
        return jdbc.query("""
                SELECT * FROM aura_batches
                WHERE (:programId IS NULL OR program_id = :programId)
                ORDER BY admission_year DESC, code ASC
                """, params().addValue("programId", programId), batchMapper());
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

    public List<SectionResponse> listSections(UUID batchId) {
        return jdbc.query("""
                SELECT * FROM aura_sections
                WHERE (:batchId IS NULL OR batch_id = :batchId)
                ORDER BY display_name ASC
                """, params().addValue("batchId", batchId), sectionMapper());
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
                    id, university_id, building, name, capacity, room_type
                ) VALUES (
                    :id, :universityId, :building, :name, :capacity, :roomType
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

    public List<RoomResponse> listRooms(UUID universityId) {
        return jdbc.query("""
                SELECT * FROM aura_rooms
                WHERE (:universityId IS NULL OR university_id = :universityId)
                ORDER BY name ASC
                """, params().addValue("universityId", universityId),
                roomMapper());
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

    public List<MeetingRequirementResponse> listMeetingRequirements(
            UUID offeringId) {
        return jdbc.query("""
                SELECT * FROM aura_meeting_requirements
                WHERE offering_id = :offeringId
                ORDER BY meeting_type ASC
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
                SELECT r.id, r.capacity, r.room_type
                FROM aura_rooms r
                JOIN aura_academic_terms t ON t.university_id = r.university_id
                WHERE t.id = :termId AND r.active = TRUE
                ORDER BY r.capacity ASC, r.name ASC
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new SolverRoom(
                        uuid(rs, "id"),
                        rs.getInt("capacity"),
                        rs.getString("room_type")));
    }

    public List<SolverTimeslot> solverTimeslots(UUID termId) {
        return jdbc.query("""
                SELECT ts.id, ts.day_of_week, ts.starts_at, ts.ends_at
                FROM aura_timeslots ts
                JOIN aura_academic_terms t ON t.university_id = ts.university_id
                WHERE t.id = :termId AND ts.active = TRUE
                ORDER BY ts.day_of_week ASC, ts.starts_at ASC
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new SolverTimeslot(
                        uuid(rs, "id"),
                        rs.getInt("day_of_week"),
                        rs.getObject("starts_at", LocalTime.class),
                        rs.getObject("ends_at", LocalTime.class)));
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

    public List<SolverRequirement> solverRequirements(UUID termId) {
        return jdbc.query("""
                SELECT r.id AS requirement_id, o.id AS offering_id,
                    o.section_id, o.instructor_id, r.sessions_per_week,
                    r.room_type, r.required_capacity
                FROM aura_meeting_requirements r
                JOIN aura_course_offerings o ON o.id = r.offering_id
                WHERE o.term_id = :termId AND o.status = 'ACTIVE'
                ORDER BY o.id ASC, r.id ASC
                """, params().addValue("termId", termId), (rs, rowNum) ->
                new SolverRequirement(
                        uuid(rs, "requirement_id"),
                        uuid(rs, "offering_id"),
                        uuid(rs, "section_id"),
                        uuid(rs, "instructor_id"),
                        rs.getInt("sessions_per_week"),
                        rs.getString("room_type"),
                        rs.getInt("required_capacity")));
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
                    termination_seconds
                ) VALUES (
                    :id, :termId, :revisionId, :userId, :terminationSeconds
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
                .addValue("startedAt", startedAt));
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
                    notes, created_by_user_id
                ) VALUES (
                    :id, :termId, :runId, :versionNumber, :score, :notes,
                    :userId
                )
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
                    section_id, instructor_id, room_id, timeslot_id, source
                ) VALUES (
                    :id, :versionId, :requirementId, :offeringId,
                    :sectionId, :instructorId, :roomId, :timeslotId, :source
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

    public void publishVersion(UUID versionId, UUID termId) {
        jdbc.update("""
                UPDATE aura_timetable_versions
                SET status = 'ARCHIVED'
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
                .addValue("sessionId", sessionId)
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
                    r.name AS room_name, r.room_type,
                    ts.day_of_week, ts.starts_at, ts.ends_at
                FROM aura_scheduled_sessions s
                JOIN aura_course_offerings o ON o.id = s.offering_id
                JOIN courses c ON c.id = o.course_id
                JOIN aura_sections sec ON sec.id = s.section_id
                JOIN aura_instructors ins ON ins.id = s.instructor_id
                JOIN aura_rooms r ON r.id = s.room_id
                JOIN aura_timeslots ts ON ts.id = s.timeslot_id
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
                (rs, rowNum) -> uuid(rs, "id"));
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

    private UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
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
                rs.getString("notes"));
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
                rs.getString("source"));
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

    public record SolverRoom(UUID id, int capacity, String roomType) {
    }

    public record SolverTimeslot(
            UUID id,
            int dayOfWeek,
            LocalTime startsAt,
            LocalTime endsAt) {
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

    public record SolverRequirement(
            UUID requirementId,
            UUID offeringId,
            UUID sectionId,
            UUID instructorId,
            int sessionsPerWeek,
            String roomType,
            int requiredCapacity) {
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
}
