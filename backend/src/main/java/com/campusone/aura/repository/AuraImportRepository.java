package com.campusone.aura.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.campusone.aura.dto.AuraImportDtos.ImportApplyResponse;
import com.campusone.aura.dto.AuraImportDtos.ImportRowIssue;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalTime;
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
public class AuraImportRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AuraImportRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void insertPreview(
            UUID id,
            UUID universityId,
            UUID termId,
            UUID userId,
            String importType,
            String format,
            String filename,
            String sourceName,
            List<String> headers,
            List<Map<String, String>> rows,
            Map<String, String> suggestedMapping,
            int totalRows,
            boolean ocrRequired) {
        jdbc.update("""
                INSERT INTO aura_import_jobs (
                    id, university_id, term_id, import_type, file_format,
                    original_filename, status, source_name, headers,
                    raw_preview, suggested_mapping, accepted_rows,
                    rejected_rows, message, created_by_user_id
                ) VALUES (
                    :id, :universityId, :termId, :importType, :format,
                    :filename, 'PREVIEWED', :sourceName,
                    CAST(:headers AS JSONB), CAST(:rows AS JSONB),
                    CAST(:suggestedMapping AS JSONB), :acceptedRows, 0,
                    :message, :userId
                )
                """, params()
                .addValue("id", id)
                .addValue("universityId", universityId)
                .addValue("termId", termId)
                .addValue("importType", importType)
                .addValue("format", format)
                .addValue("filename", filename)
                .addValue("sourceName", sourceName)
                .addValue("headers", json(headers))
                .addValue("rows", json(rows.stream().limit(200).toList()))
                .addValue("suggestedMapping", json(suggestedMapping))
                .addValue("acceptedRows", totalRows)
                .addValue("message", ocrRequired
                        ? "OCR_REQUIRED"
                        : "Import preview created.")
                .addValue("userId", userId));
        MapSqlParameterSource[] rowParameters = new MapSqlParameterSource[rows.size()];
        for (int index = 0; index < rows.size(); index++) {
            rowParameters[index] = params()
                    .addValue("jobId", id)
                    .addValue("rowNumber", index + 1)
                    .addValue("rowData", json(rows.get(index)));
        }
        if (rowParameters.length > 0) {
            jdbc.batchUpdate("""
                    INSERT INTO aura_import_source_rows (
                        import_job_id, row_number, row_data)
                    VALUES (:jobId, :rowNumber, CAST(:rowData AS JSONB))
                    """, rowParameters);
        }
    }

    public Optional<ImportJob> findJob(UUID jobId) {
        return jdbc.query("""
                SELECT id, university_id, term_id, import_type, status,
                    headers, suggested_mapping, applied_mapping,
                    accepted_rows, rejected_rows, result_version_id,
                    message, created_by_user_id
                FROM aura_import_jobs WHERE id = :jobId
                """, params().addValue("jobId", jobId), this::mapJob)
                .stream().findFirst();
    }

    public List<SourceRow> listSourceRows(UUID jobId) {
        return jdbc.query("""
                SELECT row_number, row_data
                FROM aura_import_source_rows
                WHERE import_job_id = :jobId
                ORDER BY row_number
                """, params().addValue("jobId", jobId), (rs, rowNum) ->
                new SourceRow(
                        rs.getInt("row_number"),
                        readMap(rs.getString("row_data"))));
    }

    public void replaceIssues(UUID jobId, List<ImportRowIssue> issues) {
        jdbc.update("DELETE FROM aura_import_row_errors WHERE import_job_id = :jobId",
                params().addValue("jobId", jobId));
        MapSqlParameterSource[] values = new MapSqlParameterSource[issues.size()];
        for (int index = 0; index < issues.size(); index++) {
            ImportRowIssue issue = issues.get(index);
            values[index] = params()
                    .addValue("id", UUID.randomUUID())
                    .addValue("jobId", jobId)
                    .addValue("rowNumber", issue.rowNumber())
                    .addValue("field", issue.field())
                    .addValue("code", issue.code())
                    .addValue("message", issue.message())
                    .addValue("severity", issue.severity());
        }
        if (values.length > 0) {
            jdbc.batchUpdate("""
                    INSERT INTO aura_import_row_errors (
                        id, import_job_id, row_number, field_name,
                        error_code, message, severity)
                    VALUES (
                        :id, :jobId, :rowNumber, :field,
                        :code, :message, :severity)
                    """, values);
        }
    }

    public void markValidated(
            UUID jobId,
            Map<String, String> mapping,
            int accepted,
            int rejected) {
        jdbc.update("""
                UPDATE aura_import_jobs
                SET status = :status, applied_mapping = CAST(:mapping AS JSONB),
                    accepted_rows = :accepted, rejected_rows = :rejected,
                    message = :message, updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :jobId AND status IN ('PREVIEWED', 'VALIDATED')
                """, params()
                .addValue("jobId", jobId)
                .addValue("status", rejected == 0 ? "VALIDATED" : "PREVIEWED")
                .addValue("mapping", json(mapping))
                .addValue("accepted", accepted)
                .addValue("rejected", rejected)
                .addValue("message", rejected == 0
                        ? "Import validation passed."
                        : "Correct the reported rows before applying this import."));
    }

    public void saveMappingProfile(
            UUID universityId,
            String importType,
            String name,
            Map<String, String> mapping,
            UUID userId) {
        jdbc.update("""
                INSERT INTO aura_import_mapping_profiles (
                    id, university_id, import_type, name, mapping,
                    created_by_user_id)
                VALUES (
                    :id, :universityId, :importType, :name,
                    CAST(:mapping AS JSONB), :userId)
                """, params()
                .addValue("id", UUID.randomUUID())
                .addValue("universityId", universityId)
                .addValue("importType", importType)
                .addValue("name", name.trim())
                .addValue("mapping", json(mapping))
                .addValue("userId", userId));
    }

    public void markApplying(UUID jobId) {
        int updated = jdbc.update("""
                UPDATE aura_import_jobs
                SET status = 'APPLYING', updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :jobId AND status = 'VALIDATED'
                """, params().addValue("jobId", jobId));
        if (updated != 1) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Import is not ready to apply");
        }
    }

    public void markApplied(UUID jobId, int accepted, UUID resultVersionId) {
        int updated = jdbc.update("""
                UPDATE aura_import_jobs
                SET status = 'APPLIED', accepted_rows = :accepted,
                    rejected_rows = 0, result_version_id = :resultVersionId,
                    message = 'Import applied successfully.',
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :jobId AND status = 'APPLYING'
                """, params().addValue("jobId", jobId)
                .addValue("accepted", accepted)
                .addValue("resultVersionId", resultVersionId));
        if (updated != 1) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Import application state changed");
        }
    }

    public ImportApplyResponse applyResponse(UUID jobId) {
        return jdbc.queryForObject("""
                SELECT id, import_type, status, accepted_rows,
                    rejected_rows, result_version_id, message
                FROM aura_import_jobs WHERE id = :jobId
                """, params().addValue("jobId", jobId), (rs, rowNum) ->
                new ImportApplyResponse(
                        uuid(rs, "id"), rs.getString("import_type"),
                        rs.getString("status"), rs.getInt("accepted_rows"),
                        rs.getInt("rejected_rows"),
                        nullableUuid(rs, "result_version_id"),
                        rs.getString("message")));
    }

    public Optional<UUID> findDepartment(UUID universityId, String code) {
        return findId("""
                SELECT id FROM departments
                WHERE university_id = :universityId
                  AND LOWER(code) = LOWER(:code) AND active = TRUE
                """, params().addValue("universityId", universityId)
                .addValue("code", code));
    }

    public Optional<UUID> findProgram(UUID universityId, String code) {
        return findId("""
                SELECT id FROM aura_programs
                WHERE university_id = :universityId
                  AND LOWER(code) = LOWER(:code) AND active = TRUE
                """, params().addValue("universityId", universityId)
                .addValue("code", code));
    }

    public Optional<UUID> findBatch(
            UUID universityId,
            String programCode,
            String batchCode) {
        return findId("""
                SELECT batch.id
                FROM aura_batches batch
                JOIN aura_programs program ON program.id = batch.program_id
                WHERE program.university_id = :universityId
                  AND LOWER(program.code) = LOWER(:programCode)
                  AND LOWER(batch.code) = LOWER(:batchCode)
                  AND program.active = TRUE AND batch.active = TRUE
                """, params().addValue("universityId", universityId)
                .addValue("programCode", programCode)
                .addValue("batchCode", batchCode));
    }

    public Optional<UUID> findSection(
            UUID universityId,
            UUID termId,
            String code) {
        return findId("""
                SELECT section_row.id
                FROM aura_sections section_row
                JOIN aura_batches batch ON batch.id = section_row.batch_id
                JOIN aura_programs program ON program.id = batch.program_id
                WHERE program.university_id = :universityId
                  AND (section_row.term_id IS NULL OR section_row.term_id = :termId)
                  AND (LOWER(section_row.code) = LOWER(:code)
                    OR LOWER(section_row.display_name) = LOWER(:code))
                  AND section_row.active = TRUE
                ORDER BY CASE WHEN section_row.term_id = :termId THEN 0 ELSE 1 END
                LIMIT 1
                """, params().addValue("universityId", universityId)
                .addValue("termId", termId).addValue("code", code));
    }

    public Optional<UUID> findInstructor(UUID universityId, String code) {
        return findId("""
                SELECT id FROM aura_instructors
                WHERE university_id = :universityId AND active = TRUE
                  AND (LOWER(employee_code) = LOWER(:code)
                    OR LOWER(display_name) = LOWER(:code)
                    OR LOWER(email) = LOWER(:code))
                ORDER BY CASE WHEN LOWER(employee_code) = LOWER(:code)
                    THEN 0 ELSE 1 END
                LIMIT 1
                """, params().addValue("universityId", universityId)
                .addValue("code", code));
    }

    public Optional<UUID> findRoom(UUID universityId, String code) {
        return findId("""
                SELECT id FROM aura_rooms
                WHERE university_id = :universityId AND active = TRUE
                  AND (LOWER(room_code) = LOWER(:code)
                    OR LOWER(name) = LOWER(:code)
                    OR LOWER(display_name) = LOWER(:code))
                ORDER BY CASE WHEN LOWER(room_code) = LOWER(:code)
                    THEN 0 ELSE 1 END
                LIMIT 1
                """, params().addValue("universityId", universityId)
                .addValue("code", code));
    }

    public Optional<UUID> findTimeslot(
            UUID universityId,
            UUID termId,
            int day,
            LocalTime start) {
        return findId("""
                SELECT id FROM aura_timeslots
                WHERE university_id = :universityId
                  AND (term_id IS NULL OR term_id = :termId)
                  AND day_of_week = :day AND starts_at = :start
                  AND active = TRUE
                ORDER BY CASE WHEN term_id = :termId THEN 0 ELSE 1 END
                LIMIT 1
                """, params().addValue("universityId", universityId)
                .addValue("termId", termId).addValue("day", day)
                .addValue("start", start));
    }

    public Optional<UUID> findTimeslotByLabel(
            UUID universityId,
            UUID termId,
            String label) {
        return findId("""
                SELECT id FROM aura_timeslots
                WHERE university_id = :universityId
                  AND (term_id IS NULL OR term_id = :termId)
                  AND LOWER(label) = LOWER(:label) AND active = TRUE
                ORDER BY CASE WHEN term_id = :termId THEN 0 ELSE 1 END
                LIMIT 1
                """, params().addValue("universityId", universityId)
                .addValue("termId", termId).addValue("label", label));
    }

    public Optional<UUID> findRoomForTerm(UUID termId, String code) {
        return findId("""
                SELECT room.id
                FROM aura_rooms room
                JOIN aura_academic_terms term_row
                  ON term_row.university_id = room.university_id
                WHERE term_row.id = :termId AND room.active = TRUE
                  AND (LOWER(room.room_code) = LOWER(:code)
                    OR LOWER(room.name) = LOWER(:code)
                    OR LOWER(room.display_name) = LOWER(:code))
                ORDER BY CASE WHEN LOWER(room.room_code) = LOWER(:code)
                    THEN 0 ELSE 1 END
                LIMIT 1
                """, params().addValue("termId", termId).addValue("code", code));
    }

    public Optional<UUID> findTimeslotForTerm(
            UUID termId,
            int day,
            LocalTime start) {
        return findId("""
                SELECT timeslot.id
                FROM aura_timeslots timeslot
                JOIN aura_academic_terms term_row
                  ON term_row.university_id = timeslot.university_id
                WHERE term_row.id = :termId
                  AND (timeslot.term_id IS NULL OR timeslot.term_id = :termId)
                  AND timeslot.day_of_week = :day
                  AND timeslot.starts_at = :start AND timeslot.active = TRUE
                ORDER BY CASE WHEN timeslot.term_id = :termId THEN 0 ELSE 1 END
                LIMIT 1
                """, params().addValue("termId", termId)
                .addValue("day", day).addValue("start", start));
    }

    public Optional<UUID> findCourse(UUID universityId, String code) {
        return findId("""
                SELECT course.id
                FROM courses course
                JOIN departments department ON department.id = course.department_id
                WHERE department.university_id = :universityId
                  AND LOWER(course.course_code) = LOWER(:code)
                  AND course.active = TRUE AND department.active = TRUE
                """, params().addValue("universityId", universityId)
                .addValue("code", code));
    }

    public Optional<UUID> findOffering(UUID termId, String code) {
        return findId("""
                SELECT offering.id
                FROM aura_course_offerings offering
                JOIN courses course ON course.id = offering.course_id
                WHERE offering.term_id = :termId AND offering.status = 'ACTIVE'
                  AND (LOWER(offering.offering_code) = LOWER(:code)
                    OR LOWER(course.course_code) = LOWER(:code))
                ORDER BY CASE WHEN LOWER(offering.offering_code) = LOWER(:code)
                    THEN 0 ELSE 1 END, offering.id
                LIMIT 1
                """, params().addValue("termId", termId).addValue("code", code));
    }

    public Optional<UUID> findRequirement(
            UUID termId,
            String offeringOrCourse,
            String meetingType) {
        return findId("""
                SELECT requirement.id
                FROM aura_meeting_requirements requirement
                JOIN aura_course_offerings offering
                  ON offering.id = requirement.offering_id
                JOIN courses course ON course.id = offering.course_id
                WHERE offering.term_id = :termId AND offering.status = 'ACTIVE'
                  AND requirement.active = TRUE
                  AND (LOWER(offering.offering_code) = LOWER(:code)
                    OR LOWER(course.course_code) = LOWER(:code))
                  AND requirement.meeting_type = :meetingType
                ORDER BY requirement.id LIMIT 1
                """, params().addValue("termId", termId)
                .addValue("code", offeringOrCourse)
                .addValue("meetingType", meetingType));
    }

    public Optional<UUID> findStudent(UUID universityId, String email) {
        return findId("""
                SELECT user_row.id
                FROM users user_row
                JOIN student_profiles profile ON profile.user_id = user_row.id
                WHERE profile.university_id = :universityId
                  AND LOWER(user_row.email) = LOWER(:email)
                  AND user_row.account_status = 'ACTIVE'
                """, params().addValue("universityId", universityId)
                .addValue("email", email));
    }

    public Optional<UUID> findTeachingGroup(
            UUID offeringId,
            String groupType,
            String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return findId("""
                SELECT id FROM aura_teaching_groups
                WHERE offering_id = :offeringId AND group_type = :groupType
                  AND LOWER(code) = LOWER(:code) AND active = TRUE
                """, params().addValue("offeringId", offeringId)
                .addValue("groupType", groupType).addValue("code", code));
    }

    public void updateProgramImportDetails(UUID id, Integer semesters) {
        if (semesters == null) return;
        jdbc.update("""
                UPDATE aura_programs SET number_of_semesters = :semesters,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :id
                """, params().addValue("id", id).addValue("semesters", semesters));
    }

    public void updateBatchImportDetails(UUID id, Integer graduationYear) {
        if (graduationYear == null) return;
        jdbc.update("""
                UPDATE aura_batches SET expected_graduation_year = :year,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :id
                """, params().addValue("id", id).addValue("year", graduationYear));
    }

    public void updateSectionImportDetails(
            UUID id,
            UUID termId,
            Integer semester) {
        jdbc.update("""
                UPDATE aura_sections SET term_id = :termId,
                    semester_number = :semester,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :id
                """, params().addValue("id", id).addValue("termId", termId)
                .addValue("semester", semester));
    }

    public void updateInstructorImportDetails(
            UUID id,
            String employeeCode,
            UUID departmentId) {
        jdbc.update("""
                UPDATE aura_instructors SET employee_code = :employeeCode,
                    department_id = :departmentId,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :id
                """, params().addValue("id", id)
                .addValue("employeeCode", employeeCode)
                .addValue("departmentId", departmentId));
    }

    public void updateRoomImportDetails(UUID id, String roomCode) {
        jdbc.update("""
                UPDATE aura_rooms SET room_code = :roomCode,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :id
                """, params().addValue("id", id).addValue("roomCode", roomCode));
    }

    public void updateTimeslotImportDetails(
            UUID id,
            UUID termId,
            int order,
            String slotType) {
        jdbc.update("""
                UPDATE aura_timeslots SET term_id = :termId,
                    slot_order = :slotOrder, slot_type = :slotType,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :id
                """, params().addValue("id", id).addValue("termId", termId)
                .addValue("slotOrder", order).addValue("slotType", slotType));
    }

    public void updateOfferingImportDetails(
            UUID id,
            String code,
            Integer maximumEnrollment,
            String parallelGroup,
            String electiveGroup) {
        jdbc.update("""
                UPDATE aura_course_offerings
                SET offering_code = :code, maximum_enrollment = :maximum,
                    parallel_group = :parallelGroup,
                    elective_group = :electiveGroup,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :id
                """, params().addValue("id", id).addValue("code", code)
                .addValue("maximum", maximumEnrollment)
                .addValue("parallelGroup", blankToNull(parallelGroup))
                .addValue("electiveGroup", blankToNull(electiveGroup)));
    }

    public void updateRequirementImportDetails(
            UUID id,
            String teachingGroup) {
        if (teachingGroup == null || teachingGroup.isBlank()) return;
        jdbc.update("""
                UPDATE aura_meeting_requirements SET teaching_group = :groupCode,
                    version = version + 1
                WHERE id = :id
                """, params().addValue("id", id)
                .addValue("groupCode", teachingGroup.trim()));
    }

    public void insertConflict(
            UUID termId,
            UUID leftOfferingId,
            UUID rightOfferingId,
            String source,
            String severity,
            String reason,
            UUID userId) {
        jdbc.update("""
                INSERT INTO aura_cross_offering_conflicts (
                    id, left_offering_id, right_offering_id, reason,
                    term_id, source, severity, created_by_user_id)
                VALUES (
                    :id, :leftId, :rightId, :reason, :termId,
                    :source, :severity, :userId)
                ON CONFLICT (
                    LEAST(left_offering_id, right_offering_id),
                    GREATEST(left_offering_id, right_offering_id))
                DO UPDATE SET reason = EXCLUDED.reason, source = EXCLUDED.source,
                    severity = EXCLUDED.severity, active = TRUE,
                    updated_at = CURRENT_TIMESTAMP, version = aura_cross_offering_conflicts.version + 1
                """, params().addValue("id", UUID.randomUUID())
                .addValue("leftId", leftOfferingId).addValue("rightId", rightOfferingId)
                .addValue("reason", reason).addValue("termId", termId)
                .addValue("source", source).addValue("severity", severity)
                .addValue("userId", userId));
    }

    public void insertTravelRule(
            UUID universityId,
            String from,
            String to,
            int minutes,
            String difficulty) {
        jdbc.update("""
                INSERT INTO aura_building_travel_times (
                    id, university_id, from_building, to_building,
                    minutes, difficulty)
                VALUES (:id, :universityId, :from, :to, :minutes, :difficulty)
                """, params().addValue("id", UUID.randomUUID())
                .addValue("universityId", universityId).addValue("from", from)
                .addValue("to", to).addValue("minutes", minutes)
                .addValue("difficulty", difficulty));
    }

    public void upsertTravelRule(
            UUID universityId,
            String from,
            String to,
            int minutes,
            String difficulty) {
        int updated = jdbc.update("""
                UPDATE aura_building_travel_times
                SET minutes = :minutes, difficulty = :difficulty,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE university_id = :universityId AND active = TRUE
                  AND LEAST(LOWER(BTRIM(from_building)), LOWER(BTRIM(to_building)))
                    = LEAST(LOWER(BTRIM(:from)), LOWER(BTRIM(:to)))
                  AND GREATEST(LOWER(BTRIM(from_building)), LOWER(BTRIM(to_building)))
                    = GREATEST(LOWER(BTRIM(:from)), LOWER(BTRIM(:to)))
                """, params().addValue("universityId", universityId)
                .addValue("from", from.trim()).addValue("to", to.trim())
                .addValue("minutes", minutes).addValue("difficulty", difficulty));
        if (updated == 0) {
            insertTravelRule(universityId, from.trim(), to.trim(), minutes, difficulty);
        }
    }

    public void upsertStudentAvailability(
            UUID id,
            UUID termId,
            UUID studentUserId,
            UUID timeslotId,
            String availability,
            String reason) {
        jdbc.update("""
                INSERT INTO aura_student_availability (
                    id, term_id, student_user_id, timeslot_id,
                    availability, reason)
                VALUES (
                    :id, :termId, :studentUserId, :timeslotId,
                    :availability, :reason)
                ON CONFLICT (term_id, student_user_id, timeslot_id)
                DO UPDATE SET availability = EXCLUDED.availability,
                    reason = EXCLUDED.reason,
                    updated_at = CURRENT_TIMESTAMP,
                    version = aura_student_availability.version + 1
                """, params().addValue("id", id).addValue("termId", termId)
                .addValue("studentUserId", studentUserId)
                .addValue("timeslotId", timeslotId)
                .addValue("availability", availability)
                .addValue("reason", blankToNull(reason)));
    }

    public UUID insertImportedVersion(
            UUID termId,
            int versionNumber,
            UUID userId,
            String notes) {
        UUID id = UUID.randomUUID();
        int updated = jdbc.update("""
                INSERT INTO aura_timetable_versions (
                    id, term_id, version_number, status, notes,
                    created_by_user_id, source, input_revision)
                SELECT :id, term_row.id, :versionNumber, 'DRAFT', :notes,
                    :userId, 'IMPORTED', term_row.data_revision
                FROM aura_academic_terms term_row WHERE term_row.id = :termId
                """, params().addValue("id", id).addValue("termId", termId)
                .addValue("versionNumber", versionNumber).addValue("notes", notes)
                .addValue("userId", userId));
        if (updated != 1) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                    "Imported timetable term was not found");
        }
        return id;
    }

    public void insertImportedSession(
            UUID versionId,
            UUID requirementId,
            UUID roomId,
            UUID timeslotId,
            int occurrence) {
        int updated = jdbc.update("""
                INSERT INTO aura_scheduled_sessions (
                    id, version_id, meeting_requirement_id, offering_id,
                    section_id, instructor_id, room_id, timeslot_id,
                    source, occurrence_index)
                SELECT :id, :versionId, requirement.id, offering.id,
                    offering.section_id, offering.instructor_id,
                    :roomId, :timeslotId, 'IMPORTED', :occurrence
                FROM aura_meeting_requirements requirement
                JOIN aura_course_offerings offering
                  ON offering.id = requirement.offering_id
                WHERE requirement.id = :requirementId
                """, params().addValue("id", UUID.randomUUID())
                .addValue("versionId", versionId)
                .addValue("requirementId", requirementId)
                .addValue("roomId", roomId).addValue("timeslotId", timeslotId)
                .addValue("occurrence", occurrence));
        if (updated != 1) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                    "Imported timetable reference was not found");
        }
    }

    private Optional<UUID> findId(String sql, MapSqlParameterSource values) {
        return jdbc.query(sql, values, (rs, rowNum) -> uuid(rs, "id"))
                .stream().findFirst();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ImportJob mapJob(ResultSet rs, int rowNum) throws SQLException {
        return new ImportJob(
                uuid(rs, "id"), uuid(rs, "university_id"),
                uuid(rs, "term_id"), rs.getString("import_type"),
                rs.getString("status"), readList(rs.getString("headers")),
                readMap(rs.getString("suggested_mapping")),
                readMap(rs.getString("applied_mapping")),
                rs.getInt("accepted_rows"), rs.getInt("rejected_rows"),
                nullableUuid(rs, "result_version_id"), rs.getString("message"),
                uuid(rs, "created_by_user_id"));
    }

    private Map<String, String> readMap(String value) {
        if (value == null || value.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(
                    value, new TypeReference<Map<String, String>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored import data could not be read.", exception);
        }
    }

    private List<String> readList(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(
                    value, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored import headers could not be read.", exception);
        }
    }

    private UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private UUID nullableUuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Import preview could not be serialized.", exception);
        }
    }

    private MapSqlParameterSource params() {
        return new MapSqlParameterSource();
    }

    public record ImportJob(
            UUID id,
            UUID universityId,
            UUID termId,
            String importType,
            String status,
            List<String> headers,
            Map<String, String> suggestedMapping,
            Map<String, String> appliedMapping,
            int acceptedRows,
            int rejectedRows,
            UUID resultVersionId,
            String message,
            UUID createdByUserId) {
    }

    public record SourceRow(int rowNumber, Map<String, String> values) {
    }
}
