package com.campusone.aura.service;

import com.campusone.aura.dto.AuraDtos;
import com.campusone.aura.dto.AuraDtos.RoomResponse;
import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.dto.AuraDtos.TimeslotResponse;
import com.campusone.aura.dto.AuraDtos.TimetableVersionResponse;
import com.campusone.aura.dto.AuraScenarioDtos;
import com.campusone.aura.dto.AuraScenarioDtos.EmergencyRepairResponse;
import com.campusone.aura.dto.AuraScenarioDtos.WhatIfResponse;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.DetectedClash;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.repository.AuraScenarioRepository;
import com.campusone.aura.solver.AuraConstraintCatalog;
import com.campusone.common.exception.ResourceNotFoundException;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraScenarioService {

    private static final Set<String> WHAT_IF_TYPES = Set.of(
            "ROOM_UNAVAILABLE", "INSTRUCTOR_UNAVAILABLE",
            "TIMESLOT_REMOVED", "FACILITY_REMOVED");
    private static final Set<String> EMERGENCY_TYPES = Set.of(
            "ROOM_CLOSURE", "INSTRUCTOR_ABSENCE", "UNIVERSITY_EVENT",
            "TIMESLOT_CANCELLATION", "FACILITY_OUTAGE", "SECTION_RESTRICTION");

    private final AuraAuthorizationService authorizationService;
    private final AuraJdbcRepository repository;
    private final AuraScenarioRepository scenarioRepository;
    private final AuraClashDetector clashDetector;
    private final AuraService auraService;
    private final AuraSolverService solverService;

    public AuraScenarioService(
            AuraAuthorizationService authorizationService,
            AuraJdbcRepository repository,
            AuraScenarioRepository scenarioRepository,
            AuraClashDetector clashDetector,
            AuraService auraService,
            AuraSolverService solverService) {
        this.authorizationService = authorizationService;
        this.repository = repository;
        this.scenarioRepository = scenarioRepository;
        this.clashDetector = clashDetector;
        this.auraService = auraService;
        this.solverService = solverService;
    }

    @Transactional
    public WhatIfResponse runWhatIf(
            UUID userId,
            UUID termId,
            AuraScenarioDtos.WhatIfRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireTermAndVersion(termId, request.sourceVersionId(), universityId);
        String scenarioType = normalize(request.scenarioType(), WHAT_IF_TYPES,
                "Choose a supported what-if scenario.");
        UUID resourceId = optionalResourceId(request.scenarioInput());
        validateResourceScope(scenarioType, resourceId, universityId);
        int affected = scenarioRepository.countAffectedSessions(
                request.sourceVersionId(), scenarioType, resourceId);
        TimetableVersionResponse sourceVersion = repository
                .findVersion(request.sourceVersionId())
                .orElseThrow(() -> notFound(
                        "AURA timetable version was not found."));
        ScenarioComparison comparison = compareScenario(
                termId, request.sourceVersionId(), sourceVersion.score(),
                scenarioType, resourceId);
        String recommendation;
        if (!comparison.simulated()) {
            recommendation = affected == 0
                    ? "This planning-data change has no direct session mapping. Run readiness and generation in an isolated draft before applying it."
                    : affected + " session" + (affected == 1 ? " is" : "s are")
                            + " directly affected. Use an isolated repair draft to validate the full scheduling impact.";
        } else if (!comparison.feasible()) {
            recommendation = "The simulated change cannot produce a complete clash-free timetable with the current resources.";
        } else if (comparison.clashesAdded() == 0) {
            recommendation = "The simulated change adds no hard clashes to the current timetable.";
        } else {
            recommendation = comparison.clashesAdded() + " new hard clash"
                    + (comparison.clashesAdded() == 1 ? " is" : "es are")
                    + " projected. Create and review a repair draft before applying the change.";
        }
        UUID id = scenarioRepository.insertCompletedWhatIf(
                UUID.randomUUID(), universityId, termId,
                request.sourceVersionId(), userId, scenarioType,
                request.scenarioInput(), affected,
                comparison.clashesAdded(), comparison.clashesRemoved(),
                comparison.baselineScore(), comparison.projectedScore(),
                comparison.feasible(), comparison.candidateCount(),
                comparison.terminationReason(),
                recommendation);
        repository.insertVersionAudit(
                request.sourceVersionId(), userId, "WHAT_IF_COMPLETED",
                "Completed a non-destructive " + scenarioType + " simulation.");
        return scenarioRepository.findWhatIf(id)
                .orElseThrow(() -> notFound("AURA what-if result was not found."));
    }

    public List<WhatIfResponse> listWhatIf(UUID userId, UUID termId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireTerm(termId, universityId);
        return scenarioRepository.listWhatIf(termId);
    }

    @Transactional
    public EmergencyRepairResponse createEmergencyDraft(
            UUID userId,
            UUID termId,
            AuraScenarioDtos.EmergencyRepairRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireTermAndVersion(termId, request.sourceVersionId(), universityId);
        if (!scenarioRepository.isPublishedVersionForTerm(
                request.sourceVersionId(), termId)) {
            throw new AuraStateException(
                    "Emergency repairs must start from the published timetable.");
        }
        if (!scenarioRepository.isVersionCurrent(request.sourceVersionId())) {
            throw new AuraStateException(
                    "Scheduling data changed after this timetable was created. Generate and publish a current version first.");
        }
        String emergencyType = normalize(
                request.emergencyType(), EMERGENCY_TYPES,
                "Choose a supported emergency type.");
        validateResourceScope(emergencyType, request.affectedResourceId(), universityId);
        int affected = scenarioRepository.countAffectedSessions(
                request.sourceVersionId(), emergencyType,
                request.affectedResourceId());
        UUID requestId = scenarioRepository.insertEmergency(
                UUID.randomUUID(), universityId, termId,
                request.sourceVersionId(), userId, emergencyType,
                request.affectedResourceId(), request.reason(), affected);
        UUID draftId = repository.cloneVersion(
                UUID.randomUUID(), request.sourceVersionId(),
                repository.nextVersionNumber(termId),
                "Emergency repair: " + request.reason().trim(), userId,
                "EMERGENCY_REPAIR");
        scenarioRepository.completeEmergencyDraft(
                requestId, draftId, emergencyType, request.affectedResourceId());
        EmergencyReassignment result = reassignEmergencySessions(
                userId, universityId, draftId, emergencyType,
                request.affectedResourceId());
        scenarioRepository.markEmergencyResult(
                requestId,
                result.feasible() ? "DRAFT_READY" : "FAILED",
                result.moves().size(),
                result.message());
        List<DetectedClash> clashes = clashDetector.detect(
                repository.listSessions(draftId),
                repository.clashDetectionContext(draftId));
        repository.replaceClashes(draftId, clashes);
        repository.insertVersionAudit(
                draftId, userId, "EMERGENCY_DRAFT_CREATED",
                "Created a minimally scoped emergency repair draft with unaffected sessions pinned.");
        return scenarioRepository.findEmergency(requestId)
                .orElseThrow(() -> notFound("AURA emergency repair was not found."));
    }

    private EmergencyReassignment reassignEmergencySessions(
            UUID userId,
            UUID universityId,
            UUID draftId,
            String emergencyType,
            UUID affectedResourceId) {
        List<UUID> affectedIds = scenarioRepository.listAffectedSessionIds(
                draftId, emergencyType, affectedResourceId);
        if (affectedIds.isEmpty()) {
            return new EmergencyReassignment(true, List.of(),
                    "No sessions require reassignment in this draft.");
        }
        if ("INSTRUCTOR_ABSENCE".equals(emergencyType)
                || "SECTION_RESTRICTION".equals(emergencyType)) {
            return new EmergencyReassignment(false, List.of(),
                    "Automatic reassignment cannot change the assigned instructor or section. Review this draft manually.");
        }
        List<RoomResponse> rooms = repository.listRooms(universityId).stream()
                .filter(RoomResponse::active).toList();
        List<TimeslotResponse> slots = repository.listTimeslots(universityId).stream()
                .filter(TimeslotResponse::active).toList();
        List<EmergencyMove> moves = new ArrayList<>();
        Set<String> reservedRoomSlots = new HashSet<>();
        for (UUID sessionId : affectedIds) {
            SessionResponse session = repository.findSession(sessionId)
                    .orElseThrow(() -> notFound("Affected timetable session was not found."));
            if (session.locked()) {
                return new EmergencyReassignment(false, List.of(),
                        "An affected session is pinned and cannot be reassigned automatically.");
            }
            EmergencyMove move = findEmergencyMove(
                    userId, session, rooms, slots, emergencyType,
                    affectedResourceId, reservedRoomSlots);
            if (move == null) {
                return new EmergencyReassignment(false, List.of(),
                        "No clash-free alternative is available for every affected session.");
            }
            moves.add(move);
            reservedRoomSlots.add(move.roomId() + ":" + move.timeslotId());
        }
        moves.forEach(move -> auraService.applyMove(
                userId, move.sessionId(),
                new AuraDtos.ManualMoveRequest(
                        move.roomId(), move.timeslotId(),
                        "Emergency minimum-disruption reassignment")));
        return new EmergencyReassignment(true, List.copyOf(moves),
                moves.size() + " affected session" + (moves.size() == 1 ? " was" : "s were")
                        + " reassigned in the review draft.");
    }

    private EmergencyMove findEmergencyMove(
            UUID userId,
            SessionResponse session,
            List<RoomResponse> rooms,
            List<TimeslotResponse> slots,
            String emergencyType,
            UUID affectedResourceId,
            Set<String> reservedRoomSlots) {
        EmergencyMove best = null;
        int bestDisruption = Integer.MAX_VALUE;
        for (TimeslotResponse slot : slots) {
            if (("TIMESLOT_CANCELLATION".equals(emergencyType)
                    || "UNIVERSITY_EVENT".equals(emergencyType))
                    && slot.id().equals(affectedResourceId)) continue;
            for (RoomResponse room : rooms) {
                if (("ROOM_CLOSURE".equals(emergencyType)
                        || "FACILITY_OUTAGE".equals(emergencyType))
                        && room.id().equals(affectedResourceId)) continue;
                if (room.id().equals(session.roomId())
                        && slot.id().equals(session.timeslotId())) continue;
                if (reservedRoomSlots.contains(room.id() + ":" + slot.id())) continue;
                var preview = auraService.previewMove(
                        userId, session.id(),
                        new AuraDtos.ManualMovePreviewRequest(room.id(), slot.id()));
                if (!preview.allowed()) continue;
                int disruption = (room.id().equals(session.roomId()) ? 0 : 5)
                        + (slot.dayOfWeek() == session.dayOfWeek() ? 0 : 15)
                        + (slot.startsAt().equals(session.startsAt()) ? 0 : 10);
                if (disruption < bestDisruption) {
                    best = new EmergencyMove(session.id(), room.id(), slot.id());
                    bestDisruption = disruption;
                }
            }
        }
        return best;
    }

    public List<EmergencyRepairResponse> listEmergencies(
            UUID userId,
            UUID termId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireTerm(termId, universityId);
        return scenarioRepository.listEmergencies(termId);
    }

    private void requireTermAndVersion(
            UUID termId,
            UUID versionId,
            UUID universityId) {
        requireTerm(termId, universityId);
        if (!repository.resourceBelongsToUniversity(
                ScopedResource.VERSION, versionId, universityId)) {
            throw notFound("AURA timetable version was not found.");
        }
        TimetableVersionResponse version = repository.findVersion(versionId)
                .orElseThrow(() -> notFound("AURA timetable version was not found."));
        if (!termId.equals(version.termId())) {
            throw notFound("AURA timetable version was not found.");
        }
    }

    private void requireTerm(UUID termId, UUID universityId) {
        if (!repository.resourceBelongsToUniversity(
                ScopedResource.TERM, termId, universityId)) {
            throw notFound("AURA term was not found.");
        }
    }

    private void validateResourceScope(
            String type,
            UUID resourceId,
            UUID universityId) {
        ScopedResource resource = switch (type) {
            case "ROOM_UNAVAILABLE", "ROOM_CLOSURE", "FACILITY_REMOVED", "FACILITY_OUTAGE" -> ScopedResource.ROOM;
            case "INSTRUCTOR_UNAVAILABLE", "INSTRUCTOR_ABSENCE", "INSTRUCTOR_REPLACED" -> ScopedResource.INSTRUCTOR;
            case "TIMESLOT_REMOVED", "TIMESLOT_CANCELLATION", "UNIVERSITY_EVENT" -> ScopedResource.TIMESLOT;
            case "SECTION_RESTRICTION", "SECTION_ADDED" -> ScopedResource.SECTION;
            default -> null;
        };
        if (resource != null && resourceId == null) {
            throw new AuraStateException("Choose the affected scheduling resource.");
        }
        if (resource != null && !repository.resourceBelongsToUniversity(
                resource, resourceId, universityId)) {
            throw notFound("Affected scheduling resource was not found.");
        }
    }

    private UUID optionalResourceId(Map<String, Object> input) {
        Object raw = input.get("resourceId");
        if (raw == null || raw.toString().isBlank()) return null;
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException exception) {
            throw new AuraStateException("Choose a valid affected scheduling resource.");
        }
    }

    private ScenarioComparison compareScenario(
            UUID termId,
            UUID versionId,
            String baselineScore,
            String scenarioType,
            UUID resourceId) {
        List<SessionResponse> baselineSessions = repository.listSessions(versionId);
        var context = repository.clashDetectionContext(versionId);
        Set<String> baseline = clashDetector.detect(
                        baselineSessions, context).stream()
                .map(this::clashFingerprint)
                .collect(java.util.stream.Collectors.toSet());
        List<SessionResponse> projectedSessions = baselineSessions.stream()
                .map(session -> applyScenarioRestriction(
                        session, scenarioType, resourceId))
                .toList();
        Set<String> projected = clashDetector.detect(
                        projectedSessions, context).stream()
                .map(this::clashFingerprint)
                .collect(java.util.stream.Collectors.toSet());
        var requirements = repository.solverRequirements(termId);
        var rooms = repository.solverRooms(termId);
        var timeslots = repository.solverTimeslots(termId);
        var instructorAvailability = new ArrayList<>(
                repository.solverInstructorAvailability(termId));
        if (Set.of("ROOM_UNAVAILABLE", "FACILITY_REMOVED")
                .contains(scenarioType)) {
            rooms = rooms.stream()
                    .filter(room -> !room.id().equals(resourceId))
                    .toList();
        } else if ("TIMESLOT_REMOVED".equals(scenarioType)) {
            timeslots = timeslots.stream()
                    .filter(slot -> !slot.id().equals(resourceId))
                    .toList();
        } else if ("INSTRUCTOR_UNAVAILABLE".equals(scenarioType)) {
            timeslots.forEach(slot -> instructorAvailability.add(
                    new AuraJdbcRepository.SolverInstructorAvailability(
                            resourceId, slot.id(), "UNAVAILABLE")));
        }
        int occurrenceCount = requirements.stream()
                .mapToInt(AuraJdbcRepository.SolverRequirement::sessionsPerWeek)
                .sum();
        if (rooms.isEmpty() || timeslots.isEmpty()) {
            return new ScenarioComparison(
                    true,
                    (int) projected.stream()
                            .filter(key -> !baseline.contains(key)).count(),
                    (int) baseline.stream()
                            .filter(key -> !projected.contains(key)).count(),
                    baselineScore,
                    null,
                    false,
                    occurrenceCount,
                    rooms.isEmpty()
                            ? "NO_ELIGIBLE_ROOMS"
                            : "NO_ELIGIBLE_TIMESLOTS");
        }
        AuraSolverService.SolverResult result = solverService.solve(
                requirements,
                rooms,
                timeslots,
                instructorAvailability,
                repository.solverRoomAvailability(termId),
                repository.solverSectionAvailability(termId),
                repository.solverStudentAvailability(termId),
                repository.solverTravelRules(termId),
                repository.solverCalendarBlocks(termId),
                10,
                AuraConstraintCatalog.weights(
                        AuraConstraintCatalog.WHAT_IF,
                        repository.constraintWeights(
                                termId, AuraConstraintCatalog.WHAT_IF)),
                0L);
        boolean feasible = HardMediumSoftScore
                .parseScore(result.score()).hardScore() >= 0;
        return new ScenarioComparison(
                true,
                (int) projected.stream().filter(key -> !baseline.contains(key)).count(),
                (int) baseline.stream().filter(key -> !projected.contains(key)).count(),
                baselineScore,
                result.score(),
                feasible,
                result.candidateCount(),
                result.terminationReason());
    }

    private SessionResponse applyScenarioRestriction(
            SessionResponse session,
            String scenarioType,
            UUID resourceId) {
        boolean instructor = "INSTRUCTOR_UNAVAILABLE".equals(scenarioType)
                && resourceId.equals(session.instructorId());
        boolean room = Set.of(
                        "ROOM_UNAVAILABLE", "FACILITY_REMOVED")
                .contains(scenarioType)
                && resourceId.equals(session.roomId());
        boolean calendar = "TIMESLOT_REMOVED".equals(scenarioType)
                && resourceId.equals(session.timeslotId());
        return session.withScenarioRestrictions(
                instructor, room, false, calendar);
    }

    private String clashFingerprint(DetectedClash clash) {
        String left = clash.primarySessionId().toString();
        String right = clash.secondarySessionId() == null
                ? ""
                : clash.secondarySessionId().toString();
        if (left.compareTo(right) > 0) {
            String swap = left;
            left = right;
            right = swap;
        }
        return clash.clashType() + ':' + left + ':' + right;
    }

    private String normalize(String value, Set<String> supported, String message) {
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (!supported.contains(normalized)) throw new AuraStateException(message);
        return normalized;
    }

    private ResourceNotFoundException notFound(String message) {
        String suffix = " was not found.";
        return new ResourceNotFoundException(message.endsWith(suffix)
                ? message.substring(0, message.length() - suffix.length())
                : message);
    }

    private record EmergencyMove(UUID sessionId, UUID roomId, UUID timeslotId) { }

    private record EmergencyReassignment(
            boolean feasible,
            List<EmergencyMove> moves,
            String message) { }

    private record ScenarioComparison(
            boolean simulated,
            int clashesAdded,
            int clashesRemoved,
            String baselineScore,
            String projectedScore,
            boolean feasible,
            int candidateCount,
            String terminationReason) { }
}
