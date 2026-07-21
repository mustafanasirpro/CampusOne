package com.campusone.aura.service;

import com.campusone.aura.dto.AuraOperationsDtos;
import com.campusone.aura.dto.AuraOperationsDtos.AuditEventResponse;
import com.campusone.aura.dto.AuraOperationsDtos.AnalyticsResponse;
import com.campusone.aura.dto.AuraOperationsDtos.ScopedTimetableResponse;
import com.campusone.aura.dto.AuraOperationsDtos.BuildingResponse;
import com.campusone.aura.dto.AuraOperationsDtos.MutationResponse;
import com.campusone.aura.dto.AuraOperationsDtos.OfferingConflictResponse;
import com.campusone.aura.dto.AuraOperationsDtos.StudentAvailabilityResponse;
import com.campusone.aura.dto.AuraOperationsDtos.TeachingGroupResponse;
import com.campusone.aura.dto.AuraOperationsDtos.TravelRuleResponse;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.repository.AuraOperationsRepository;
import com.campusone.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraOperationsService {

    private static final Set<String> TERM_STATUSES = Set.of("DRAFT", "READY", "ARCHIVED");
    private static final Set<String> ROOM_TYPES = Set.of(
            "CLASSROOM", "LAB", "LECTURE_HALL", "SEMINAR_ROOM");
    private static final Set<String> SLOT_TYPES = Set.of("INSTRUCTIONAL", "BREAK");
    private static final Set<String> MEETING_TYPES = Set.of(
            "LECTURE", "LAB", "TUTORIAL", "SEMINAR");
    private static final Set<String> WEEK_PATTERNS = Set.of(
            "EVERY_WEEK", "ODD_WEEK", "EVEN_WEEK", "CUSTOM_WEEK_SET");
    private static final Set<String> AVAILABILITY_TYPES = Set.of(
            "AVAILABLE", "UNAVAILABLE", "AVOID", "PREFERRED");
    private static final Set<String> CONFLICT_SOURCES = Set.of(
            "REPEATER_REGISTRATION", "ELECTIVE_REGISTRATION",
            "SHARED_STUDENTS", "PROGRAM_POLICY", "MANUAL");
    private static final Set<String> CONFLICT_SEVERITIES = Set.of("HARD", "MEDIUM");
    private static final Set<String> TRAVEL_DIFFICULTIES = Set.of(
            "NORMAL", "DIFFICULT", "IMPOSSIBLE");

    private final AuraAuthorizationService authorization;
    private final AuraJdbcRepository coreRepository;
    private final AuraOperationsRepository repository;

    public AuraOperationsService(
            AuraAuthorizationService authorization,
            AuraJdbcRepository coreRepository,
            AuraOperationsRepository repository) {
        this.authorization = authorization;
        this.coreRepository = coreRepository;
        this.repository = repository;
    }

    @Transactional
    public MutationResponse updateTerm(
            UUID actorId,
            UUID termId,
            AuraOperationsDtos.UpdateTermRequest request) {
        UUID universityId = adminUniversity(actorId);
        requireDates(request.startsOn(), request.endsOn());
        requireEnum(request.status(), TERM_STATUSES, "Choose a supported term status.");
        try {
            ZoneId.of(request.timezone().trim());
        } catch (DateTimeException exception) {
            throw new AuraStateException("Choose a valid timetable timezone.");
        }
        return mutate(actorId, universityId, termId, "TERM", "TERM_UPDATED",
                () -> repository.updateTerm(termId, universityId, request));
    }

    @Transactional
    public MutationResponse setActiveState(
            UUID actorId,
            String requestedType,
            UUID resourceId,
            AuraOperationsDtos.ActiveStateRequest request) {
        UUID universityId = adminUniversity(actorId);
        String resourceType = normalize(requestedType).replace('-', '_');
        ScopedResource scope = switch (resourceType) {
            case "PROGRAM" -> ScopedResource.PROGRAM;
            case "BATCH" -> ScopedResource.BATCH;
            case "SECTION" -> ScopedResource.SECTION;
            case "INSTRUCTOR" -> ScopedResource.INSTRUCTOR;
            case "ROOM" -> ScopedResource.ROOM;
            case "BUILDING" -> ScopedResource.BUILDING;
            case "TIMESLOT" -> ScopedResource.TIMESLOT;
            case "OFFERING" -> ScopedResource.OFFERING;
            case "REQUIREMENT", "MEETING_REQUIREMENT" -> ScopedResource.REQUIREMENT;
            default -> throw new AuraStateException("Choose a supported setup resource.");
        };
        if (scope == ScopedResource.REQUIREMENT) resourceType = "REQUIREMENT";
        requireScoped(scope, resourceId, universityId, "Scheduling resource");
        MutationResponse response = repository.setActiveState(
                resourceType, resourceId, request.active(), request.version())
                .orElseThrow(this::stale);
        UUID termId = repository.termForResource(resourceType, resourceId).orElse(null);
        audit(actorId, universityId, termId,
                request.active() ? resourceType + "_ACTIVATED" : resourceType + "_DEACTIVATED",
                resourceType, resourceId);
        return response;
    }

    @Transactional
    public MutationResponse updateProgram(
            UUID actorId,
            UUID programId,
            AuraOperationsDtos.UpdateProgramRequest request) {
        UUID universityId = adminUniversity(actorId);
        if (!coreRepository.departmentBelongsToUniversity(request.departmentId(), universityId)) {
            throw notFound("Department was not found.");
        }
        return mutate(actorId, universityId, programId, "PROGRAM", "PROGRAM_UPDATED",
                () -> repository.updateProgram(programId, universityId, request));
    }

    @Transactional
    public MutationResponse updateBatch(
            UUID actorId,
            UUID batchId,
            AuraOperationsDtos.UpdateBatchRequest request) {
        UUID universityId = adminUniversity(actorId);
        if (request.expectedGraduationYear() != null
                && request.expectedGraduationYear() < request.admissionYear()) {
            throw new AuraStateException(
                    "Expected graduation year cannot be before the admission year.");
        }
        return mutate(actorId, universityId, batchId, "BATCH", "BATCH_UPDATED",
                () -> repository.updateBatch(batchId, universityId, request));
    }

    @Transactional
    public MutationResponse updateSection(
            UUID actorId,
            UUID sectionId,
            AuraOperationsDtos.UpdateSectionRequest request) {
        UUID universityId = adminUniversity(actorId);
        requireScoped(ScopedResource.BATCH, request.batchId(), universityId, "Batch");
        if (request.termId() != null) {
            requireScoped(ScopedResource.TERM, request.termId(), universityId, "Term");
        }
        if (request.preferredDailyLoad() > request.hardDailyLoad()) {
            throw new AuraStateException(
                    "Preferred daily load cannot exceed the hard daily load.");
        }
        return mutate(actorId, universityId, sectionId, "SECTION", "SECTION_UPDATED",
                () -> repository.updateSection(sectionId, universityId, request));
    }

    @Transactional
    public MutationResponse updateInstructor(
            UUID actorId,
            UUID instructorId,
            AuraOperationsDtos.UpdateInstructorRequest request) {
        UUID universityId = adminUniversity(actorId);
        if (request.userId() != null
                && !coreRepository.userBelongsToUniversity(request.userId(), universityId)) {
            throw notFound("Instructor user was not found.");
        }
        if (request.departmentId() != null
                && !coreRepository.departmentBelongsToUniversity(
                        request.departmentId(), universityId)) {
            throw notFound("Department was not found.");
        }
        if (request.preferredDailyLoad() > request.hardDailyLoad()
                || request.preferredWeeklyLoad() > request.maxHoursPerWeek()) {
            throw new AuraStateException("Instructor load preferences exceed their hard limits.");
        }
        return mutate(actorId, universityId, instructorId, "INSTRUCTOR", "INSTRUCTOR_UPDATED",
                () -> repository.updateInstructor(instructorId, universityId, request));
    }

    @Transactional
    public MutationResponse updateRoom(
            UUID actorId,
            UUID roomId,
            AuraOperationsDtos.UpdateRoomRequest request) {
        UUID universityId = adminUniversity(actorId);
        requireEnum(request.roomType(), ROOM_TYPES, "Choose a supported room type.");
        try {
            MutationResponse response = repository.updateRoom(roomId, universityId, request)
                    .orElseThrow(this::stale);
            coreRepository.replaceRoomFacilities(
                    roomId,
                    new java.util.LinkedHashSet<>(request.facilities() == null
                            ? List.of() : request.facilities().stream()
                            .map(this::normalize).toList()));
            audit(actorId, universityId, null, "ROOM_UPDATED", "ROOM", roomId);
            return response;
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Room details conflict with existing scheduling data.");
        }
    }

    @Transactional
    public MutationResponse updateTimeslot(
            UUID actorId,
            UUID timeslotId,
            AuraOperationsDtos.UpdateTimeslotRequest request) {
        UUID universityId = adminUniversity(actorId);
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new AuraStateException("Timeslot start time must be before its end time.");
        }
        requireEnum(request.slotType(), SLOT_TYPES, "Choose a supported timeslot type.");
        return mutate(actorId, universityId, timeslotId, "TIMESLOT", "TIMESLOT_UPDATED",
                () -> repository.updateTimeslot(timeslotId, universityId, request));
    }

    @Transactional
    public MutationResponse updateOffering(
            UUID actorId,
            UUID offeringId,
            AuraOperationsDtos.UpdateOfferingRequest request) {
        UUID universityId = adminUniversity(actorId);
        if (!coreRepository.courseBelongsToUniversity(request.courseId(), universityId)) {
            throw notFound("Course was not found.");
        }
        requireScoped(ScopedResource.SECTION, request.sectionId(), universityId, "Section");
        requireScoped(ScopedResource.INSTRUCTOR, request.instructorId(), universityId, "Instructor");
        requireEnum(request.status(), Set.of("ACTIVE", "INACTIVE"),
                "Choose a supported offering status.");
        if (request.maximumEnrollment() != null
                && request.maximumEnrollment() < request.expectedStudents()) {
            throw new AuraStateException(
                    "Maximum enrollment cannot be below expected enrollment.");
        }
        return mutate(actorId, universityId, offeringId, "OFFERING", "OFFERING_UPDATED",
                () -> repository.updateOffering(offeringId, universityId, request));
    }

    @Transactional
    public MutationResponse updateRequirement(
            UUID actorId,
            UUID requirementId,
            AuraOperationsDtos.UpdateMeetingRequirementRequest request) {
        UUID universityId = adminUniversity(actorId);
        requireEnum(request.meetingType(), MEETING_TYPES,
                "Choose a supported meeting type.");
        requireEnum(request.roomType(), ROOM_TYPES, "Choose a supported room type.");
        requireEnum(request.weekPattern(), WEEK_PATTERNS,
                "Choose a supported week pattern.");
        if (request.preferredStartTime() != null && request.preferredEndTime() != null
                && !request.preferredStartTime().isBefore(request.preferredEndTime())) {
            throw new AuraStateException("Preferred start time must be before its end time.");
        }
        boolean custom = normalize(request.weekPattern()).equals("CUSTOM_WEEK_SET");
        if (custom != (request.customWeeks() != null && !request.customWeeks().isEmpty())) {
            throw new AuraStateException(
                    "Custom week patterns require at least one selected week.");
        }
        try {
            MutationResponse response = repository.updateRequirement(
                    requirementId, universityId, request).orElseThrow(this::stale);
            coreRepository.replaceRequirementFacilities(
                    requirementId,
                    new java.util.LinkedHashSet<>(request.requiredFacilities() == null
                            ? List.of() : request.requiredFacilities().stream()
                            .map(this::normalize).toList()));
            audit(actorId, universityId,
                    repository.termForResource("MEETING_REQUIREMENT", requirementId).orElse(null),
                    "MEETING_REQUIREMENT_UPDATED", "MEETING_REQUIREMENT", requirementId);
            return response;
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Meeting requirement details are no longer valid.");
        }
    }

    @Transactional
    public BuildingResponse createBuilding(
            UUID actorId,
            AuraOperationsDtos.CreateBuildingRequest request) {
        UUID universityId = adminUniversity(actorId);
        try {
            UUID id = repository.insertBuilding(universityId, request);
            audit(actorId, universityId, null, "BUILDING_CREATED", "BUILDING", id);
            return repository.listBuildings(universityId).stream()
                    .filter(row -> row.id().equals(id)).findFirst()
                    .orElseThrow(() -> notFound("Building was not found."));
        } catch (DataIntegrityViolationException exception) {
            throw conflict("A building with this code or name already exists.");
        }
    }

    @Transactional(readOnly = true)
    public List<BuildingResponse> listBuildings(UUID actorId) {
        return repository.listBuildings(adminUniversity(actorId));
    }

    @Transactional
    public MutationResponse updateBuilding(
            UUID actorId,
            UUID buildingId,
            AuraOperationsDtos.UpdateBuildingRequest request) {
        UUID universityId = adminUniversity(actorId);
        return mutate(actorId, universityId, buildingId, "BUILDING", "BUILDING_UPDATED",
                () -> repository.updateBuilding(buildingId, universityId, request));
    }

    @Transactional
    public TeachingGroupResponse createTeachingGroup(
            UUID actorId,
            AuraOperationsDtos.UpsertTeachingGroupRequest request) {
        UUID universityId = adminUniversity(actorId);
        requireScoped(ScopedResource.OFFERING, request.offeringId(), universityId, "Offering");
        requireEnum(request.groupType(), Set.of("LECTURE", "LAB", "TUTORIAL"),
                "Choose a supported teaching group type.");
        try {
            UUID id = repository.insertTeachingGroup(request);
            UUID termId = repository.termForResource("OFFERING", request.offeringId()).orElse(null);
            audit(actorId, universityId, termId,
                    "TEACHING_GROUP_CREATED", "TEACHING_GROUP", id);
            return repository.listTeachingGroups(termId).stream()
                    .filter(row -> row.id().equals(id)).findFirst()
                    .orElseThrow(() -> notFound("Teaching group was not found."));
        } catch (DataIntegrityViolationException exception) {
            throw conflict("This teaching group conflicts with existing setup data.");
        }
    }

    @Transactional
    public TeachingGroupResponse updateTeachingGroup(
            UUID actorId,
            UUID groupId,
            AuraOperationsDtos.UpsertTeachingGroupRequest request) {
        UUID universityId = adminUniversity(actorId);
        requireScoped(ScopedResource.OFFERING, request.offeringId(), universityId, "Offering");
        if (request.version() == null) throw stale();
        requireEnum(request.groupType(), Set.of("LECTURE", "LAB", "TUTORIAL"),
                "Choose a supported teaching group type.");
        try {
            if (!repository.updateTeachingGroup(groupId, request)) throw stale();
            UUID termId = repository.termForResource("OFFERING", request.offeringId()).orElse(null);
            audit(actorId, universityId, termId,
                    "TEACHING_GROUP_UPDATED", "TEACHING_GROUP", groupId);
            return repository.listTeachingGroups(termId).stream()
                    .filter(row -> row.id().equals(groupId)).findFirst()
                    .orElseThrow(() -> notFound("Teaching group was not found."));
        } catch (DataIntegrityViolationException exception) {
            throw conflict("This teaching group conflicts with existing setup data.");
        }
    }

    @Transactional(readOnly = true)
    public List<TeachingGroupResponse> listTeachingGroups(UUID actorId, UUID termId) {
        UUID universityId = adminUniversity(actorId);
        requireScoped(ScopedResource.TERM, termId, universityId, "Term");
        return repository.listTeachingGroups(termId);
    }

    @Transactional
    public StudentAvailabilityResponse upsertStudentAvailability(
            UUID actorId,
            AuraOperationsDtos.UpsertStudentAvailabilityRequest request) {
        UUID universityId = adminUniversity(actorId);
        requireScoped(ScopedResource.TERM, request.termId(), universityId, "Term");
        requireScoped(ScopedResource.TIMESLOT, request.timeslotId(), universityId, "Timeslot");
        if (!coreRepository.userBelongsToUniversity(request.studentUserId(), universityId)) {
            throw notFound("Student was not found.");
        }
        requireEnum(request.availability(), AVAILABILITY_TYPES,
                "Choose a supported availability type.");
        StudentAvailabilityResponse response = repository.upsertStudentAvailability(request);
        audit(actorId, universityId, request.termId(),
                "STUDENT_AVAILABILITY_UPDATED", "STUDENT_AVAILABILITY", response.id());
        return response;
    }

    @Transactional(readOnly = true)
    public List<StudentAvailabilityResponse> listStudentAvailability(
            UUID actorId,
            UUID termId,
            UUID studentUserId) {
        UUID universityId = adminUniversity(actorId);
        requireScoped(ScopedResource.TERM, termId, universityId, "Term");
        if (studentUserId != null
                && !coreRepository.userBelongsToUniversity(studentUserId, universityId)) {
            throw notFound("Student was not found.");
        }
        return repository.listStudentAvailability(termId, studentUserId);
    }

    @Transactional
    public OfferingConflictResponse createOfferingConflict(
            UUID actorId,
            AuraOperationsDtos.UpsertOfferingConflictRequest request) {
        UUID universityId = adminUniversity(actorId);
        validateConflict(request, universityId);
        try {
            UUID id = repository.insertOfferingConflict(actorId, request);
            audit(actorId, universityId, request.termId(),
                    "OFFERING_CONFLICT_CREATED", "OFFERING_CONFLICT", id);
            return findConflict(request.termId(), id);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("This offering conflict already exists or is no longer valid.");
        }
    }

    @Transactional
    public OfferingConflictResponse updateOfferingConflict(
            UUID actorId,
            UUID conflictId,
            AuraOperationsDtos.UpsertOfferingConflictRequest request) {
        UUID universityId = adminUniversity(actorId);
        validateConflict(request, universityId);
        if (request.version() == null) throw stale();
        try {
            if (!repository.updateOfferingConflict(conflictId, request)) throw stale();
            audit(actorId, universityId, request.termId(),
                    "OFFERING_CONFLICT_UPDATED", "OFFERING_CONFLICT", conflictId);
            return findConflict(request.termId(), conflictId);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("This offering conflict already exists or is no longer valid.");
        }
    }

    @Transactional(readOnly = true)
    public List<OfferingConflictResponse> listOfferingConflicts(UUID actorId, UUID termId) {
        UUID universityId = adminUniversity(actorId);
        requireScoped(ScopedResource.TERM, termId, universityId, "Term");
        return repository.listOfferingConflicts(termId);
    }

    @Transactional
    public TravelRuleResponse createTravelRule(
            UUID actorId,
            AuraOperationsDtos.UpsertTravelRuleRequest request) {
        UUID universityId = adminUniversity(actorId);
        validateTravel(request);
        try {
            UUID id = repository.insertTravelRule(universityId, request);
            audit(actorId, universityId, null,
                    "TRAVEL_RULE_CREATED", "TRAVEL_RULE", id);
            return findTravel(universityId, id);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("This building travel rule already exists.");
        }
    }

    @Transactional
    public TravelRuleResponse updateTravelRule(
            UUID actorId,
            UUID ruleId,
            AuraOperationsDtos.UpsertTravelRuleRequest request) {
        UUID universityId = adminUniversity(actorId);
        validateTravel(request);
        if (request.version() == null) throw stale();
        try {
            if (!repository.updateTravelRule(ruleId, universityId, request)) throw stale();
            audit(actorId, universityId, null,
                    "TRAVEL_RULE_UPDATED", "TRAVEL_RULE", ruleId);
            return findTravel(universityId, ruleId);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("This building travel rule conflicts with existing setup data.");
        }
    }

    @Transactional(readOnly = true)
    public List<TravelRuleResponse> listTravelRules(UUID actorId) {
        return repository.listTravelRules(adminUniversity(actorId));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> listAudit(
            UUID actorId,
            UUID termId,
            String action,
            String targetType,
            Instant from,
            Instant to,
            int page,
            int size) {
        UUID universityId = adminUniversity(actorId);
        if (termId != null) requireScoped(ScopedResource.TERM, termId, universityId, "Term");
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        return repository.listAudit(universityId, termId,
                normalizeOptional(action), normalizeOptional(targetType), from, to,
                safeSize, safePage * safeSize);
    }

    @Transactional(readOnly = true)
    public ScopedTimetableResponse scopedTimetable(
            UUID actorId,
            UUID versionId,
            String requestedScope,
            UUID scopeId,
            Integer dayOfWeek) {
        UUID universityId = adminUniversity(actorId);
        requireScoped(ScopedResource.VERSION, versionId, universityId, "Timetable version");
        String scope = normalize(requestedScope);
        validateTimetableScope(scope, scopeId, dayOfWeek, universityId);
        var version = coreRepository.findVersion(versionId)
                .orElseThrow(() -> notFound("Timetable version was not found."));
        return new ScopedTimetableResponse(
                version.termId(), versionId, scope, scopeId, scopeLabel(scope, dayOfWeek),
                coreRepository.listScopedSessions(versionId, scope, scopeId, dayOfWeek));
    }

    @Transactional(readOnly = true)
    public ScopedTimetableResponse instructorTimetable(
            UUID actorId,
            UUID termId) {
        UUID universityId = authorization.requireUniversity(actorId);
        requireScoped(ScopedResource.TERM, termId, universityId, "Term");
        UUID instructorId = coreRepository.findInstructorIdForUser(actorId, universityId)
                .orElseThrow(() -> notFound("Instructor profile was not found."));
        UUID versionId = coreRepository.findPublishedVersionId(termId)
                .orElseThrow(() -> notFound("Published timetable was not found."));
        return new ScopedTimetableResponse(
                termId, versionId, "INSTRUCTOR", instructorId, "My teaching timetable",
                coreRepository.listScopedSessions(
                        versionId, "INSTRUCTOR", instructorId, null));
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(UUID actorId, UUID termId, UUID versionId) {
        UUID universityId = adminUniversity(actorId);
        requireScoped(ScopedResource.TERM, termId, universityId, "Term");
        requireScoped(ScopedResource.VERSION, versionId, universityId, "Timetable version");
        var version = coreRepository.findVersion(versionId)
                .orElseThrow(() -> notFound("Timetable version was not found."));
        if (!version.termId().equals(termId)) {
            throw notFound("Timetable version was not found.");
        }
        return repository.analytics(termId, versionId);
    }

    private void validateConflict(
            AuraOperationsDtos.UpsertOfferingConflictRequest request,
            UUID universityId) {
        requireScoped(ScopedResource.TERM, request.termId(), universityId, "Term");
        requireScoped(ScopedResource.OFFERING, request.leftOfferingId(), universityId,
                "Left offering");
        requireScoped(ScopedResource.OFFERING, request.rightOfferingId(), universityId,
                "Right offering");
        if (request.leftOfferingId().equals(request.rightOfferingId())) {
            throw new AuraStateException("Choose two different offerings.");
        }
        requireEnum(request.source(), CONFLICT_SOURCES,
                "Choose a supported conflict source.");
        requireEnum(request.severity(), CONFLICT_SEVERITIES,
                "Choose a supported conflict severity.");
    }

    private void validateTravel(AuraOperationsDtos.UpsertTravelRuleRequest request) {
        if (request.fromBuilding().trim().equalsIgnoreCase(request.toBuilding().trim())) {
            throw new AuraStateException("Choose two different buildings.");
        }
        requireEnum(request.difficulty(), TRAVEL_DIFFICULTIES,
                "Choose a supported travel difficulty.");
    }

    private void validateTimetableScope(
            String scope,
            UUID scopeId,
            Integer dayOfWeek,
            UUID universityId) {
        switch (scope) {
            case "UNIVERSITY", "WEEK" -> {
                if (scopeId != null || dayOfWeek != null) {
                    throw new AuraStateException("This timetable scope does not accept a resource.");
                }
            }
            case "DAY" -> {
                if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
                    throw new AuraStateException("Choose a day from Monday through Sunday.");
                }
            }
            case "INSTRUCTOR" -> requireScoped(
                    ScopedResource.INSTRUCTOR, requiredScopeId(scopeId), universityId, "Instructor");
            case "SECTION" -> requireScoped(
                    ScopedResource.SECTION, requiredScopeId(scopeId), universityId, "Section");
            case "ROOM" -> requireScoped(
                    ScopedResource.ROOM, requiredScopeId(scopeId), universityId, "Room");
            case "OFFERING" -> requireScoped(
                    ScopedResource.OFFERING, requiredScopeId(scopeId), universityId, "Offering");
            case "PROGRAM" -> requireScoped(
                    ScopedResource.PROGRAM, requiredScopeId(scopeId), universityId, "Program");
            case "COURSE" -> {
                UUID id = requiredScopeId(scopeId);
                if (!coreRepository.courseBelongsToUniversity(id, universityId)) {
                    throw notFound("Course was not found.");
                }
            }
            case "DEPARTMENT" -> {
                UUID id = requiredScopeId(scopeId);
                if (!coreRepository.departmentBelongsToUniversity(id, universityId)) {
                    throw notFound("Department was not found.");
                }
            }
            default -> throw new AuraStateException("Choose a supported timetable view.");
        }
    }

    private UUID requiredScopeId(UUID scopeId) {
        if (scopeId == null) throw new AuraStateException("Choose a timetable resource.");
        return scopeId;
    }

    private String scopeLabel(String scope, Integer dayOfWeek) {
        if ("DAY".equals(scope)) {
            return java.time.DayOfWeek.of(dayOfWeek).getDisplayName(
                    java.time.format.TextStyle.FULL, Locale.ENGLISH);
        }
        return switch (scope) {
            case "WEEK" -> "Weekly timetable";
            case "UNIVERSITY" -> "University timetable";
            default -> scope.charAt(0) + scope.substring(1).toLowerCase(Locale.ROOT)
                    + " timetable";
        };
    }

    private OfferingConflictResponse findConflict(UUID termId, UUID id) {
        return repository.listOfferingConflicts(termId).stream()
                .filter(row -> row.id().equals(id)).findFirst()
                .orElseThrow(() -> notFound("Offering conflict was not found."));
    }

    private TravelRuleResponse findTravel(UUID universityId, UUID id) {
        return repository.listTravelRules(universityId).stream()
                .filter(row -> row.id().equals(id)).findFirst()
                .orElseThrow(() -> notFound("Building travel rule was not found."));
    }

    private MutationResponse mutate(
            UUID actorId,
            UUID universityId,
            UUID targetId,
            String targetType,
            String action,
            Mutation operation) {
        try {
            MutationResponse response = operation.run().orElseThrow(this::stale);
            audit(actorId, universityId,
                    repository.termForResource(targetType, targetId).orElse(null),
                    action, targetType, targetId);
            return response;
        } catch (DataIntegrityViolationException exception) {
            throw conflict("The requested change conflicts with existing scheduling data.");
        }
    }

    private void audit(
            UUID actorId,
            UUID universityId,
            UUID termId,
            String action,
            String targetType,
            UUID targetId) {
        repository.insertAudit(universityId, termId, actorId, action, targetType,
                targetId, action.replace('_', ' ').toLowerCase(Locale.ROOT),
                UUID.randomUUID(), "SUCCESS", Map.of());
    }

    private UUID adminUniversity(UUID actorId) {
        return authorization.requireAdminUniversity(actorId);
    }

    private void requireScoped(
            ScopedResource type,
            UUID id,
            UUID universityId,
            String label) {
        if (!coreRepository.resourceBelongsToUniversity(type, id, universityId)) {
            throw notFound(label + " was not found.");
        }
    }

    private void requireDates(java.time.LocalDate from, java.time.LocalDate to) {
        if (from.isAfter(to)) {
            throw new AuraStateException("Start date must be on or before the end date.");
        }
    }

    private void requireEnum(String value, Set<String> allowed, String message) {
        if (!allowed.contains(normalize(value))) throw new AuraStateException(message);
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : normalize(value);
    }

    private AuraStateException stale() {
        return new AuraStateException(
                "This scheduling record changed while you were editing it. Refresh and try again.");
    }

    private AuraStateException conflict(String message) {
        return new AuraStateException(message);
    }

    private ResourceNotFoundException notFound(String message) {
        String suffix = " was not found.";
        String resourceName = message.endsWith(suffix)
                ? message.substring(0, message.length() - suffix.length())
                : message;
        return new ResourceNotFoundException(resourceName);
    }

    @FunctionalInterface
    private interface Mutation {
        java.util.Optional<MutationResponse> run();
    }
}
