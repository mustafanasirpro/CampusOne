package com.campusone.aura.service;

import com.campusone.aura.dto.AuraDtos;
import com.campusone.aura.dto.AuraDtos.BatchResponse;
import com.campusone.aura.dto.AuraDtos.ClashResponse;
import com.campusone.aura.dto.AuraDtos.GenerationRunResponse;
import com.campusone.aura.dto.AuraDtos.InstructorResponse;
import com.campusone.aura.dto.AuraDtos.MeetingRequirementResponse;
import com.campusone.aura.dto.AuraDtos.OfferingResponse;
import com.campusone.aura.dto.AuraDtos.PageResponse;
import com.campusone.aura.dto.AuraDtos.ProgramResponse;
import com.campusone.aura.dto.AuraDtos.ReadinessResponse;
import com.campusone.aura.dto.AuraDtos.RoomResponse;
import com.campusone.aura.dto.AuraDtos.SectionResponse;
import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.dto.AuraDtos.TermResponse;
import com.campusone.aura.dto.AuraDtos.TimetableVersionResponse;
import com.campusone.aura.dto.AuraDtos.TimeslotResponse;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.repository.AuraJdbcRepository.DetectedClash;
import com.campusone.aura.repository.AuraJdbcRepository.VersionSessionSnapshot;
import com.campusone.aura.repository.AuraJdbcRepository.ConstraintConfigurationRow;
import com.campusone.aura.solver.AuraConstraintCatalog;
import com.campusone.aura.solver.AuraConstraintCatalog.ConstraintWeight;
import com.campusone.aura.solver.AuraConstraintCatalog.Level;
import com.campusone.common.exception.ResourceNotFoundException;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraService {

    private static final Logger LOG = LoggerFactory.getLogger(AuraService.class);

    private static final Set<String> FACILITIES = Set.of(
            "PROJECTOR",
            "SMART_BOARD",
            "COMPUTERS",
            "INTERNET",
            "LAB_EQUIPMENT",
            "ACCESSIBLE",
            "AIR_CONDITIONING",
            "VIDEO_CONFERENCING",
            "SPECIALIZED_SOFTWARE",
            "OTHER");
    private static final Set<String> CALENDAR_EXCEPTION_TYPES = Set.of(
            "HOLIDAY",
            "NON_TEACHING_DAY",
            "UNIVERSITY_EVENT",
            "INSTRUCTOR_ABSENCE",
            "ROOM_CLOSURE",
            "SECTION_RESTRICTION",
            "TIMESLOT_CANCELLATION",
            "FACILITY_OUTAGE");

    private final AuraAuthorizationService authorizationService;
    private final AuraJdbcRepository repository;
    private final AuraReadinessValidator readinessValidator;
    private final AuraSolverService solverService;
    private final AuraClashDetector clashDetector;
    private final AuraGenerationPersistenceService generationPersistenceService;
    private final AuraNotificationService notificationService;
    private final Clock clock;
    private final ExecutorService generationExecutor =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "aura-generation");
                thread.setDaemon(true);
                return thread;
            });
    private final Map<UUID, Future<?>> runningRuns = new ConcurrentHashMap<>();

    public AuraService(
            AuraAuthorizationService authorizationService,
            AuraJdbcRepository repository,
            AuraReadinessValidator readinessValidator,
            AuraSolverService solverService,
            AuraClashDetector clashDetector,
            AuraGenerationPersistenceService generationPersistenceService,
            AuraNotificationService notificationService,
            Clock clock) {
        this.authorizationService = authorizationService;
        this.repository = repository;
        this.readinessValidator = readinessValidator;
        this.solverService = solverService;
        this.clashDetector = clashDetector;
        this.generationPersistenceService = generationPersistenceService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @PreDestroy
    void shutdown() {
        generationExecutor.shutdownNow();
    }

    public TermResponse createTerm(
            UUID userId,
            AuraDtos.CreateTermRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireRequestedUniversity(request.universityId(), universityId);
        if (request.startsOn().isAfter(request.endsOn())) {
            throw new AuraStateException("Term start date must be before the end date.");
        }
        UUID id = repository.insertTerm(UUID.randomUUID(), userId, request);
        return repository.findTerm(id)
                .orElseThrow(() -> notFound("AURA term was not found."));
    }

    public PageResponse<TermResponse> listTerms(UUID userId, int page, int size) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        long total = repository.countTerms(universityId);
        List<TermResponse> content = repository.listTerms(
                universityId,
                safePage,
                safeSize);
        return page(content, safePage, safeSize, total);
    }

    public ProgramResponse createProgram(
            UUID userId,
            AuraDtos.CreateProgramRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireRequestedUniversity(request.universityId(), universityId);
        if (!repository.departmentBelongsToUniversity(
                request.departmentId(),
                universityId)) {
            throw notFound("Department was not found.");
        }
        UUID id = repository.insertProgram(UUID.randomUUID(), request);
        return repository.listPrograms(request.universityId()).stream()
                .filter(program -> program.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Program was not found."));
    }

    public List<ProgramResponse> listPrograms(UUID userId, UUID universityId) {
        UUID adminUniversityId = authorizationService.requireAdminUniversity(userId);
        requireOptionalRequestedUniversity(universityId, adminUniversityId);
        return repository.listPrograms(adminUniversityId);
    }

    public BatchResponse createBatch(
            UUID userId,
            AuraDtos.CreateBatchRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.PROGRAM,
                request.programId(),
                universityId,
                "Program was not found.");
        UUID id = repository.insertBatch(UUID.randomUUID(), request);
        return repository.listBatches(universityId, request.programId()).stream()
                .filter(batch -> batch.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Batch was not found."));
    }

    public List<BatchResponse> listBatches(UUID userId, UUID programId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        if (programId != null) {
            requireScopedResource(
                    ScopedResource.PROGRAM,
                    programId,
                    universityId,
                    "Program was not found.");
        }
        return repository.listBatches(universityId, programId);
    }

    public SectionResponse createSection(
            UUID userId,
            AuraDtos.CreateSectionRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.BATCH,
                request.batchId(),
                universityId,
                "Batch was not found.");
        UUID id = repository.insertSection(UUID.randomUUID(), request);
        return repository.listSections(universityId, request.batchId()).stream()
                .filter(section -> section.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Section was not found."));
    }

    public List<SectionResponse> listSections(UUID userId, UUID batchId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        if (batchId != null) {
            requireScopedResource(
                    ScopedResource.BATCH,
                    batchId,
                    universityId,
                    "Batch was not found.");
        }
        return repository.listSections(universityId, batchId);
    }

    public InstructorResponse createInstructor(
            UUID userId,
            AuraDtos.CreateInstructorRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireRequestedUniversity(request.universityId(), universityId);
        if (request.userId() != null
                && !repository.userBelongsToUniversity(
                        request.userId(),
                        universityId)) {
            throw notFound("Linked instructor account was not found.");
        }
        UUID id = repository.insertInstructor(UUID.randomUUID(), request);
        return repository.listInstructors(request.universityId()).stream()
                .filter(instructor -> instructor.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Instructor was not found."));
    }

    public List<InstructorResponse> listInstructors(
            UUID userId,
            UUID universityId) {
        UUID adminUniversityId = authorizationService.requireAdminUniversity(userId);
        requireOptionalRequestedUniversity(universityId, adminUniversityId);
        return repository.listInstructors(adminUniversityId);
    }

    @Transactional
    public RoomResponse createRoom(
            UUID userId,
            AuraDtos.CreateRoomRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireRequestedUniversity(request.universityId(), universityId);
        if (request.capacity() <= 0) {
            throw new AuraStateException("Room capacity must be greater than zero.");
        }
        UUID id = repository.insertRoom(UUID.randomUUID(), request);
        repository.replaceRoomFacilities(
                id,
                normalizeFacilities(request.facilities()));
        return repository.listRooms(request.universityId()).stream()
                .filter(room -> room.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Room was not found."));
    }

    public List<RoomResponse> listRooms(UUID userId, UUID universityId) {
        UUID adminUniversityId = authorizationService.requireAdminUniversity(userId);
        requireOptionalRequestedUniversity(universityId, adminUniversityId);
        return repository.listRooms(adminUniversityId);
    }

    public TimeslotResponse createTimeslot(
            UUID userId,
            AuraDtos.CreateTimeslotRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireRequestedUniversity(request.universityId(), universityId);
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new AuraStateException("Timeslot start time must be before the end time.");
        }
        UUID id = repository.insertTimeslot(UUID.randomUUID(), request);
        return repository.listTimeslots(request.universityId()).stream()
                .filter(timeslot -> timeslot.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Timeslot was not found."));
    }

    public List<TimeslotResponse> listTimeslots(UUID userId, UUID universityId) {
        UUID adminUniversityId = authorizationService.requireAdminUniversity(userId);
        requireOptionalRequestedUniversity(universityId, adminUniversityId);
        return repository.listTimeslots(adminUniversityId);
    }

    public AuraDtos.AvailabilityResponse upsertInstructorAvailability(
            UUID userId,
            AuraDtos.CreateInstructorAvailabilityRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.INSTRUCTOR,
                request.instructorId(),
                universityId,
                "Instructor was not found.");
        requireScopedResource(
                ScopedResource.TIMESLOT,
                request.timeslotId(),
                universityId,
                "Timeslot was not found.");
        String availability = normalizeAvailability(request.availability());
        UUID id = repository.upsertInstructorAvailability(
                UUID.randomUUID(),
                request,
                availability);
        return repository.listInstructorAvailability(request.instructorId())
                .stream()
                .filter(entry -> entry.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Instructor availability was not found."));
    }

    public List<AuraDtos.AvailabilityResponse> listInstructorAvailability(
            UUID userId,
            UUID instructorId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.INSTRUCTOR,
                instructorId,
                universityId,
                "Instructor was not found.");
        return repository.listInstructorAvailability(instructorId);
    }

    public AuraDtos.AvailabilityResponse upsertRoomAvailability(
            UUID userId,
            AuraDtos.CreateRoomAvailabilityRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.ROOM,
                request.roomId(),
                universityId,
                "Room was not found.");
        requireScopedResource(
                ScopedResource.TIMESLOT,
                request.timeslotId(),
                universityId,
                "Timeslot was not found.");
        String availability = normalizeAvailability(request.availability());
        UUID id = repository.upsertRoomAvailability(
                UUID.randomUUID(),
                request,
                availability);
        return repository.listRoomAvailability(request.roomId()).stream()
                .filter(entry -> entry.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Room availability was not found."));
    }

    public List<AuraDtos.AvailabilityResponse> listRoomAvailability(
            UUID userId,
            UUID roomId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.ROOM,
                roomId,
                universityId,
                "Room was not found.");
        return repository.listRoomAvailability(roomId);
    }

    public AuraDtos.AvailabilityResponse upsertSectionAvailability(
            UUID userId,
            AuraDtos.CreateSectionAvailabilityRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.SECTION,
                request.sectionId(),
                universityId,
                "Section was not found.");
        requireScopedResource(
                ScopedResource.TIMESLOT,
                request.timeslotId(),
                universityId,
                "Timeslot was not found.");
        if (!repository.sectionAndTimeslotShareUniversity(
                request.sectionId(),
                request.timeslotId())) {
            throw new AuraStateException(
                    "Section availability must use a timeslot from the same university.");
        }
        String availability = normalizeAvailability(request.availability());
        UUID id = repository.upsertSectionAvailability(
                UUID.randomUUID(),
                request,
                availability);
        return repository.listSectionAvailability(request.sectionId()).stream()
                .filter(entry -> entry.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Section availability was not found."));
    }

    public List<AuraDtos.AvailabilityResponse> listSectionAvailability(
            UUID userId,
            UUID sectionId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.SECTION,
                sectionId,
                universityId,
                "Section was not found.");
        return repository.listSectionAvailability(sectionId);
    }

    public AuraDtos.SetupReferencesResponse setupReferences(UUID userId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        return new AuraDtos.SetupReferencesResponse(
                universityId,
                repository.listDepartmentReferences(universityId),
                repository.listCourseReferences(universityId),
                repository.listStudentReferences(universityId));
    }

    public AuraDtos.ConstraintProfileResponse constraintProfile(
            UUID userId,
            UUID termId,
            String requestedProfile) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.TERM,
                termId,
                universityId,
                "AURA term was not found.");
        String profile = normalizeConstraintProfile(requestedProfile);
        Map<String, ConstraintConfigurationRow> configured = repository
                .listConstraintConfigurations(termId, profile).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ConstraintConfigurationRow::constraintName,
                        row -> row));
        Map<String, ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore>
                resolved = AuraConstraintCatalog.weights(
                        profile,
                        configured.values().stream().collect(
                                java.util.stream.Collectors.toMap(
                                        ConstraintConfigurationRow::constraintName,
                                        row -> new ConstraintWeight(
                                                row.level(),
                                                row.active() ? row.weight() : 0))));
        List<AuraDtos.ConstraintWeightResponse> weights =
                AuraConstraintCatalog.constraints().entrySet().stream()
                        .map(entry -> {
                            ConstraintConfigurationRow custom = configured.get(entry.getKey());
                            long weight = custom == null
                                    ? scoreWeight(resolved.get(entry.getKey()), entry.getValue())
                                    : custom.weight();
                            return new AuraDtos.ConstraintWeightResponse(
                                    entry.getKey(),
                                    entry.getValue().name(),
                                    weight,
                                    custom == null || custom.active(),
                                    custom != null);
                        })
                        .sorted(java.util.Comparator.comparing(
                                AuraDtos.ConstraintWeightResponse::constraintLevel)
                                .thenComparing(
                                        AuraDtos.ConstraintWeightResponse::constraintName))
                        .toList();
        return new AuraDtos.ConstraintProfileResponse(termId, profile, weights);
    }

    @Transactional
    public AuraDtos.ConstraintProfileResponse replaceConstraintProfile(
            UUID userId,
            UUID termId,
            AuraDtos.UpsertConstraintProfileRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.TERM,
                termId,
                universityId,
                "AURA term was not found.");
        String profile = normalizeConstraintProfile(request.profile());
        Map<String, AuraDtos.ConstraintWeightRequest> unique = new LinkedHashMap<>();
        for (AuraDtos.ConstraintWeightRequest weight : request.weights()) {
            Level expected = AuraConstraintCatalog.constraints().get(
                    weight.constraintName());
            if (expected == null) {
                throw new AuraStateException("Unknown AURA constraint name.");
            }
            Level supplied;
            try {
                supplied = Level.valueOf(weight.constraintLevel()
                        .trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new AuraStateException("Unsupported AURA constraint level.");
            }
            if (supplied != expected) {
                throw new AuraStateException(
                        "A constraint cannot be moved to a different score level.");
            }
            if (unique.putIfAbsent(weight.constraintName(), weight) != null) {
                throw new AuraStateException(
                        "Each constraint may be configured only once per profile.");
            }
        }
        repository.replaceConstraintConfigurations(
                termId,
                profile,
                unique.values().stream()
                        .map(weight -> new ConstraintConfigurationRow(
                                weight.constraintName(),
                                Level.valueOf(weight.constraintLevel()
                                        .trim().toUpperCase(Locale.ROOT)),
                                weight.weight(),
                                weight.active()))
                        .toList());
        repository.insertTermAudit(
                termId,
                userId,
                "CONSTRAINT_PROFILE_UPDATED",
                "CONSTRAINT_PROFILE",
                termId,
                "Updated the " + profile + " constraint profile.");
        return constraintProfile(userId, termId, profile);
    }

    public OfferingResponse createOffering(
            UUID userId,
            AuraDtos.CreateOfferingRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.TERM,
                request.termId(),
                universityId,
                "AURA term was not found.");
        requireScopedResource(
                ScopedResource.SECTION,
                request.sectionId(),
                universityId,
                "Section was not found.");
        requireScopedResource(
                ScopedResource.INSTRUCTOR,
                request.instructorId(),
                universityId,
                "Instructor was not found.");
        if (!repository.courseBelongsToUniversity(
                request.courseId(),
                universityId)) {
            throw notFound("Course was not found.");
        }
        UUID id = repository.insertOffering(UUID.randomUUID(), request);
        return repository.listOfferings(request.termId()).stream()
                .filter(offering -> offering.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Offering was not found."));
    }

    public List<OfferingResponse> listOfferings(UUID userId, UUID termId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.TERM,
                termId,
                universityId,
                "AURA term was not found.");
        return repository.listOfferings(termId);
    }

    @Transactional
    public MeetingRequirementResponse createMeetingRequirement(
            UUID userId,
            AuraDtos.CreateMeetingRequirementRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.OFFERING,
                request.offeringId(),
                universityId,
                "Course offering was not found.");
        UUID id = repository.insertMeetingRequirement(UUID.randomUUID(), request);
        repository.replaceRequirementFacilities(
                id,
                normalizeFacilities(request.requiredFacilities()));
        return repository.listMeetingRequirements(request.offeringId()).stream()
                .filter(requirement -> requirement.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Meeting requirement was not found."));
    }

    public List<MeetingRequirementResponse> listMeetingRequirements(
            UUID userId,
            UUID offeringId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.OFFERING,
                offeringId,
                universityId,
                "Course offering was not found.");
        return repository.listMeetingRequirements(offeringId);
    }

    @Transactional
    public AuraDtos.FacilitySetResponse replaceRoomFacilities(
            UUID userId,
            UUID roomId,
            AuraDtos.ReplaceFacilitiesRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.ROOM,
                roomId,
                universityId,
                "Room was not found.");
        Set<String> facilities = normalizeFacilities(request.facilities());
        repository.replaceRoomFacilities(roomId, facilities);
        return new AuraDtos.FacilitySetResponse(
                roomId,
                repository.listRoomFacilities(roomId));
    }

    @Transactional
    public AuraDtos.FacilitySetResponse replaceRequirementFacilities(
            UUID userId,
            UUID requirementId,
            AuraDtos.ReplaceFacilitiesRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.REQUIREMENT,
                requirementId,
                universityId,
                "Meeting requirement was not found.");
        Set<String> facilities = normalizeFacilities(request.facilities());
        repository.replaceRequirementFacilities(requirementId, facilities);
        return new AuraDtos.FacilitySetResponse(
                requirementId,
                repository.listRequirementFacilities(requirementId));
    }

    @Transactional
    public AuraDtos.CalendarExceptionResponse createCalendarException(
            UUID userId,
            AuraDtos.CreateCalendarExceptionRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.TERM,
                request.termId(),
                universityId,
                "AURA term was not found.");
        String type = validateCalendarException(request, universityId);
        String facility = normalizeOptionalFacility(request.facility());
        UUID id = repository.insertCalendarException(
                UUID.randomUUID(),
                userId,
                request,
                type,
                facility);
        return repository.findCalendarException(id)
                .orElseThrow(() -> notFound("Calendar exception was not found."));
    }

    public List<AuraDtos.CalendarExceptionResponse> listCalendarExceptions(
            UUID userId,
            UUID termId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.TERM,
                termId,
                universityId,
                "AURA term was not found.");
        return repository.listCalendarExceptions(termId);
    }

    @Transactional
    public AuraDtos.CalendarExceptionResponse updateCalendarException(
            UUID userId,
            UUID exceptionId,
            AuraDtos.UpdateCalendarExceptionRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.CALENDAR_EXCEPTION,
                exceptionId,
                universityId,
                "Calendar exception was not found.");
        AuraDtos.CalendarExceptionResponse existing = repository
                .findCalendarException(exceptionId)
                .orElseThrow(() -> notFound("Calendar exception was not found."));
        AuraDtos.CreateCalendarExceptionRequest validationRequest =
                new AuraDtos.CreateCalendarExceptionRequest(
                        existing.termId(),
                        request.exceptionType(),
                        request.startsOn(),
                        request.endsOn(),
                        request.instructorId(),
                        request.roomId(),
                        request.sectionId(),
                        request.timeslotId(),
                        request.facility(),
                        request.reason());
        String type = validateCalendarException(validationRequest, universityId);
        if (!repository.updateCalendarException(
                exceptionId,
                request,
                type,
                normalizeOptionalFacility(request.facility()))) {
            throw new AuraStateException(
                    "This calendar exception changed while you were editing it. Refresh and try again.");
        }
        return repository.findCalendarException(exceptionId)
                .orElseThrow(() -> notFound("Calendar exception was not found."));
    }

    @Transactional
    public void deactivateCalendarException(
            UUID userId,
            UUID exceptionId,
            long version) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.CALENDAR_EXCEPTION,
                exceptionId,
                universityId,
                "Calendar exception was not found.");
        if (!repository.deactivateCalendarException(exceptionId, version)) {
            throw new AuraStateException(
                    "This calendar exception changed while you were editing it. Refresh and try again.");
        }
    }

    public ReadinessResponse readiness(UUID userId, UUID termId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.TERM,
                termId,
                universityId,
                "AURA term was not found.");
        return readinessValidator.validate(termId);
    }

    public GenerationRunResponse startGeneration(
            UUID userId,
            UUID termId,
            AuraDtos.GenerateTimetableRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.TERM,
                termId,
                universityId,
                "AURA term was not found.");
        ReadinessResponse readiness = readinessValidator.validate(termId);
        if (!readiness.ready()) {
            throw new AuraStateException(
                    "AURA is not ready to generate this timetable yet.");
        }
        repository.expireAbandonedGenerationRuns(
                clock.instant().minus(Duration.ofMinutes(10)));
        if (repository.hasActiveGenerationRun(termId)) {
            throw new AuraStateException(
                    "A generation run is already active for this term.");
        }
        int terminationSeconds = request.terminationSeconds() == null
                ? 30
                : Math.max(1, Math.min(request.terminationSeconds(), 300));
        String profile = normalizeConstraintProfile(request.profile());
        long randomSeed = request.randomSeed() == null ? 0L : request.randomSeed();
        UUID revisionId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        try {
            repository.createGenerationRun(
                    runId,
                    revisionId,
                    termId,
                    checksum(repository.schedulingInputSnapshot(termId)),
                    "AURA readiness snapshot: "
                            + readiness.meetingRequirements()
                            + " meeting requirements.",
                    userId,
                    terminationSeconds,
                    profile,
                    randomSeed);
        } catch (DataIntegrityViolationException exception) {
            throw new AuraStateException(
                    "A generation run is already active for this term.");
        }
        FutureTask<Void> task = new FutureTask<>(() -> {
            executeGeneration(
                    runId,
                    termId,
                    userId,
                    terminationSeconds,
                    request.notes(),
                    profile,
                    randomSeed);
            return null;
        });
        runningRuns.put(runId, task);
        generationExecutor.execute(task);
        return getRun(userId, runId);
    }

    public GenerationRunResponse getRun(UUID userId, UUID runId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.RUN,
                runId,
                universityId,
                "AURA generation run was not found.");
        return repository.findRun(runId)
                .orElseThrow(() -> notFound("AURA generation run was not found."));
    }

    public GenerationRunResponse cancelRun(UUID userId, UUID runId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.RUN,
                runId,
                universityId,
                "AURA generation run was not found.");
        Future<?> future = runningRuns.remove(runId);
        if (future != null) {
            future.cancel(true);
        }
        repository.cancelRun(runId);
        return getRun(userId, runId);
    }

    public List<TimetableVersionResponse> listVersions(
            UUID userId,
            UUID termId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.TERM,
                termId,
                universityId,
                "AURA term was not found.");
        return repository.listVersions(termId);
    }

    public TimetableVersionResponse getVersion(UUID userId, UUID versionId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.VERSION,
                versionId,
                universityId,
                "AURA timetable version was not found.");
        return repository.findVersion(versionId)
                .orElseThrow(() -> notFound("AURA timetable version was not found."));
    }

    @Transactional
    public TimetableVersionResponse publishVersion(
            UUID userId,
            UUID versionId) {
        TimetableVersionResponse version = getVersion(userId, versionId);
        if (!"DRAFT".equals(version.status())) {
            throw new AuraStateException(
                    "Only draft timetable versions can be published.");
        }
        if (repository.isVersionStale(versionId)) {
            throw new AuraStateException(
                    "This timetable was created from older scheduling data. Clone or regenerate it before publishing.");
        }
        long expectedOccurrences = repository.countExpectedOccurrences(
                version.termId());
        long scheduledOccurrences = repository.countScheduledSessions(versionId);
        if (scheduledOccurrences != expectedOccurrences) {
            throw new AuraStateException(
                    "Schedule every required session before publishing this timetable.");
        }
        if (repository.countOccurrenceIntegrityViolations(versionId) > 0) {
            throw new AuraStateException(
                    "Each meeting requirement must contain exactly its required weekly occurrences before publishing.");
        }
        long openHardClashes = repository.countOpenHardClashes(versionId);
        if (openHardClashes > 0) {
            throw new AuraStateException(
                    "Resolve all hard timetable clashes before publishing.");
        }
        if (!repository.publishVersion(versionId, version.termId())) {
            throw new AuraStateException(
                    "This timetable changed while it was being published. Refresh and try again.");
        }
        repository.insertVersionAudit(
                versionId,
                userId,
                "TIMETABLE_PUBLISHED",
                "Published timetable version " + version.versionNumber() + ".");
        notificationService.notifyTimetablePublished(
                repository.activeStudentUserIds(version.termId()),
                userId,
                versionId);
        return getVersion(userId, versionId);
    }

    @Transactional
    public TimetableVersionResponse cloneVersion(
            UUID userId,
            UUID sourceVersionId,
            AuraDtos.CloneVersionRequest request) {
        TimetableVersionResponse source = getVersion(userId, sourceVersionId);
        UUID cloneId = repository.cloneVersion(
                UUID.randomUUID(),
                sourceVersionId,
                repository.nextVersionNumber(source.termId()),
                request.notes(),
                userId,
                "MANUAL");
        refreshClashes(cloneId);
        repository.insertVersionAudit(
                cloneId,
                userId,
                "TIMETABLE_CLONED",
                "Created an editable timetable draft from version "
                        + source.versionNumber() + ".");
        return getVersion(userId, cloneId);
    }

    public AuraDtos.VersionComparisonResponse compareVersions(
            UUID userId,
            UUID baseVersionId,
            UUID comparedVersionId) {
        TimetableVersionResponse base = getVersion(userId, baseVersionId);
        TimetableVersionResponse compared = getVersion(userId, comparedVersionId);
        if (!base.termId().equals(compared.termId())) {
            throw new AuraStateException(
                    "Timetable versions can only be compared within the same academic term.");
        }
        Map<String, VersionSessionSnapshot> before = snapshotsByOccurrence(
                repository.versionSessionSnapshots(baseVersionId));
        Map<String, VersionSessionSnapshot> after = snapshotsByOccurrence(
                repository.versionSessionSnapshots(comparedVersionId));
        Set<String> keys = new LinkedHashSet<>(before.keySet());
        keys.addAll(after.keySet());
        List<AuraDtos.VersionSessionChange> changes = keys.stream()
                .map(key -> toVersionChange(before.get(key), after.get(key)))
                .filter(AuraDtos.VersionSessionChange::assignmentChanged)
                .toList();
        int added = (int) changes.stream()
                .filter(change -> change.beforeSessionId() == null)
                .count();
        int removed = (int) changes.stream()
                .filter(change -> change.afterSessionId() == null)
                .count();
        return new AuraDtos.VersionComparisonResponse(
                baseVersionId,
                comparedVersionId,
                keys.size(),
                changes.size(),
                added,
                removed,
                changes);
    }

    public List<SessionResponse> listSessions(UUID userId, UUID versionId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.VERSION,
                versionId,
                universityId,
                "AURA timetable version was not found.");
        return repository.listSessions(versionId);
    }

    public List<ClashResponse> listClashes(UUID userId, UUID versionId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.VERSION,
                versionId,
                universityId,
                "AURA timetable version was not found.");
        return repository.listClashes(versionId);
    }

    public AuraDtos.ManualMovePreviewResponse previewMove(
            UUID userId,
            UUID sessionId,
            AuraDtos.ManualMovePreviewRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.SESSION,
                sessionId,
                universityId,
                "AURA session was not found.");
        requireScopedResource(
                ScopedResource.ROOM,
                request.roomId(),
                universityId,
                "Room was not found.");
        requireScopedResource(
                ScopedResource.TIMESLOT,
                request.timeslotId(),
                universityId,
                "Timeslot was not found.");
        SessionResponse session = repository.findSession(sessionId)
                .orElseThrow(() -> notFound("AURA session was not found."));
        TimetableVersionResponse version = repository
                .findVersion(session.versionId())
                .orElseThrow(() -> notFound("AURA timetable version was not found."));
        if (!"DRAFT".equals(version.status())) {
            throw new AuraStateException(
                    "Published and archived timetable versions cannot be changed.");
        }
        if (session.locked()) {
            throw new AuraStateException(
                    "Unlock this session before changing its assignment.");
        }
        if (!repository.roomMeetsRequirement(
                request.roomId(),
                session.meetingRequirementId())) {
            return new AuraDtos.ManualMovePreviewResponse(
                    false,
                    "The selected room does not meet this session's capacity, type, or facility requirements.",
                    List.of());
        }
        if (!repository.assignmentMeetsHardRestrictions(
                sessionId, request.roomId(), request.timeslotId())) {
            return new AuraDtos.ManualMovePreviewResponse(
                    false,
                    "The selected assignment conflicts with a fixed rule, availability, or contiguous-duration requirement.",
                    List.of());
        }
        RoomResponse room = repository.findRoom(request.roomId())
                .orElseThrow(() -> notFound("Room was not found."));
        TimeslotResponse timeslot = repository.findTimeslot(request.timeslotId())
                .orElseThrow(() -> notFound("Timeslot was not found."));
        SessionResponse replacement = withAssignment(session, room, timeslot);
        List<DetectedClash> detected = clashDetector.previewMove(
                repository.listSessions(session.versionId()),
                replacement);
        List<ClashResponse> clashes =
                toPreviewClashes(session.versionId(), detected);
        boolean allowed = clashes.isEmpty();
        return new AuraDtos.ManualMovePreviewResponse(
                allowed,
                allowed
                        ? "This move does not create a hard clash."
                        : "This move would create a clash.",
                clashes);
    }

    @Transactional
    public SessionResponse applyMove(
            UUID userId,
            UUID sessionId,
            AuraDtos.ManualMoveRequest request) {
        AuraDtos.ManualMovePreviewResponse preview = previewMove(
                userId,
                sessionId,
                new AuraDtos.ManualMovePreviewRequest(
                        request.roomId(),
                        request.timeslotId()));
        if (!preview.allowed()) {
            throw new AuraStateException(
                    "The requested move would create a timetable clash.");
        }
        SessionResponse before = repository.findSession(sessionId)
                .orElseThrow(() -> notFound("AURA session was not found."));
        boolean moved = repository.moveSession(
                sessionId,
                request.roomId(),
                request.timeslotId(),
                userId,
                request.reason());
        if (!moved) {
            throw new AuraStateException(
                    "This draft changed while the move was being applied. Refresh and try again.");
        }
        refreshClashes(before.versionId());
        repository.insertVersionAudit(
                before.versionId(),
                userId,
                "SESSION_MOVED",
                "Moved one scheduled session after a clash-safe preview.");
        return repository.findSession(sessionId)
                .orElseThrow(() -> notFound("AURA session was not found."));
    }

    public AuraDtos.ManualMovePreviewResponse previewSwap(
            UUID userId,
            UUID sessionId,
            AuraDtos.SessionSwapPreviewRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(ScopedResource.SESSION, sessionId, universityId,
                "AURA session was not found.");
        requireScopedResource(ScopedResource.SESSION, request.otherSessionId(),
                universityId, "AURA session was not found.");
        if (sessionId.equals(request.otherSessionId())) {
            throw new AuraStateException("Choose two different sessions to swap.");
        }
        SessionResponse first = repository.findSession(sessionId)
                .orElseThrow(() -> notFound("AURA session was not found."));
        SessionResponse second = repository.findSession(request.otherSessionId())
                .orElseThrow(() -> notFound("AURA session was not found."));
        if (!first.versionId().equals(second.versionId())) {
            throw new AuraStateException(
                    "Sessions can only be swapped within the same timetable draft.");
        }
        TimetableVersionResponse version = repository.findVersion(first.versionId())
                .orElseThrow(() -> notFound("AURA timetable version was not found."));
        if (!"DRAFT".equals(version.status())) {
            throw new AuraStateException(
                    "Published and archived timetable versions cannot be changed.");
        }
        if (first.locked() || second.locked()) {
            throw new AuraStateException("Unlock both sessions before swapping them.");
        }
        if (!repository.roomMeetsRequirement(
                second.roomId(), first.meetingRequirementId())
                || !repository.roomMeetsRequirement(
                        first.roomId(), second.meetingRequirementId())) {
            return new AuraDtos.ManualMovePreviewResponse(
                    false,
                    "One of the rooms does not meet the swapped session's requirements.",
                    List.of());
        }
        if (!repository.assignmentMeetsHardRestrictions(
                first.id(), second.roomId(), second.timeslotId())
                || !repository.assignmentMeetsHardRestrictions(
                        second.id(), first.roomId(), first.timeslotId())) {
            return new AuraDtos.ManualMovePreviewResponse(
                    false,
                    "The swap conflicts with a fixed rule, availability, or contiguous-duration requirement.",
                    List.of());
        }
        List<SessionResponse> sessions = repository.listSessions(first.versionId());
        List<DetectedClash> baseline = clashDetector.detect(sessions);
        SessionResponse movedFirst = withAssignment(first, second);
        SessionResponse movedSecond = withAssignment(second, first);
        List<SessionResponse> swapped = sessions.stream()
                .map(session -> session.id().equals(first.id())
                        ? movedFirst
                        : session.id().equals(second.id()) ? movedSecond : session)
                .toList();
        Set<String> baselineKeys = baseline.stream()
                .map(this::clashKey)
                .collect(java.util.stream.Collectors.toSet());
        List<DetectedClash> newClashes = clashDetector.detect(swapped).stream()
                .filter(clash -> !baselineKeys.contains(clashKey(clash)))
                .toList();
        return new AuraDtos.ManualMovePreviewResponse(
                newClashes.isEmpty(),
                newClashes.isEmpty()
                        ? "This swap does not create a new hard clash."
                        : "This swap would create a new clash.",
                toPreviewClashes(first.versionId(), newClashes));
    }

    @Transactional
    public List<SessionResponse> applySwap(
            UUID userId,
            UUID sessionId,
            AuraDtos.SessionSwapRequest request) {
        AuraDtos.ManualMovePreviewResponse preview = previewSwap(
                userId,
                sessionId,
                new AuraDtos.SessionSwapPreviewRequest(request.otherSessionId()));
        if (!preview.allowed()) {
            throw new AuraStateException(
                    "The requested swap would create a timetable clash.");
        }
        SessionResponse first = repository.findSession(sessionId)
                .orElseThrow(() -> notFound("AURA session was not found."));
        if (!repository.swapSessionAssignments(
                sessionId, request.otherSessionId(), userId, request.reason())) {
            throw new AuraStateException(
                    "This draft changed while the swap was being applied. Refresh and try again.");
        }
        refreshClashes(first.versionId());
        repository.insertVersionAudit(
                first.versionId(), userId, "SESSIONS_SWAPPED",
                "Swapped two scheduled sessions after a clash-safe preview.");
        return List.of(
                repository.findSession(sessionId).orElseThrow(),
                repository.findSession(request.otherSessionId()).orElseThrow());
    }

    @Transactional
    public SessionResponse setSessionPinned(
            UUID userId,
            UUID sessionId,
            AuraDtos.SessionPinRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(ScopedResource.SESSION, sessionId, universityId,
                "AURA session was not found.");
        SessionResponse session = repository.findSession(sessionId)
                .orElseThrow(() -> notFound("AURA session was not found."));
        TimetableVersionResponse version = repository.findVersion(session.versionId())
                .orElseThrow(() -> notFound("AURA timetable version was not found."));
        if (!"DRAFT".equals(version.status())) {
            throw new AuraStateException(
                    "Only sessions in draft timetable versions can be pinned or unpinned.");
        }
        if (request.pinned()
                && (request.reason() == null || request.reason().isBlank())) {
            throw new AuraStateException("Add a reason before pinning this session.");
        }
        if (!repository.setSessionPinned(
                sessionId, request.pinned(), request.reason())) {
            throw new AuraStateException(
                    "This draft changed while the session was being updated. Refresh and try again.");
        }
        repository.insertVersionAudit(
                session.versionId(),
                userId,
                request.pinned() ? "SESSION_PINNED" : "SESSION_UNPINNED",
                request.pinned()
                        ? "Pinned one scheduled session."
                        : "Unpinned one scheduled session.");
        return repository.findSession(sessionId)
                .orElseThrow(() -> notFound("AURA session was not found."));
    }

    @Transactional
    public TimetableVersionResponse archiveVersion(UUID userId, UUID versionId) {
        TimetableVersionResponse version = getVersion(userId, versionId);
        if (!"DRAFT".equals(version.status())) {
            throw new AuraStateException("Only draft timetable versions can be archived.");
        }
        if (!repository.archiveVersion(versionId)) {
            throw new AuraStateException(
                    "This timetable changed while it was being archived. Refresh and try again.");
        }
        repository.insertVersionAudit(
                versionId,
                userId,
                "TIMETABLE_ARCHIVED",
                "Archived timetable version " + version.versionNumber() + ".");
        return getVersion(userId, versionId);
    }

    public AuraDtos.AuraMetricsResponse metrics(UUID userId, UUID termId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireScopedResource(
                ScopedResource.TERM,
                termId,
                universityId,
                "AURA term was not found.");
        return repository.metrics(termId);
    }

    private void executeGeneration(
            UUID runId,
            UUID termId,
            UUID userId,
            int terminationSeconds,
            String notes,
            String profile,
            long randomSeed) {
        try {
            if (!repository.markRunRunning(runId, clock.instant())) {
                return;
            }
            AuraSolverService.SolverResult result = solverService.solve(
                    repository.solverRequirements(termId),
                    repository.solverRooms(termId),
                    repository.solverTimeslots(termId),
                    repository.solverInstructorAvailability(termId),
                    repository.solverRoomAvailability(termId),
                    repository.solverSectionAvailability(termId),
                    repository.solverStudentAvailability(termId),
                    repository.solverTravelRules(termId),
                    terminationSeconds,
                    AuraConstraintCatalog.weights(
                            profile,
                            repository.constraintWeights(termId, profile)),
                    randomSeed);
            generationPersistenceService.persistCompletedRun(
                    runId,
                    termId,
                    userId,
                    result.score(),
                    notes,
                    result.assignments(),
                    result.candidateCount(),
                    result.terminationReason());
        } catch (RuntimeException exception) {
            LOG.error(
                    "AURA generation run {} failed safely: {}",
                    runId,
                    exception.getMessage(),
                    exception);
            try {
                repository.markRunFailed(
                        runId,
                        "Generation failed before a valid timetable could be saved.");
            } catch (RuntimeException statusException) {
                LOG.error(
                        "AURA generation run {} status could not be updated: {}",
                        runId,
                        statusException.getMessage());
            }
        } finally {
            runningRuns.remove(runId);
        }
    }

    private void refreshClashes(UUID versionId) {
        List<DetectedClash> detected =
                clashDetector.detect(repository.listSessions(versionId));
        repository.replaceClashes(versionId, detected);
    }

    private String normalizeConstraintProfile(String profile) {
        try {
            return AuraConstraintCatalog.normalizeProfile(profile);
        } catch (IllegalArgumentException exception) {
            throw new AuraStateException(exception.getMessage());
        }
    }

    private long scoreWeight(
            ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore score,
            Level level) {
        return switch (level) {
            case HARD -> score.hardScore();
            case MEDIUM -> score.mediumScore();
            case SOFT -> score.softScore();
        };
    }

    private Map<String, VersionSessionSnapshot> snapshotsByOccurrence(
            List<VersionSessionSnapshot> snapshots) {
        Map<String, VersionSessionSnapshot> indexed = new LinkedHashMap<>();
        snapshots.forEach(snapshot -> indexed.put(
                snapshot.meetingRequirementId() + ":" + snapshot.occurrenceIndex(),
                snapshot));
        return indexed;
    }

    private AuraDtos.VersionSessionChange toVersionChange(
            VersionSessionSnapshot before,
            VersionSessionSnapshot after) {
        VersionSessionSnapshot available = before == null ? after : before;
        boolean changed = before == null
                || after == null
                || !before.roomId().equals(after.roomId())
                || !before.timeslotId().equals(after.timeslotId())
                || !before.instructorId().equals(after.instructorId())
                || !before.sectionId().equals(after.sectionId());
        return new AuraDtos.VersionSessionChange(
                available.meetingRequirementId(),
                available.occurrenceIndex(),
                before == null ? null : before.sessionId(),
                after == null ? null : after.sessionId(),
                before == null ? null : before.roomId(),
                after == null ? null : after.roomId(),
                before == null ? null : before.timeslotId(),
                after == null ? null : after.timeslotId(),
                changed);
    }

    private SessionResponse withAssignment(
            SessionResponse session,
            SessionResponse assignmentSource) {
        return new SessionResponse(
                session.id(),
                session.versionId(),
                session.offeringId(),
                session.meetingRequirementId(),
                session.courseCode(),
                session.courseTitle(),
                session.sectionId(),
                session.sectionName(),
                session.instructorId(),
                session.instructorName(),
                assignmentSource.roomId(),
                assignmentSource.roomName(),
                assignmentSource.roomType(),
                assignmentSource.timeslotId(),
                assignmentSource.dayOfWeek(),
                assignmentSource.startsAt(),
                repository.assignmentEndTime(
                        assignmentSource.timeslotId(), session.durationSlots()),
                session.locked(),
                "MANUAL",
                session.occurrenceIndex(),
                session.sessionsPerWeek(),
                session.durationSlots(),
                session.durationSlots(),
                session.requiredCapacity(),
                session.requiredRoomType(),
                assignmentSource.roomCapacity(),
                session.requiredFacilities(),
                assignmentSource.roomFacilities(),
                assignmentSource.timeslotType(),
                assignmentSource.roomActive(),
                session.instructorActive(),
                session.sectionActive(),
                session.fixedRoomId(),
                session.fixedTimeslotId(),
                session.weekPattern(),
                session.customWeeks(),
                session.hardConflictOfferingIds(),
                false,
                false,
                false,
                false);
    }

    private SessionResponse withAssignment(
            SessionResponse session,
            RoomResponse room,
            TimeslotResponse timeslot) {
        return new SessionResponse(
                session.id(), session.versionId(), session.offeringId(),
                session.meetingRequirementId(), session.courseCode(),
                session.courseTitle(), session.sectionId(), session.sectionName(),
                session.instructorId(), session.instructorName(), room.id(),
                room.name(), room.roomType(), timeslot.id(), timeslot.dayOfWeek(),
                timeslot.startsAt(), repository.assignmentEndTime(
                        timeslot.id(), session.durationSlots()), false, "MANUAL",
                session.occurrenceIndex(), session.sessionsPerWeek(),
                session.durationSlots(), session.durationSlots(),
                session.requiredCapacity(), session.requiredRoomType(), room.capacity(),
                session.requiredFacilities(), room.facilities(), "INSTRUCTIONAL",
                room.active(), session.instructorActive(), session.sectionActive(),
                session.fixedRoomId(), session.fixedTimeslotId(),
                session.weekPattern(), session.customWeeks(),
                session.hardConflictOfferingIds(), false, false, false, false);
    }

    private String clashKey(DetectedClash clash) {
        String first = String.valueOf(clash.primarySessionId());
        String second = String.valueOf(clash.secondarySessionId());
        return first.compareTo(second) <= 0
                ? clash.clashType() + ":" + first + ":" + second
                : clash.clashType() + ":" + second + ":" + first;
    }

    private void requireRequestedUniversity(
            UUID requestedUniversityId,
            UUID adminUniversityId) {
        if (!adminUniversityId.equals(requestedUniversityId)) {
            throw notFound("University scheduling data was not found.");
        }
    }

    private void requireOptionalRequestedUniversity(
            UUID requestedUniversityId,
            UUID adminUniversityId) {
        if (requestedUniversityId != null) {
            requireRequestedUniversity(requestedUniversityId, adminUniversityId);
        }
    }

    private void requireScopedResource(
            ScopedResource resource,
            UUID resourceId,
            UUID universityId,
            String notFoundMessage) {
        if (!repository.resourceBelongsToUniversity(
                resource,
                resourceId,
                universityId)) {
            throw notFound(notFoundMessage);
        }
    }

    private Set<String> normalizeFacilities(List<String> facilities) {
        if (facilities == null || facilities.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        facilities.forEach(value -> {
            String facility = normalizeEnumValue(value);
            if (!FACILITIES.contains(facility)) {
                throw new AuraStateException(
                        "Choose a supported room facility.");
            }
            normalized.add(facility);
        });
        return normalized;
    }

    private String normalizeOptionalFacility(String facility) {
        if (facility == null || facility.isBlank()) {
            return null;
        }
        String normalized = normalizeEnumValue(facility);
        if (!FACILITIES.contains(normalized)) {
            throw new AuraStateException("Choose a supported room facility.");
        }
        return normalized;
    }

    private String validateCalendarException(
            AuraDtos.CreateCalendarExceptionRequest request,
            UUID universityId) {
        String type = normalizeEnumValue(request.exceptionType());
        if (!CALENDAR_EXCEPTION_TYPES.contains(type)) {
            throw new AuraStateException("Choose a supported calendar exception type.");
        }
        if (request.startsOn().isAfter(request.endsOn())) {
            throw new AuraStateException(
                    "Calendar exception start date must be before the end date.");
        }
        TermResponse term = repository.findTerm(request.termId())
                .orElseThrow(() -> notFound("AURA term was not found."));
        if (request.startsOn().isBefore(term.startsOn())
                || request.endsOn().isAfter(term.endsOn())) {
            throw new AuraStateException(
                    "Calendar exception dates must fall within the academic term.");
        }
        if (request.instructorId() != null) {
            requireScopedResource(
                    ScopedResource.INSTRUCTOR,
                    request.instructorId(),
                    universityId,
                    "Instructor was not found.");
        }
        if (request.roomId() != null) {
            requireScopedResource(
                    ScopedResource.ROOM,
                    request.roomId(),
                    universityId,
                    "Room was not found.");
        }
        if (request.sectionId() != null) {
            requireScopedResource(
                    ScopedResource.SECTION,
                    request.sectionId(),
                    universityId,
                    "Section was not found.");
        }
        if (request.timeslotId() != null) {
            requireScopedResource(
                    ScopedResource.TIMESLOT,
                    request.timeslotId(),
                    universityId,
                    "Timeslot was not found.");
        }
        validateCalendarExceptionTarget(type, request);
        return type;
    }

    private void validateCalendarExceptionTarget(
            String type,
            AuraDtos.CreateCalendarExceptionRequest request) {
        boolean valid = switch (type) {
            case "HOLIDAY", "NON_TEACHING_DAY", "UNIVERSITY_EVENT" ->
                    request.instructorId() == null
                            && request.roomId() == null
                            && request.sectionId() == null
                            && request.facility() == null;
            case "INSTRUCTOR_ABSENCE" -> request.instructorId() != null
                    && request.roomId() == null
                    && request.sectionId() == null
                    && request.facility() == null;
            case "ROOM_CLOSURE" -> request.roomId() != null
                    && request.instructorId() == null
                    && request.sectionId() == null
                    && request.facility() == null;
            case "SECTION_RESTRICTION" -> request.sectionId() != null
                    && request.instructorId() == null
                    && request.roomId() == null
                    && request.facility() == null;
            case "TIMESLOT_CANCELLATION" -> request.timeslotId() != null
                    && request.instructorId() == null
                    && request.roomId() == null
                    && request.sectionId() == null
                    && request.facility() == null;
            case "FACILITY_OUTAGE" -> request.roomId() != null
                    && request.facility() != null
                    && !request.facility().isBlank()
                    && request.instructorId() == null
                    && request.sectionId() == null;
            default -> false;
        };
        if (!valid) {
            throw new AuraStateException(
                    "Choose the resource that matches this calendar exception type.");
        }
    }

    private String normalizeEnumValue(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private List<ClashResponse> toPreviewClashes(
            UUID versionId,
            List<DetectedClash> clashes) {
        Instant now = clock.instant();
        return clashes.stream()
                .map(clash -> new ClashResponse(
                        UUID.randomUUID(),
                        versionId,
                        clash.clashType(),
                        clash.severity(),
                        clash.message(),
                        clash.primarySessionId(),
                        clash.secondarySessionId(),
                        now,
                        null))
                .toList();
    }

    private ResourceNotFoundException notFound(String message) {
        String suffix = " was not found.";
        String resourceName = message.endsWith(suffix)
                ? message.substring(0, message.length() - suffix.length())
                : message;
        return new ResourceNotFoundException(resourceName);
    }

    private String normalizeAvailability(String value) {
        String normalized = value == null
                ? ""
                : value.trim().toUpperCase().replace('-', '_');
        if (!List.of("UNAVAILABLE", "AVOID", "PREFERRED", "AVAILABLE")
                .contains(normalized)) {
            throw new AuraStateException(
                    "Availability must be UNAVAILABLE, AVOID, or PREFERRED.");
        }
        return normalized;
    }

    private String checksum(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private <T> PageResponse<T> page(
            List<T> content,
            int page,
            int size,
            long totalElements) {
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                totalPages == 0 || page >= totalPages - 1);
    }
}
