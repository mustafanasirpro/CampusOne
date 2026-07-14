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
import com.campusone.aura.repository.AuraJdbcRepository.DetectedClash;
import com.campusone.aura.repository.AuraJdbcRepository.SolverAssignment;
import com.campusone.common.exception.ResourceNotFoundException;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraService {

    private final AuraAuthorizationService authorizationService;
    private final AuraJdbcRepository repository;
    private final AuraReadinessValidator readinessValidator;
    private final AuraSolverService solverService;
    private final AuraClashDetector clashDetector;
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
            Clock clock) {
        this.authorizationService = authorizationService;
        this.repository = repository;
        this.readinessValidator = readinessValidator;
        this.solverService = solverService;
        this.clashDetector = clashDetector;
        this.clock = clock;
    }

    @PreDestroy
    void shutdown() {
        generationExecutor.shutdownNow();
    }

    public TermResponse createTerm(
            UUID userId,
            AuraDtos.CreateTermRequest request) {
        authorizationService.requireAdmin(userId);
        if (request.startsOn().isAfter(request.endsOn())) {
            throw new AuraStateException("Term start date must be before the end date.");
        }
        UUID id = repository.insertTerm(UUID.randomUUID(), userId, request);
        return repository.findTerm(id)
                .orElseThrow(() -> notFound("AURA term was not found."));
    }

    public PageResponse<TermResponse> listTerms(UUID userId, int page, int size) {
        authorizationService.requireAdmin(userId);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        long total = repository.countTerms();
        List<TermResponse> content = repository.listTerms(safePage, safeSize);
        return page(content, safePage, safeSize, total);
    }

    public ProgramResponse createProgram(
            UUID userId,
            AuraDtos.CreateProgramRequest request) {
        authorizationService.requireAdmin(userId);
        UUID id = repository.insertProgram(UUID.randomUUID(), request);
        return repository.listPrograms(request.universityId()).stream()
                .filter(program -> program.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Program was not found."));
    }

    public List<ProgramResponse> listPrograms(UUID userId, UUID universityId) {
        authorizationService.requireAdmin(userId);
        return repository.listPrograms(universityId);
    }

    public BatchResponse createBatch(
            UUID userId,
            AuraDtos.CreateBatchRequest request) {
        authorizationService.requireAdmin(userId);
        UUID id = repository.insertBatch(UUID.randomUUID(), request);
        return repository.listBatches(request.programId()).stream()
                .filter(batch -> batch.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Batch was not found."));
    }

    public List<BatchResponse> listBatches(UUID userId, UUID programId) {
        authorizationService.requireAdmin(userId);
        return repository.listBatches(programId);
    }

    public SectionResponse createSection(
            UUID userId,
            AuraDtos.CreateSectionRequest request) {
        authorizationService.requireAdmin(userId);
        UUID id = repository.insertSection(UUID.randomUUID(), request);
        return repository.listSections(request.batchId()).stream()
                .filter(section -> section.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Section was not found."));
    }

    public List<SectionResponse> listSections(UUID userId, UUID batchId) {
        authorizationService.requireAdmin(userId);
        return repository.listSections(batchId);
    }

    public InstructorResponse createInstructor(
            UUID userId,
            AuraDtos.CreateInstructorRequest request) {
        authorizationService.requireAdmin(userId);
        UUID id = repository.insertInstructor(UUID.randomUUID(), request);
        return repository.listInstructors(request.universityId()).stream()
                .filter(instructor -> instructor.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Instructor was not found."));
    }

    public List<InstructorResponse> listInstructors(
            UUID userId,
            UUID universityId) {
        authorizationService.requireAdmin(userId);
        return repository.listInstructors(universityId);
    }

    public RoomResponse createRoom(
            UUID userId,
            AuraDtos.CreateRoomRequest request) {
        authorizationService.requireAdmin(userId);
        if (request.capacity() <= 0) {
            throw new AuraStateException("Room capacity must be greater than zero.");
        }
        UUID id = repository.insertRoom(UUID.randomUUID(), request);
        return repository.listRooms(request.universityId()).stream()
                .filter(room -> room.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Room was not found."));
    }

    public List<RoomResponse> listRooms(UUID userId, UUID universityId) {
        authorizationService.requireAdmin(userId);
        return repository.listRooms(universityId);
    }

    public TimeslotResponse createTimeslot(
            UUID userId,
            AuraDtos.CreateTimeslotRequest request) {
        authorizationService.requireAdmin(userId);
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
        authorizationService.requireAdmin(userId);
        return repository.listTimeslots(universityId);
    }

    public AuraDtos.AvailabilityResponse upsertInstructorAvailability(
            UUID userId,
            AuraDtos.CreateInstructorAvailabilityRequest request) {
        authorizationService.requireAdmin(userId);
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
        authorizationService.requireAdmin(userId);
        return repository.listInstructorAvailability(instructorId);
    }

    public AuraDtos.AvailabilityResponse upsertRoomAvailability(
            UUID userId,
            AuraDtos.CreateRoomAvailabilityRequest request) {
        authorizationService.requireAdmin(userId);
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
        authorizationService.requireAdmin(userId);
        return repository.listRoomAvailability(roomId);
    }

    public OfferingResponse createOffering(
            UUID userId,
            AuraDtos.CreateOfferingRequest request) {
        authorizationService.requireAdmin(userId);
        UUID id = repository.insertOffering(UUID.randomUUID(), request);
        return repository.listOfferings(request.termId()).stream()
                .filter(offering -> offering.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Offering was not found."));
    }

    public List<OfferingResponse> listOfferings(UUID userId, UUID termId) {
        authorizationService.requireAdmin(userId);
        ensureTerm(termId);
        return repository.listOfferings(termId);
    }

    public MeetingRequirementResponse createMeetingRequirement(
            UUID userId,
            AuraDtos.CreateMeetingRequirementRequest request) {
        authorizationService.requireAdmin(userId);
        UUID id = repository.insertMeetingRequirement(UUID.randomUUID(), request);
        return repository.listMeetingRequirements(request.offeringId()).stream()
                .filter(requirement -> requirement.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("Meeting requirement was not found."));
    }

    public List<MeetingRequirementResponse> listMeetingRequirements(
            UUID userId,
            UUID offeringId) {
        authorizationService.requireAdmin(userId);
        return repository.listMeetingRequirements(offeringId);
    }

    public ReadinessResponse readiness(UUID userId, UUID termId) {
        authorizationService.requireAdmin(userId);
        ensureTerm(termId);
        return readinessValidator.validate(termId);
    }

    public GenerationRunResponse startGeneration(
            UUID userId,
            UUID termId,
            AuraDtos.GenerateTimetableRequest request) {
        authorizationService.requireAdmin(userId);
        ensureTerm(termId);
        ReadinessResponse readiness = readinessValidator.validate(termId);
        if (!readiness.ready()) {
            throw new AuraStateException(
                    "AURA is not ready to generate this timetable yet.");
        }
        if (repository.hasActiveGenerationRun(termId)) {
            throw new AuraStateException(
                    "A generation run is already active for this term.");
        }
        int terminationSeconds = request.terminationSeconds() == null
                ? 30
                : Math.max(1, Math.min(request.terminationSeconds(), 300));
        UUID revisionId = repository.insertRevision(
                UUID.randomUUID(),
                termId,
                repository.nextRevisionNumber(termId),
                checksum(readiness.toString()),
                "AURA readiness snapshot: "
                        + readiness.meetingRequirements()
                        + " meeting requirements.",
                userId);
        UUID runId = repository.insertRun(
                UUID.randomUUID(),
                termId,
                revisionId,
                userId,
                terminationSeconds);
        Future<?> future = generationExecutor.submit(() ->
                executeGeneration(
                        runId,
                        termId,
                        userId,
                        terminationSeconds,
                        request.notes()));
        runningRuns.put(runId, future);
        return getRun(userId, runId);
    }

    public GenerationRunResponse getRun(UUID userId, UUID runId) {
        authorizationService.requireAdmin(userId);
        return repository.findRun(runId)
                .orElseThrow(() -> notFound("AURA generation run was not found."));
    }

    public GenerationRunResponse cancelRun(UUID userId, UUID runId) {
        authorizationService.requireAdmin(userId);
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
        authorizationService.requireAdmin(userId);
        ensureTerm(termId);
        return repository.listVersions(termId);
    }

    public TimetableVersionResponse getVersion(UUID userId, UUID versionId) {
        authorizationService.requireAdmin(userId);
        return repository.findVersion(versionId)
                .orElseThrow(() -> notFound("AURA timetable version was not found."));
    }

    public TimetableVersionResponse publishVersion(
            UUID userId,
            UUID versionId) {
        authorizationService.requireAdmin(userId);
        TimetableVersionResponse version = getVersion(userId, versionId);
        if (!"DRAFT".equals(version.status())) {
            throw new AuraStateException(
                    "Only draft timetable versions can be published.");
        }
        long openHardClashes = repository.countOpenHardClashes(versionId);
        if (openHardClashes > 0) {
            throw new AuraStateException(
                    "Resolve all hard timetable clashes before publishing.");
        }
        repository.publishVersion(versionId, version.termId());
        return getVersion(userId, versionId);
    }

    public List<SessionResponse> listSessions(UUID userId, UUID versionId) {
        authorizationService.requireAdmin(userId);
        ensureVersion(versionId);
        return repository.listSessions(versionId);
    }

    public List<ClashResponse> listClashes(UUID userId, UUID versionId) {
        authorizationService.requireAdmin(userId);
        ensureVersion(versionId);
        return repository.listClashes(versionId);
    }

    public AuraDtos.ManualMovePreviewResponse previewMove(
            UUID userId,
            UUID sessionId,
            AuraDtos.ManualMovePreviewRequest request) {
        authorizationService.requireAdmin(userId);
        SessionResponse session = repository.findSession(sessionId)
                .orElseThrow(() -> notFound("AURA session was not found."));
        List<DetectedClash> detected = clashDetector.previewMove(
                repository.listSessions(session.versionId()),
                sessionId,
                request.roomId(),
                request.timeslotId());
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

    public SessionResponse applyMove(
            UUID userId,
            UUID sessionId,
            AuraDtos.ManualMoveRequest request) {
        authorizationService.requireAdmin(userId);
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
        repository.moveSession(
                sessionId,
                request.roomId(),
                request.timeslotId(),
                userId,
                request.reason());
        refreshClashes(before.versionId());
        return repository.findSession(sessionId)
                .orElseThrow(() -> notFound("AURA session was not found."));
    }

    public AuraDtos.AuraMetricsResponse metrics(UUID userId, UUID termId) {
        authorizationService.requireAdmin(userId);
        ensureTerm(termId);
        return repository.metrics(termId);
    }

    private void executeGeneration(
            UUID runId,
            UUID termId,
            UUID userId,
            int terminationSeconds,
            String notes) {
        repository.markRunRunning(runId, clock.instant());
        try {
            AuraSolverService.SolverResult result = solverService.solve(
                    repository.solverRequirements(termId),
                    repository.solverRooms(termId),
                    repository.solverTimeslots(termId),
                    repository.solverInstructorAvailability(termId),
                    repository.solverRoomAvailability(termId),
                    terminationSeconds);
            UUID versionId = repository.insertVersion(
                    UUID.randomUUID(),
                    termId,
                    runId,
                    repository.nextVersionNumber(termId),
                    result.score(),
                    notes,
                    userId);
            for (SolverAssignment assignment : result.assignments()) {
                repository.insertSession(
                        UUID.randomUUID(),
                        versionId,
                        assignment,
                        "SOLVER");
            }
            refreshClashes(versionId);
            repository.markRunCompleted(
                    runId,
                    result.score(),
                    "Timetable generated with "
                            + result.assignments().size()
                            + " scheduled sessions.");
        } catch (RuntimeException exception) {
            repository.markRunFailed(
                    runId,
                    "Generation failed before a valid timetable could be saved.");
        } finally {
            runningRuns.remove(runId);
        }
    }

    private void refreshClashes(UUID versionId) {
        List<DetectedClash> detected =
                clashDetector.detect(repository.listSessions(versionId));
        repository.replaceClashes(versionId, detected);
    }

    private void ensureTerm(UUID termId) {
        if (repository.findTerm(termId).isEmpty()) {
            throw notFound("AURA term was not found.");
        }
    }

    private void ensureVersion(UUID versionId) {
        if (repository.findVersion(versionId).isEmpty()) {
            throw notFound("AURA timetable version was not found.");
        }
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
        return new ResourceNotFoundException(message);
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
