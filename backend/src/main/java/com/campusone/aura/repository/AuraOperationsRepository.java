package com.campusone.aura.repository;

import com.campusone.aura.dto.AuraOperationsDtos;
import com.campusone.aura.dto.AuraOperationsDtos.AuditEventResponse;
import com.campusone.aura.dto.AuraOperationsDtos.AnalyticsResponse;
import com.campusone.aura.dto.AuraOperationsDtos.BuildingResponse;
import com.campusone.aura.dto.AuraOperationsDtos.MutationResponse;
import com.campusone.aura.dto.AuraOperationsDtos.OfferingConflictResponse;
import com.campusone.aura.dto.AuraOperationsDtos.StudentAvailabilityResponse;
import com.campusone.aura.dto.AuraOperationsDtos.TeachingGroupResponse;
import com.campusone.aura.dto.AuraOperationsDtos.TravelRuleResponse;
import com.campusone.aura.dto.AuraOperationsDtos.RepairImpact;
import com.campusone.aura.dto.AuraOperationsDtos.RepairMove;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraOperationsRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AuraOperationsRepository(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Optional<MutationResponse> updateTerm(
            UUID id,
            UUID universityId,
            AuraOperationsDtos.UpdateTermRequest request) {
        return mutate("""
                UPDATE aura_academic_terms
                SET code = :code, name = :name, starts_on = :startsOn,
                    ends_on = :endsOn, timezone = :timezone, status = :status,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :id AND university_id = :universityId AND version = :version
                RETURNING id, status <> 'ARCHIVED' AS active, version, updated_at
                """, params(id, universityId, request.version())
                .addValue("code", request.code().trim())
                .addValue("name", request.name().trim())
                .addValue("startsOn", request.startsOn())
                .addValue("endsOn", request.endsOn())
                .addValue("timezone", request.timezone().trim())
                .addValue("status", normalize(request.status())), "TERM");
    }

    public Optional<MutationResponse> setActiveState(
            String resourceType,
            UUID id,
            boolean active,
            long version) {
        String sql = switch (resourceType) {
            case "PROGRAM" -> activeUpdate("aura_programs", resourceType);
            case "BATCH" -> activeUpdate("aura_batches", resourceType);
            case "SECTION" -> activeUpdate("aura_sections", resourceType);
            case "INSTRUCTOR" -> activeUpdate("aura_instructors", resourceType);
            case "ROOM" -> activeUpdate("aura_rooms", resourceType);
            case "BUILDING" -> activeUpdate("aura_buildings", resourceType);
            case "TIMESLOT" -> activeUpdate("aura_timeslots", resourceType);
            case "REQUIREMENT" -> activeUpdate("aura_meeting_requirements", resourceType);
            case "OFFERING" -> """
                    UPDATE aura_course_offerings
                    SET status = CASE WHEN :active THEN 'ACTIVE' ELSE 'INACTIVE' END,
                        updated_at = CURRENT_TIMESTAMP, version = version + 1
                    WHERE id = :id AND version = :version
                    RETURNING id, :resourceType AS resource_type,
                        (status = 'ACTIVE') AS active, version, updated_at
                    """;
            default -> throw new IllegalArgumentException("Unsupported AURA resource type.");
        };
        return mutate(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("active", active)
                .addValue("version", version)
                .addValue("resourceType", resourceType), resourceType);
    }

    public Optional<MutationResponse> updateProgram(
            UUID id,
            UUID universityId,
            AuraOperationsDtos.UpdateProgramRequest request) {
        return mutate("""
                UPDATE aura_programs
                SET department_id = :departmentId, code = :code, name = :name,
                    number_of_semesters = :numberOfSemesters, active = :active,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :id AND university_id = :universityId AND version = :version
                RETURNING id, active, version, updated_at
                """, params(id, universityId, request.version())
                .addValue("departmentId", request.departmentId())
                .addValue("code", request.code().trim())
                .addValue("name", request.name().trim())
                .addValue("numberOfSemesters", request.numberOfSemesters())
                .addValue("active", request.active()), "PROGRAM");
    }

    public Optional<MutationResponse> updateBatch(
            UUID id,
            UUID universityId,
            AuraOperationsDtos.UpdateBatchRequest request) {
        return mutate("""
                UPDATE aura_batches batch
                SET code = :code, admission_year = :admissionYear,
                    expected_graduation_year = :expectedGraduationYear,
                    active = :active, updated_at = CURRENT_TIMESTAMP,
                    version = batch.version + 1
                FROM aura_programs program
                WHERE batch.id = :id AND batch.program_id = program.id
                  AND program.university_id = :universityId AND batch.version = :version
                RETURNING batch.id, batch.active, batch.version, batch.updated_at
                """, params(id, universityId, request.version())
                .addValue("code", request.code().trim())
                .addValue("admissionYear", request.admissionYear())
                .addValue("expectedGraduationYear", request.expectedGraduationYear())
                .addValue("active", request.active()), "BATCH");
    }

    public Optional<MutationResponse> updateSection(
            UUID id,
            UUID universityId,
            AuraOperationsDtos.UpdateSectionRequest request) {
        return mutate("""
                UPDATE aura_sections section_row
                SET batch_id = :batchId, term_id = :termId, code = :code,
                    display_name = :displayName, student_count = :studentCount,
                    semester_number = :semesterNumber, hard_daily_load = :hardDailyLoad,
                    preferred_daily_load = :preferredDailyLoad,
                    home_building_id = :homeBuildingId,
                    home_building = building.name, active = :active,
                    updated_at = CURRENT_TIMESTAMP, version = section_row.version + 1
                FROM aura_batches batch
                JOIN aura_programs program ON program.id = batch.program_id
                LEFT JOIN aura_buildings building ON building.id = :homeBuildingId
                WHERE section_row.id = :id AND batch.id = :batchId
                  AND program.university_id = :universityId
                  AND (building.id IS NULL OR building.university_id = :universityId)
                  AND section_row.version = :version
                RETURNING section_row.id, section_row.active,
                    section_row.version, section_row.updated_at
                """, params(id, universityId, request.version())
                .addValue("batchId", request.batchId())
                .addValue("termId", request.termId())
                .addValue("code", request.code().trim())
                .addValue("displayName", request.displayName().trim())
                .addValue("studentCount", request.studentCount())
                .addValue("semesterNumber", request.semesterNumber())
                .addValue("hardDailyLoad", request.hardDailyLoad())
                .addValue("preferredDailyLoad", request.preferredDailyLoad())
                .addValue("homeBuildingId", request.homeBuildingId())
                .addValue("active", request.active()), "SECTION");
    }

    public Optional<MutationResponse> updateInstructor(
            UUID id,
            UUID universityId,
            AuraOperationsDtos.UpdateInstructorRequest request) {
        return mutate("""
                UPDATE aura_instructors instructor
                SET user_id = :userId, department_id = :departmentId,
                    employee_code = NULLIF(BTRIM(:employeeCode), ''),
                    display_name = :displayName, email = NULLIF(BTRIM(:email), ''),
                    max_hours_per_week = :maxHoursPerWeek,
                    hard_daily_load = :hardDailyLoad,
                    preferred_weekly_load = :preferredWeeklyLoad,
                    preferred_daily_load = :preferredDailyLoad,
                    maximum_consecutive_slots = :maximumConsecutiveSlots,
                    home_building_id = :homeBuildingId,
                    home_building = (SELECT name FROM aura_buildings
                        WHERE id = :homeBuildingId), active = :active,
                    updated_at = CURRENT_TIMESTAMP, version = instructor.version + 1
                WHERE instructor.id = :id AND instructor.university_id = :universityId
                  AND (:homeBuildingId IS NULL OR EXISTS (
                      SELECT 1 FROM aura_buildings building
                      WHERE building.id = :homeBuildingId
                        AND building.university_id = :universityId))
                  AND instructor.version = :version
                RETURNING instructor.id, instructor.active,
                    instructor.version, instructor.updated_at
                """, params(id, universityId, request.version())
                .addValue("userId", request.userId())
                .addValue("departmentId", request.departmentId())
                .addValue("employeeCode", request.employeeCode())
                .addValue("displayName", request.displayName().trim())
                .addValue("email", request.email())
                .addValue("maxHoursPerWeek", request.maxHoursPerWeek())
                .addValue("hardDailyLoad", request.hardDailyLoad())
                .addValue("preferredWeeklyLoad", request.preferredWeeklyLoad())
                .addValue("preferredDailyLoad", request.preferredDailyLoad())
                .addValue("maximumConsecutiveSlots", request.maximumConsecutiveSlots())
                .addValue("homeBuildingId", request.homeBuildingId())
                .addValue("active", request.active()), "INSTRUCTOR");
    }

    public Optional<MutationResponse> updateRoom(
            UUID id,
            UUID universityId,
            AuraOperationsDtos.UpdateRoomRequest request) {
        return mutate("""
                UPDATE aura_rooms room
                SET building_id = :buildingId,
                    building = (SELECT name FROM aura_buildings WHERE id = :buildingId),
                    room_code = NULLIF(BTRIM(:roomCode), ''), name = :name,
                    display_name = :displayName, capacity = :capacity,
                    room_type = :roomType, active = :active,
                    updated_at = CURRENT_TIMESTAMP, version = room.version + 1
                WHERE room.id = :id AND room.university_id = :universityId
                  AND (:buildingId IS NULL OR EXISTS (
                      SELECT 1 FROM aura_buildings building
                      WHERE building.id = :buildingId
                        AND building.university_id = :universityId))
                  AND room.version = :version
                RETURNING room.id, room.active, room.version, room.updated_at
                """, params(id, universityId, request.version())
                .addValue("buildingId", request.buildingId())
                .addValue("roomCode", request.roomCode())
                .addValue("name", request.name().trim())
                .addValue("displayName", request.displayName().trim())
                .addValue("capacity", request.capacity())
                .addValue("roomType", normalize(request.roomType()))
                .addValue("active", request.active()), "ROOM");
    }

    public Optional<MutationResponse> updateTimeslot(
            UUID id,
            UUID universityId,
            AuraOperationsDtos.UpdateTimeslotRequest request) {
        return mutate("""
                UPDATE aura_timeslots
                SET term_id = :termId, day_of_week = :dayOfWeek,
                    starts_at = :startsAt, ends_at = :endsAt, label = :label,
                    slot_order = :slotOrder, slot_type = :slotType,
                    active = :active, updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :id AND university_id = :universityId AND version = :version
                  AND (:termId IS NULL OR EXISTS (
                      SELECT 1 FROM aura_academic_terms term_row
                      WHERE term_row.id = :termId
                        AND term_row.university_id = :universityId))
                RETURNING id, active, version, updated_at
                """, params(id, universityId, request.version())
                .addValue("termId", request.termId())
                .addValue("dayOfWeek", request.dayOfWeek())
                .addValue("startsAt", request.startsAt())
                .addValue("endsAt", request.endsAt())
                .addValue("label", request.label().trim())
                .addValue("slotOrder", request.slotOrder())
                .addValue("slotType", normalize(request.slotType()))
                .addValue("active", request.active()), "TIMESLOT");
    }

    public Optional<MutationResponse> updateOffering(
            UUID id,
            UUID universityId,
            AuraOperationsDtos.UpdateOfferingRequest request) {
        return mutate("""
                UPDATE aura_course_offerings offering
                SET course_id = :courseId, section_id = :sectionId,
                    instructor_id = :instructorId,
                    offering_code = NULLIF(BTRIM(:offeringCode), ''),
                    expected_students = :expectedStudents,
                    maximum_enrollment = :maximumEnrollment,
                    combined_sections = :combinedSections,
                    parallel_group = NULLIF(BTRIM(:parallelGroup), ''),
                    elective_group = NULLIF(BTRIM(:electiveGroup), ''),
                    notes = NULLIF(BTRIM(:notes), ''), status = :status,
                    updated_at = CURRENT_TIMESTAMP, version = offering.version + 1
                FROM aura_academic_terms term_row
                WHERE offering.id = :id AND offering.term_id = term_row.id
                  AND term_row.university_id = :universityId
                  AND offering.version = :version
                RETURNING offering.id, offering.status = 'ACTIVE' AS active,
                    offering.version, offering.updated_at
                """, params(id, universityId, request.version())
                .addValue("courseId", request.courseId())
                .addValue("sectionId", request.sectionId())
                .addValue("instructorId", request.instructorId())
                .addValue("offeringCode", request.offeringCode())
                .addValue("expectedStudents", request.expectedStudents())
                .addValue("maximumEnrollment", request.maximumEnrollment())
                .addValue("combinedSections", request.combinedSections())
                .addValue("parallelGroup", request.parallelGroup())
                .addValue("electiveGroup", request.electiveGroup())
                .addValue("notes", request.notes())
                .addValue("status", normalize(request.status())), "OFFERING");
    }

    public Optional<MutationResponse> updateRequirement(
            UUID id,
            UUID universityId,
            AuraOperationsDtos.UpdateMeetingRequirementRequest request) {
        return mutate("""
                UPDATE aura_meeting_requirements requirement
                SET meeting_type = :meetingType,
                    sessions_per_week = :sessionsPerWeek,
                    duration_slots = :durationSlots, room_type = :roomType,
                    required_capacity = :requiredCapacity,
                    notes = NULLIF(BTRIM(:notes), ''),
                    allowed_days = CAST(:allowedDays AS SMALLINT[]),
                    prohibited_days = CAST(:prohibitedDays AS SMALLINT[]),
                    preferred_days = CAST(:preferredDays AS SMALLINT[]),
                    preferred_start_time = :preferredStartTime,
                    preferred_end_time = :preferredEndTime,
                    minimum_day_separation = :minimumDaySeparation,
                    maximum_occurrences_per_day = :maximumOccurrencesPerDay,
                    same_room_preferred = :sameRoomPreferred,
                    fixed_room_id = :fixedRoomId,
                    fixed_timeslot_id = :fixedTimeslotId,
                    pinned = :pinned, teaching_group_id = :teachingGroupId,
                    teaching_group = teaching_group.code,
                    linked_requirement_id = :linkedRequirementId,
                    lecture_before_linked = :lectureBeforeLinked,
                    week_pattern = :weekPattern,
                    custom_weeks = CAST(:customWeeks AS SMALLINT[]),
                    active = :active, version = requirement.version + 1
                FROM aura_course_offerings offering
                LEFT JOIN aura_teaching_groups teaching_group
                  ON teaching_group.id = :teachingGroupId
                JOIN aura_academic_terms term_row ON term_row.id = offering.term_id
                WHERE requirement.id = :id
                  AND requirement.offering_id = offering.id
                  AND term_row.university_id = :universityId
                  AND (:teachingGroupId IS NULL
                    OR teaching_group.offering_id = offering.id)
                  AND requirement.version = :version
                RETURNING requirement.id, requirement.active,
                    requirement.version, CURRENT_TIMESTAMP AS updated_at
                """, params(id, universityId, request.version())
                .addValue("meetingType", normalize(request.meetingType()))
                .addValue("sessionsPerWeek", request.sessionsPerWeek())
                .addValue("durationSlots", request.durationSlots())
                .addValue("roomType", normalize(request.roomType()))
                .addValue("requiredCapacity", request.requiredCapacity())
                .addValue("notes", request.notes())
                .addValue("allowedDays", request.allowedDays() == null ? null
                        : request.allowedDays().toArray(Integer[]::new))
                .addValue("prohibitedDays", request.prohibitedDays() == null ? null
                        : request.prohibitedDays().toArray(Integer[]::new))
                .addValue("preferredDays", request.preferredDays() == null ? null
                        : request.preferredDays().toArray(Integer[]::new))
                .addValue("preferredStartTime", request.preferredStartTime())
                .addValue("preferredEndTime", request.preferredEndTime())
                .addValue("minimumDaySeparation", request.minimumDaySeparation())
                .addValue("maximumOccurrencesPerDay", request.maximumOccurrencesPerDay())
                .addValue("sameRoomPreferred", request.sameRoomPreferred())
                .addValue("fixedRoomId", request.fixedRoomId())
                .addValue("fixedTimeslotId", request.fixedTimeslotId())
                .addValue("pinned", request.pinned())
                .addValue("teachingGroupId", request.teachingGroupId())
                .addValue("linkedRequirementId", request.linkedRequirementId())
                .addValue("lectureBeforeLinked", request.lectureBeforeLinked())
                .addValue("weekPattern", normalize(request.weekPattern()))
                .addValue("customWeeks", request.customWeeks() == null ? null
                        : request.customWeeks().toArray(Integer[]::new))
                .addValue("active", request.active()), "MEETING_REQUIREMENT");
    }

    public UUID insertBuilding(
            UUID universityId,
            AuraOperationsDtos.CreateBuildingRequest request) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO aura_buildings (
                    id, university_id, code, name, minimum_transition_minutes
                ) VALUES (:id, :universityId, :code, :name, :minutes)
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("universityId", universityId)
                .addValue("code", request.code().trim())
                .addValue("name", request.name().trim())
                .addValue("minutes", request.minimumTransitionMinutes()));
        return id;
    }

    public Optional<MutationResponse> updateBuilding(
            UUID id,
            UUID universityId,
            AuraOperationsDtos.UpdateBuildingRequest request) {
        return mutate("""
                UPDATE aura_buildings
                SET code = :code, name = :name,
                    minimum_transition_minutes = :minutes, active = :active,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :id AND university_id = :universityId AND version = :version
                RETURNING id, active, version, updated_at
                """, params(id, universityId, request.version())
                .addValue("code", request.code().trim())
                .addValue("name", request.name().trim())
                .addValue("minutes", request.minimumTransitionMinutes())
                .addValue("active", request.active()), "BUILDING");
    }

    public List<BuildingResponse> listBuildings(UUID universityId) {
        return jdbc.query("""
                SELECT id, code, name, minimum_transition_minutes,
                    active, version
                FROM aura_buildings WHERE university_id = :universityId
                ORDER BY active DESC, LOWER(name)
                """, new MapSqlParameterSource("universityId", universityId),
                (rs, row) -> new BuildingResponse(
                        rs.getObject("id", UUID.class), rs.getString("code"),
                        rs.getString("name"), rs.getInt("minimum_transition_minutes"),
                        rs.getBoolean("active"), rs.getLong("version")));
    }

    public UUID insertTeachingGroup(AuraOperationsDtos.UpsertTeachingGroupRequest request) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO aura_teaching_groups (
                    id, offering_id, group_type, code, display_name, capacity, active
                ) VALUES (:id, :offeringId, :groupType, :code, :displayName,
                    :capacity, :active)
                """, groupParams(id, request));
        return id;
    }

    public boolean updateTeachingGroup(
            UUID id,
            AuraOperationsDtos.UpsertTeachingGroupRequest request) {
        return jdbc.update("""
                UPDATE aura_teaching_groups
                SET group_type = :groupType, code = :code,
                    display_name = :displayName, capacity = :capacity,
                    active = :active, updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :id AND offering_id = :offeringId AND version = :version
                """, groupParams(id, request)
                .addValue("version", request.version())) == 1;
    }

    public List<TeachingGroupResponse> listTeachingGroups(UUID termId) {
        return jdbc.query("""
                SELECT group_row.*
                FROM aura_teaching_groups group_row
                JOIN aura_course_offerings offering ON offering.id = group_row.offering_id
                WHERE offering.term_id = :termId
                ORDER BY offering.id, group_row.group_type, LOWER(group_row.code)
                """, new MapSqlParameterSource("termId", termId),
                (rs, row) -> new TeachingGroupResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("offering_id", UUID.class),
                        rs.getString("group_type"), rs.getString("code"),
                        rs.getString("display_name"), nullableInt(rs, "capacity"),
                        rs.getBoolean("active"), rs.getLong("version")));
    }

    public StudentAvailabilityResponse upsertStudentAvailability(
            AuraOperationsDtos.UpsertStudentAvailabilityRequest request) {
        return jdbc.queryForObject("""
                INSERT INTO aura_student_availability (
                    id, term_id, student_user_id, timeslot_id,
                    availability, reason
                ) VALUES (:id, :termId, :studentUserId, :timeslotId,
                    :availability, NULLIF(BTRIM(:reason), ''))
                ON CONFLICT (term_id, student_user_id, timeslot_id)
                DO UPDATE SET availability = EXCLUDED.availability,
                    reason = EXCLUDED.reason, updated_at = CURRENT_TIMESTAMP,
                    version = aura_student_availability.version + 1
                RETURNING *
                """, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("termId", request.termId())
                .addValue("studentUserId", request.studentUserId())
                .addValue("timeslotId", request.timeslotId())
                .addValue("availability", normalize(request.availability()))
                .addValue("reason", request.reason()),
                (rs, row) -> studentAvailability(rs));
    }

    public List<StudentAvailabilityResponse> listStudentAvailability(
            UUID termId,
            UUID studentUserId) {
        return jdbc.query("""
                SELECT * FROM aura_student_availability
                WHERE term_id = :termId
                  AND (CAST(:studentUserId AS UUID) IS NULL
                    OR student_user_id = :studentUserId)
                ORDER BY student_user_id, timeslot_id
                """, new MapSqlParameterSource("termId", termId)
                .addValue("studentUserId", studentUserId),
                (rs, row) -> studentAvailability(rs));
    }

    public UUID insertOfferingConflict(
            UUID actorId,
            AuraOperationsDtos.UpsertOfferingConflictRequest request) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO aura_cross_offering_conflicts (
                    id, term_id, left_offering_id, right_offering_id,
                    source, severity, reason, active, created_by_user_id
                ) VALUES (:id, :termId, :leftOfferingId, :rightOfferingId,
                    :source, :severity, :reason, :active, :actorId)
                """, conflictParams(id, request).addValue("actorId", actorId));
        return id;
    }

    public boolean updateOfferingConflict(
            UUID id,
            AuraOperationsDtos.UpsertOfferingConflictRequest request) {
        return jdbc.update("""
                UPDATE aura_cross_offering_conflicts
                SET left_offering_id = :leftOfferingId,
                    right_offering_id = :rightOfferingId, source = :source,
                    severity = :severity, reason = :reason, active = :active,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :id AND term_id = :termId AND version = :version
                """, conflictParams(id, request)
                .addValue("version", request.version())) == 1;
    }

    public List<OfferingConflictResponse> listOfferingConflicts(UUID termId) {
        return jdbc.query("""
                SELECT * FROM aura_cross_offering_conflicts
                WHERE term_id = :termId
                ORDER BY active DESC, created_at DESC
                """, new MapSqlParameterSource("termId", termId),
                (rs, row) -> new OfferingConflictResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("term_id", UUID.class),
                        rs.getObject("left_offering_id", UUID.class),
                        rs.getObject("right_offering_id", UUID.class),
                        rs.getString("source"), rs.getString("severity"),
                        rs.getString("reason"), rs.getBoolean("active"),
                        rs.getLong("version")));
    }

    public UUID insertTravelRule(
            UUID universityId,
            AuraOperationsDtos.UpsertTravelRuleRequest request) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO aura_building_travel_times (
                    id, university_id, from_building, to_building,
                    minutes, difficulty, active
                ) VALUES (:id, :universityId, :fromBuilding, :toBuilding,
                    :minutes, :difficulty, :active)
                """, travelParams(id, universityId, request));
        return id;
    }

    public boolean updateTravelRule(
            UUID id,
            UUID universityId,
            AuraOperationsDtos.UpsertTravelRuleRequest request) {
        return jdbc.update("""
                UPDATE aura_building_travel_times
                SET from_building = :fromBuilding, to_building = :toBuilding,
                    minutes = :minutes, difficulty = :difficulty,
                    active = :active, updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :id AND university_id = :universityId
                  AND version = :version
                """, travelParams(id, universityId, request)
                .addValue("version", request.version())) == 1;
    }

    public List<TravelRuleResponse> listTravelRules(UUID universityId) {
        return jdbc.query("""
                SELECT * FROM aura_building_travel_times
                WHERE university_id = :universityId
                ORDER BY active DESC, LOWER(from_building), LOWER(to_building)
                """, new MapSqlParameterSource("universityId", universityId),
                (rs, row) -> new TravelRuleResponse(
                        rs.getObject("id", UUID.class),
                        rs.getString("from_building"), rs.getString("to_building"),
                        rs.getInt("minutes"), rs.getString("difficulty"),
                        rs.getBoolean("active"), rs.getLong("version")));
    }

    public List<AuditEventResponse> listAudit(
            UUID universityId,
            UUID termId,
            String action,
            String targetType,
            Instant from,
            Instant to,
            int limit,
            int offset) {
        return jdbc.query("""
                SELECT audit.*, COALESCE(profile.full_name, user_row.email) AS actor_name
                FROM aura_audit_events audit
                JOIN users user_row ON user_row.id = audit.actor_user_id
                LEFT JOIN student_profiles profile ON profile.user_id = user_row.id
                WHERE audit.university_id = :universityId
                  AND (CAST(:termId AS UUID) IS NULL OR audit.term_id = :termId)
                  AND (CAST(:action AS VARCHAR) IS NULL OR audit.action = :action)
                  AND (CAST(:targetType AS VARCHAR) IS NULL OR audit.target_type = :targetType)
                  AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR audit.created_at >= :from)
                  AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR audit.created_at <= :to)
                ORDER BY audit.created_at DESC, audit.id
                LIMIT :limit OFFSET :offset
                """, new MapSqlParameterSource()
                .addValue("universityId", universityId).addValue("termId", termId)
                .addValue("action", blank(action)).addValue("targetType", blank(targetType))
                .addValue("from", from == null ? null : Timestamp.from(from))
                .addValue("to", to == null ? null : Timestamp.from(to))
                .addValue("limit", limit).addValue("offset", offset),
                (rs, row) -> new AuditEventResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("term_id", UUID.class),
                        rs.getObject("actor_user_id", UUID.class),
                        rs.getString("actor_name"), rs.getString("action"),
                        rs.getString("target_type"),
                        rs.getObject("target_id", UUID.class), rs.getString("summary"),
                        jsonMap(rs.getString("metadata")),
                        rs.getObject("correlation_id", UUID.class), rs.getString("result"),
                        rs.getTimestamp("created_at").toInstant()));
    }

    public AnalyticsResponse analytics(UUID termId, UUID versionId) {
        Map<String, Double> roomUtilization = doubleMetrics("""
                SELECT room.display_name AS label,
                    CASE WHEN COUNT(DISTINCT slot.id) = 0 THEN 0.0
                      ELSE 100.0 * COUNT(DISTINCT session_row.id)
                        / COUNT(DISTINCT slot.id) END AS metric
                FROM aura_rooms room
                JOIN aura_academic_terms term_row
                  ON term_row.university_id = room.university_id
                 AND term_row.id = :termId
                LEFT JOIN aura_timeslots slot
                  ON slot.university_id = room.university_id
                 AND (slot.term_id IS NULL OR slot.term_id = term_row.id)
                 AND slot.active = TRUE AND slot.slot_type = 'INSTRUCTIONAL'
                LEFT JOIN aura_scheduled_sessions session_row
                  ON session_row.room_id = room.id
                 AND session_row.timeslot_id = slot.id
                 AND session_row.version_id = :versionId
                WHERE room.active = TRUE
                GROUP BY room.id, room.display_name
                ORDER BY room.display_name
                """, termId, versionId);
        Map<String, Double> buildingUtilization = doubleMetrics("""
                SELECT COALESCE(building.name, room.building, 'Unassigned') AS label,
                    CASE WHEN COUNT(DISTINCT room.id) * COUNT(DISTINCT slot.id) = 0
                      THEN 0.0 ELSE 100.0 * COUNT(DISTINCT session_row.id)
                        / (COUNT(DISTINCT room.id) * COUNT(DISTINCT slot.id)) END AS metric
                FROM aura_rooms room
                JOIN aura_academic_terms term_row
                  ON term_row.university_id = room.university_id
                 AND term_row.id = :termId
                LEFT JOIN aura_buildings building ON building.id = room.building_id
                LEFT JOIN aura_timeslots slot
                  ON slot.university_id = room.university_id
                 AND (slot.term_id IS NULL OR slot.term_id = term_row.id)
                 AND slot.active = TRUE AND slot.slot_type = 'INSTRUCTIONAL'
                LEFT JOIN aura_scheduled_sessions session_row
                  ON session_row.room_id = room.id
                 AND session_row.timeslot_id = slot.id
                 AND session_row.version_id = :versionId
                WHERE room.active = TRUE
                GROUP BY COALESCE(building.name, room.building, 'Unassigned')
                ORDER BY label
                """, termId, versionId);
        Map<String, Integer> instructorLoads = integerMetrics("""
                SELECT instructor.display_name AS label,
                    COUNT(session_row.id)::INTEGER AS metric
                FROM aura_instructors instructor
                JOIN aura_academic_terms term_row
                  ON term_row.university_id = instructor.university_id
                 AND term_row.id = :termId
                LEFT JOIN aura_scheduled_sessions session_row
                  ON session_row.instructor_id = instructor.id
                 AND session_row.version_id = :versionId
                WHERE instructor.active = TRUE
                GROUP BY instructor.id, instructor.display_name
                ORDER BY instructor.display_name
                """, termId, versionId);
        Map<String, Integer> sectionLoads = integerMetrics("""
                SELECT section_row.display_name AS label,
                    COUNT(session_row.id)::INTEGER AS metric
                FROM aura_sections section_row
                LEFT JOIN aura_scheduled_sessions session_row
                  ON session_row.section_id = section_row.id
                 AND session_row.version_id = :versionId
                WHERE section_row.term_id = :termId AND section_row.active = TRUE
                GROUP BY section_row.id, section_row.display_name
                ORDER BY section_row.display_name
                """, termId, versionId);
        Map<String, Long> clashes = longMetrics("""
                SELECT clash_type AS label, COUNT(*) AS metric
                FROM aura_clashes WHERE version_id = :versionId
                GROUP BY clash_type ORDER BY clash_type
                """, termId, versionId);
        Map<String, Long> sessionsByDay = longMetrics("""
                SELECT timeslot.day_of_week::TEXT AS label, COUNT(*) AS metric
                FROM aura_scheduled_sessions session_row
                JOIN aura_timeslots timeslot ON timeslot.id = session_row.timeslot_id
                WHERE session_row.version_id = :versionId
                GROUP BY timeslot.day_of_week ORDER BY timeslot.day_of_week
                """, termId, versionId);
        Map<String, Number> totals = jdbc.queryForObject("""
                SELECT COALESCE(AVG(
                    100.0 * requirement.required_capacity / NULLIF(room.capacity, 0)), 0) AS capacity,
                  (SELECT COUNT(*) FROM aura_clashes
                    WHERE version_id = :versionId AND resolved_at IS NULL) AS unresolved,
                  (SELECT COUNT(*) FROM aura_meeting_requirements requirement_row
                    JOIN aura_course_offerings offering
                      ON offering.id = requirement_row.offering_id
                    WHERE offering.term_id = :termId AND offering.status = 'ACTIVE'
                      AND requirement_row.active = TRUE
                      AND NOT EXISTS (
                        SELECT 1 FROM aura_rooms candidate
                        WHERE candidate.university_id = (
                            SELECT university_id FROM aura_academic_terms WHERE id = :termId)
                          AND candidate.active = TRUE
                          AND candidate.capacity >= requirement_row.required_capacity
                          AND candidate.room_type = requirement_row.room_type)) AS impossible,
                  (SELECT COUNT(*) FROM aura_repair_plans WHERE term_id = :termId) AS repairs,
                  (SELECT COALESCE(AVG((impact->>'disruptionScore')::DOUBLE PRECISION), 0)
                    FROM aura_repair_plans WHERE term_id = :termId
                      AND jsonb_exists(impact, 'disruptionScore')) AS disruption
                FROM aura_scheduled_sessions session_row
                JOIN aura_meeting_requirements requirement
                  ON requirement.id = session_row.meeting_requirement_id
                JOIN aura_rooms room ON room.id = session_row.room_id
                WHERE session_row.version_id = :versionId
                """, metricParams(termId, versionId), (rs, row) -> Map.of(
                        "capacity", rs.getDouble("capacity"),
                        "unresolved", rs.getLong("unresolved"),
                        "impossible", rs.getLong("impossible"),
                        "repairs", rs.getLong("repairs"),
                        "disruption", rs.getDouble("disruption")));
        return new AnalyticsResponse(
                termId, versionId, roomUtilization, buildingUtilization,
                instructorLoads, sectionLoads, clashes, sessionsByDay,
                totals.get("capacity").doubleValue(), totals.get("unresolved").longValue(),
                totals.get("impossible").longValue(), totals.get("repairs").longValue(),
                totals.get("disruption").doubleValue());
    }

    public Optional<RepairSourceState> repairSourceState(UUID versionId) {
        return jdbc.query("""
                SELECT timetable_version.id, timetable_version.term_id,
                    term_row.university_id, timetable_version.status,
                    timetable_version.version AS version_revision,
                    timetable_version.input_revision,
                    term_row.data_revision AS current_input_revision
                FROM aura_timetable_versions timetable_version
                JOIN aura_academic_terms term_row
                  ON term_row.id = timetable_version.term_id
                WHERE timetable_version.id = :versionId
                """, new MapSqlParameterSource("versionId", versionId),
                (rs, row) -> new RepairSourceState(
                        rs.getObject("id", UUID.class),
                        rs.getObject("term_id", UUID.class),
                        rs.getObject("university_id", UUID.class),
                        rs.getString("status"), rs.getLong("version_revision"),
                        rs.getLong("input_revision"),
                        rs.getLong("current_input_revision"))).stream().findFirst();
    }

    public Optional<UUID> clashSession(UUID clashId, UUID versionId) {
        return jdbc.query("""
                SELECT COALESCE(primary_session_id, secondary_session_id)
                FROM aura_clashes
                WHERE id = :clashId AND version_id = :versionId
                  AND status IN ('OPEN', 'ACKNOWLEDGED')
                """, new MapSqlParameterSource("clashId", clashId)
                .addValue("versionId", versionId),
                (rs, row) -> rs.getObject(1, UUID.class)).stream().findFirst();
    }

    public Optional<UUID> correspondingSession(UUID sourceSessionId, UUID draftVersionId) {
        return jdbc.query("""
                SELECT clone.id
                FROM aura_scheduled_sessions source
                JOIN aura_scheduled_sessions clone
                  ON clone.meeting_requirement_id = source.meeting_requirement_id
                 AND clone.occurrence_index = source.occurrence_index
                WHERE source.id = :sourceSessionId
                  AND clone.version_id = :draftVersionId
                """, new MapSqlParameterSource("sourceSessionId", sourceSessionId)
                .addValue("draftVersionId", draftVersionId),
                (rs, row) -> rs.getObject(1, UUID.class)).stream().findFirst();
    }

    public int activeStudentsForOffering(UUID offeringId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT student_user_id)::INTEGER
                FROM aura_student_course_registrations
                WHERE offering_id = :offeringId AND status = 'ACTIVE'
                """, new MapSqlParameterSource("offeringId", offeringId), Integer.class);
        return count == null ? 0 : count;
    }

    public UUID insertRepairPlan(
            RepairSourceState source,
            UUID draftVersionId,
            UUID actorId,
            String triggerType,
            UUID triggerId,
            List<RepairMove> moves,
            RepairImpact impact,
            String previewHash,
            String reason,
            Instant expiresAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO aura_repair_plans (
                    id, university_id, term_id, source_version_id,
                    draft_version_id, requested_by_user_id, trigger_type,
                    trigger_id, input_revision, source_version_revision,
                    proposed_moves, impact, preview_hash, reason, expires_at
                ) VALUES (
                    :id, :universityId, :termId, :sourceVersionId,
                    :draftVersionId, :actorId, :triggerType, :triggerId,
                    :inputRevision, :sourceVersionRevision,
                    CAST(:moves AS JSONB), CAST(:impact AS JSONB),
                    :previewHash, :reason, :expiresAt)
                """, new MapSqlParameterSource("id", id)
                .addValue("universityId", source.universityId())
                .addValue("termId", source.termId())
                .addValue("sourceVersionId", source.versionId())
                .addValue("draftVersionId", draftVersionId)
                .addValue("actorId", actorId).addValue("triggerType", triggerType)
                .addValue("triggerId", triggerId)
                .addValue("inputRevision", source.currentInputRevision())
                .addValue("sourceVersionRevision", source.versionRevision())
                .addValue("moves", writeJson(Map.of("moves", moves)))
                .addValue("impact", writeJson(impact))
                .addValue("previewHash", previewHash).addValue("reason", reason.trim())
                .addValue("expiresAt", Timestamp.from(expiresAt)));
        return id;
    }

    public Optional<RepairPlanRow> lockRepairPlan(UUID planId, UUID universityId) {
        return jdbc.query("""
                SELECT plan.*, source.version AS current_source_version,
                    term_row.data_revision AS current_input_revision
                FROM aura_repair_plans plan
                JOIN aura_timetable_versions source ON source.id = plan.source_version_id
                JOIN aura_academic_terms term_row ON term_row.id = plan.term_id
                WHERE plan.id = :planId AND plan.university_id = :universityId
                FOR UPDATE OF plan
                """, new MapSqlParameterSource("planId", planId)
                .addValue("universityId", universityId), this::mapRepairPlan)
                .stream().findFirst();
    }

    public void markRepairApplied(UUID planId) {
        jdbc.update("""
                UPDATE aura_repair_plans
                SET status = 'APPLIED', applied_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :planId AND status = 'PREVIEWED'
                """, new MapSqlParameterSource("planId", planId));
    }

    public Optional<RepairPlanRow> findRepairPlan(UUID planId, UUID universityId) {
        return jdbc.query("""
                SELECT plan.*, source.version AS current_source_version,
                    term_row.data_revision AS current_input_revision
                FROM aura_repair_plans plan
                JOIN aura_timetable_versions source ON source.id = plan.source_version_id
                JOIN aura_academic_terms term_row ON term_row.id = plan.term_id
                WHERE plan.id = :planId AND plan.university_id = :universityId
                """, new MapSqlParameterSource("planId", planId)
                .addValue("universityId", universityId), this::mapRepairPlan)
                .stream().findFirst();
    }

    public void insertAudit(
            UUID universityId,
            UUID termId,
            UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            String summary,
            UUID correlationId,
            String result,
            Map<String, ?> metadata) {
        jdbc.update("""
                INSERT INTO aura_audit_events (
                    id, university_id, term_id, actor_user_id, action,
                    target_type, target_id, summary, metadata,
                    correlation_id, result
                ) VALUES (:id, :universityId, :termId, :actorId, :action,
                    :targetType, :targetId, :summary, CAST(:metadata AS JSONB),
                    :correlationId, :result)
                """, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID()).addValue("universityId", universityId)
                .addValue("termId", termId).addValue("actorId", actorId)
                .addValue("action", action).addValue("targetType", targetType)
                .addValue("targetId", targetId).addValue("summary", summary)
                .addValue("metadata", writeJson(metadata))
                .addValue("correlationId", correlationId).addValue("result", result));
    }

    public Optional<UUID> termForResource(String resourceType, UUID resourceId) {
        String sql = switch (resourceType) {
            case "TERM" -> "SELECT id FROM aura_academic_terms WHERE id = :id";
            case "SECTION" -> "SELECT term_id FROM aura_sections WHERE id = :id";
            case "TIMESLOT" -> "SELECT term_id FROM aura_timeslots WHERE id = :id";
            case "OFFERING" -> "SELECT term_id FROM aura_course_offerings WHERE id = :id";
            case "MEETING_REQUIREMENT" -> """
                    SELECT offering.term_id FROM aura_meeting_requirements requirement
                    JOIN aura_course_offerings offering ON offering.id = requirement.offering_id
                    WHERE requirement.id = :id
                    """;
            default -> null;
        };
        if (sql == null) return Optional.empty();
        List<UUID> rows = jdbc.query(sql, new MapSqlParameterSource("id", resourceId),
                (rs, row) -> rs.getObject(1, UUID.class));
        return rows.stream().filter(java.util.Objects::nonNull).findFirst();
    }

    private Optional<MutationResponse> mutate(
            String sql,
            MapSqlParameterSource parameters,
            String resourceType) {
        return jdbc.query(sql, parameters, (rs, row) -> new MutationResponse(
                rs.getObject("id", UUID.class), resourceType,
                rs.getBoolean("active"), rs.getLong("version"),
                rs.getTimestamp("updated_at").toInstant())).stream().findFirst();
    }

    private String activeUpdate(String table, String resourceType) {
        return "UPDATE " + table + " "
                + "SET active = :active, updated_at = CURRENT_TIMESTAMP, "
                + "version = version + 1 WHERE id = :id AND version = :version "
                + "RETURNING id, '" + resourceType + "' AS resource_type, "
                + "active, version, updated_at";
    }

    private Map<String, Double> doubleMetrics(String sql, UUID termId, UUID versionId) {
        Map<String, Double> result = new LinkedHashMap<>();
        jdbc.query(sql, metricParams(termId, versionId), rs -> {
            result.put(rs.getString("label"), rs.getDouble("metric"));
        });
        return Map.copyOf(result);
    }

    private Map<String, Integer> integerMetrics(String sql, UUID termId, UUID versionId) {
        Map<String, Integer> result = new LinkedHashMap<>();
        jdbc.query(sql, metricParams(termId, versionId), rs -> {
            result.put(rs.getString("label"), rs.getInt("metric"));
        });
        return Map.copyOf(result);
    }

    private Map<String, Long> longMetrics(String sql, UUID termId, UUID versionId) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.query(sql, metricParams(termId, versionId), rs -> {
            result.put(rs.getString("label"), rs.getLong("metric"));
        });
        return Map.copyOf(result);
    }

    private MapSqlParameterSource metricParams(UUID termId, UUID versionId) {
        return new MapSqlParameterSource("termId", termId).addValue("versionId", versionId);
    }

    private MapSqlParameterSource params(UUID id, UUID universityId, long version) {
        return new MapSqlParameterSource().addValue("id", id)
                .addValue("universityId", universityId).addValue("version", version);
    }

    private MapSqlParameterSource groupParams(
            UUID id,
            AuraOperationsDtos.UpsertTeachingGroupRequest request) {
        return new MapSqlParameterSource().addValue("id", id)
                .addValue("offeringId", request.offeringId())
                .addValue("groupType", normalize(request.groupType()))
                .addValue("code", request.code().trim())
                .addValue("displayName", request.displayName().trim())
                .addValue("capacity", request.capacity()).addValue("active", request.active());
    }

    private MapSqlParameterSource conflictParams(
            UUID id,
            AuraOperationsDtos.UpsertOfferingConflictRequest request) {
        UUID left = request.leftOfferingId().compareTo(request.rightOfferingId()) < 0
                ? request.leftOfferingId() : request.rightOfferingId();
        UUID right = left.equals(request.leftOfferingId())
                ? request.rightOfferingId() : request.leftOfferingId();
        return new MapSqlParameterSource().addValue("id", id)
                .addValue("termId", request.termId()).addValue("leftOfferingId", left)
                .addValue("rightOfferingId", right).addValue("source", normalize(request.source()))
                .addValue("severity", normalize(request.severity()))
                .addValue("reason", request.reason().trim()).addValue("active", request.active());
    }

    private MapSqlParameterSource travelParams(
            UUID id,
            UUID universityId,
            AuraOperationsDtos.UpsertTravelRuleRequest request) {
        return new MapSqlParameterSource().addValue("id", id)
                .addValue("universityId", universityId)
                .addValue("fromBuilding", request.fromBuilding().trim())
                .addValue("toBuilding", request.toBuilding().trim())
                .addValue("minutes", request.minutes())
                .addValue("difficulty", normalize(request.difficulty()))
                .addValue("active", request.active());
    }

    private StudentAvailabilityResponse studentAvailability(ResultSet rs) throws SQLException {
        return new StudentAvailabilityResponse(
                rs.getObject("id", UUID.class), rs.getObject("term_id", UUID.class),
                rs.getObject("student_user_id", UUID.class),
                rs.getObject("timeslot_id", UUID.class),
                rs.getString("availability"), rs.getString("reason"), rs.getLong("version"));
    }

    private RepairPlanRow mapRepairPlan(ResultSet rs, int row) throws SQLException {
        Map<String, Object> moveEnvelope = jsonMap(rs.getString("proposed_moves"));
        List<RepairMove> moves = objectMapper.convertValue(
                moveEnvelope.getOrDefault("moves", List.of()),
                new TypeReference<List<RepairMove>>() { });
        RepairImpact impact = objectMapper.convertValue(
                jsonMap(rs.getString("impact")), RepairImpact.class);
        return new RepairPlanRow(
                rs.getObject("id", UUID.class),
                rs.getObject("university_id", UUID.class),
                rs.getObject("term_id", UUID.class),
                rs.getObject("source_version_id", UUID.class),
                rs.getObject("draft_version_id", UUID.class),
                rs.getObject("requested_by_user_id", UUID.class),
                rs.getString("trigger_type"), rs.getObject("trigger_id", UUID.class),
                rs.getString("status"), rs.getLong("input_revision"),
                rs.getLong("source_version_revision"),
                rs.getLong("current_source_version"),
                rs.getLong("current_input_revision"), moves, impact,
                rs.getString("preview_hash"), rs.getString("reason"),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("applied_at") == null ? null
                        : rs.getTimestamp("applied_at").toInstant());
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Map<String, Object> jsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Stored AURA audit metadata is invalid.", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("AURA audit metadata could not be serialized.", exception);
        }
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record RepairSourceState(
            UUID versionId,
            UUID termId,
            UUID universityId,
            String status,
            long versionRevision,
            long inputRevision,
            long currentInputRevision) {
    }

    public record RepairPlanRow(
            UUID id,
            UUID universityId,
            UUID termId,
            UUID sourceVersionId,
            UUID draftVersionId,
            UUID requestedByUserId,
            String triggerType,
            UUID triggerId,
            String status,
            long inputRevision,
            long sourceVersionRevision,
            long currentSourceVersion,
            long currentInputRevision,
            List<RepairMove> moves,
            RepairImpact impact,
            String previewHash,
            String reason,
            Instant expiresAt,
            Instant appliedAt) {
    }
}
