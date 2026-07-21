package com.campusone.aura.service;

import com.campusone.aura.dto.AuraDtos;
import com.campusone.aura.dto.AuraDtos.RoomResponse;
import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.dto.AuraDtos.TimeslotResponse;
import com.campusone.aura.dto.AuraOperationsDtos;
import com.campusone.aura.dto.AuraOperationsDtos.RepairImpact;
import com.campusone.aura.dto.AuraOperationsDtos.RepairMove;
import com.campusone.aura.dto.AuraOperationsDtos.RepairPlanResponse;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.repository.AuraOperationsRepository;
import com.campusone.aura.repository.AuraOperationsRepository.RepairPlanRow;
import com.campusone.aura.repository.AuraOperationsRepository.RepairSourceState;
import com.campusone.common.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
public class AuraRepairService {

    private static final int MAX_CANDIDATES = 500;
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(15);

    private final AuraAuthorizationService authorization;
    private final AuraJdbcRepository coreRepository;
    private final AuraOperationsRepository repository;
    private final AuraService auraService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuraRepairService(
            AuraAuthorizationService authorization,
            AuraJdbcRepository coreRepository,
            AuraOperationsRepository repository,
            AuraService auraService,
            Clock clock) {
        this.authorization = authorization;
        this.coreRepository = coreRepository;
        this.repository = repository;
        this.auraService = auraService;
        this.clock = clock;
    }

    @Transactional
    public RepairPlanResponse preview(
            UUID actorId,
            UUID sourceVersionId,
            AuraOperationsDtos.RepairPreviewRequest request) {
        UUID universityId = authorization.requireAdminUniversity(actorId);
        RepairSourceState source = repository.repairSourceState(sourceVersionId)
                .filter(row -> row.universityId().equals(universityId))
                .orElseThrow(() -> notFound("Timetable version"));
        if (!List.of("DRAFT", "PUBLISHED").contains(source.status())) {
            throw new AuraStateException(
                    "Repairs can start only from a draft or published timetable.");
        }
        if (source.inputRevision() != source.currentInputRevision()) {
            throw new AuraStateException(
                    "Scheduling data changed after this timetable was created. Revalidate it first.");
        }
        if ((request.clashId() == null) == (request.sessionId() == null)) {
            throw new AuraStateException("Choose one clash or one session to repair.");
        }
        UUID sourceSessionId = request.sessionId() != null
                ? request.sessionId()
                : repository.clashSession(request.clashId(), sourceVersionId)
                        .orElseThrow(() -> notFound("Open timetable clash"));
        SessionResponse sourceSession = coreRepository.findSession(sourceSessionId)
                .filter(session -> session.versionId().equals(sourceVersionId))
                .orElseThrow(() -> notFound("Timetable session"));
        var draft = auraService.cloneVersion(actorId, sourceVersionId,
                new AuraDtos.CloneVersionRequest("Localized repair: " + request.reason().trim()));
        UUID draftSessionId = repository.correspondingSession(sourceSessionId, draft.id())
                .orElseThrow(() -> notFound("Repair draft session"));
        SessionResponse draftSession = coreRepository.findSession(draftSessionId)
                .orElseThrow(() -> notFound("Repair draft session"));

        Candidate candidate = draftSession.locked()
                ? null : bestCandidate(actorId, universityId, draftSession);
        int students = repository.activeStudentsForOffering(sourceSession.offeringId());
        RepairMove move = candidate == null ? null : new RepairMove(
                draftSession.id(), draftSession.roomId(), draftSession.timeslotId(),
                candidate.room().id(), candidate.timeslot().id(), students,
                candidate.disruptionScore());
        RepairImpact impact = candidate == null
                ? new RepairImpact(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                : new RepairImpact(1, students, 1, 1,
                        candidate.roomChanged() ? 1 : 0,
                        candidate.dayChanged() ? 1 : 0,
                        candidate.timeChanged() ? 1 : 0,
                        0, request.clashId() == null ? 0 : 1,
                        candidate.disruptionScore());
        String rawToken = token();
        Instant expiresAt = clock.instant().plus(PREVIEW_TTL);
        String triggerType = request.clashId() == null ? "SESSION" : "CLASH";
        UUID triggerId = request.clashId() == null ? sourceSessionId : request.clashId();
        UUID planId = repository.insertRepairPlan(
                source, draft.id(), actorId, triggerType, triggerId,
                move == null ? List.of() : List.of(move), impact,
                hash(rawToken), request.reason(), expiresAt);
        repository.insertAudit(
                universityId, source.termId(), actorId, "REPAIR_PREVIEWED",
                "REPAIR_PLAN", planId,
                candidate == null
                        ? "No safe localized repair was found."
                        : "Prepared a one-session minimum-disruption repair.",
                UUID.randomUUID(), candidate == null ? "REJECTED" : "SUCCESS",
                Map.of("feasible", candidate != null,
                        "sessionsMoved", impact.sessionsMoved(),
                        "disruptionScore", impact.disruptionScore()));
        return new RepairPlanResponse(
                planId, sourceVersionId, draft.id(), triggerType, triggerId,
                "PREVIEWED", candidate != null,
                candidate == null
                        ? (draftSession.locked()
                            ? "The affected session is pinned, so no automatic repair was applied."
                            : "No clash-free room and timeslot alternative is currently available.")
                        : "A minimum-disruption repair is ready for review.",
                candidate == null ? null : rawToken,
                move == null ? List.of() : List.of(move), impact, expiresAt, null);
    }

    @Transactional
    public RepairPlanResponse apply(
            UUID actorId,
            UUID planId,
            AuraOperationsDtos.ApplyRepairRequest request) {
        UUID universityId = authorization.requireAdminUniversity(actorId);
        RepairPlanRow plan = repository.lockRepairPlan(planId, universityId)
                .orElseThrow(() -> notFound("Repair preview"));
        if (!"PREVIEWED".equals(plan.status()) || !clock.instant().isBefore(plan.expiresAt())) {
            throw new AuraStateException("This repair preview expired. Create a new preview.");
        }
        if (!constantTimeEquals(plan.previewHash(), hash(request.previewToken()))) {
            throw new AuraStateException("This repair preview is invalid. Create a new preview.");
        }
        if (plan.inputRevision() != plan.currentInputRevision()
                || plan.sourceVersionRevision() != plan.currentSourceVersion()) {
            throw new AuraStateException(
                    "Scheduling data changed after this preview. Create a new repair preview.");
        }
        if (plan.moves().isEmpty()) {
            throw new AuraStateException("This preview has no safe repair to apply.");
        }
        for (RepairMove move : plan.moves()) {
            SessionResponse current = coreRepository.findSession(move.sessionId())
                    .filter(session -> session.versionId().equals(plan.draftVersionId()))
                    .orElseThrow(() -> notFound("Repair draft session"));
            if (!current.roomId().equals(move.originalRoomId())
                    || !current.timeslotId().equals(move.originalTimeslotId())) {
                throw new AuraStateException(
                        "The repair draft changed after preview. Create a new preview.");
            }
            auraService.applyMove(actorId, move.sessionId(),
                    new AuraDtos.ManualMoveRequest(
                            move.proposedRoomId(), move.proposedTimeslotId(), plan.reason()));
        }
        repository.markRepairApplied(planId);
        repository.insertAudit(
                universityId, plan.termId(), actorId, "REPAIR_APPLIED",
                "REPAIR_PLAN", planId,
                "Applied a reviewed minimum-disruption repair.",
                UUID.randomUUID(), "SUCCESS",
                Map.of("sessionsMoved", plan.impact().sessionsMoved(),
                        "disruptionScore", plan.impact().disruptionScore()));
        return response(repository.findRepairPlan(planId, universityId)
                .orElseThrow(() -> notFound("Repair plan")), null);
    }

    @Transactional(readOnly = true)
    public RepairPlanResponse get(UUID actorId, UUID planId) {
        UUID universityId = authorization.requireAdminUniversity(actorId);
        return response(repository.findRepairPlan(planId, universityId)
                .orElseThrow(() -> notFound("Repair plan")), null);
    }

    private Candidate bestCandidate(
            UUID actorId,
            UUID universityId,
            SessionResponse session) {
        List<RoomResponse> rooms = coreRepository.listRooms(universityId).stream()
                .filter(RoomResponse::active)
                .sorted(Comparator.comparing(room -> !room.id().equals(session.roomId())))
                .toList();
        List<TimeslotResponse> slots = coreRepository.listTimeslots(universityId).stream()
                .filter(TimeslotResponse::active)
                .sorted(Comparator
                        .comparing((TimeslotResponse slot) -> slot.dayOfWeek() != session.dayOfWeek())
                        .thenComparing(slot -> !slot.startsAt().equals(session.startsAt())))
                .toList();
        Candidate best = null;
        int checked = 0;
        for (TimeslotResponse slot : slots) {
            for (RoomResponse room : rooms) {
                if (++checked > MAX_CANDIDATES) return best;
                if (room.id().equals(session.roomId())
                        && slot.id().equals(session.timeslotId())) continue;
                AuraDtos.ManualMovePreviewResponse preview = auraService.previewMove(
                        actorId, session.id(),
                        new AuraDtos.ManualMovePreviewRequest(room.id(), slot.id()));
                if (!preview.allowed()) continue;
                boolean roomChanged = !room.id().equals(session.roomId());
                boolean dayChanged = slot.dayOfWeek() != session.dayOfWeek();
                boolean timeChanged = !slot.startsAt().equals(session.startsAt());
                int students = repository.activeStudentsForOffering(session.offeringId());
                int score = (roomChanged ? 5 : 0) + (dayChanged ? 15 : 0)
                        + (timeChanged ? 10 : 0) + Math.min(20, students / 10);
                Candidate candidate = new Candidate(
                        room, slot, roomChanged, dayChanged, timeChanged, score);
                if (best == null || candidate.disruptionScore() < best.disruptionScore()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private RepairPlanResponse response(RepairPlanRow plan, String token) {
        return new RepairPlanResponse(
                plan.id(), plan.sourceVersionId(), plan.draftVersionId(),
                plan.triggerType(), plan.triggerId(), plan.status(),
                !plan.moves().isEmpty(),
                "APPLIED".equals(plan.status())
                        ? "The localized repair was applied to the draft."
                        : plan.moves().isEmpty()
                            ? "No safe localized repair is available."
                            : "The repair preview is ready.",
                token, plan.moves(), plan.impact(), plan.expiresAt(), plan.appliedAt());
    }

    private String token() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }

    private ResourceNotFoundException notFound(String resource) {
        return new ResourceNotFoundException(resource);
    }

    private record Candidate(
            RoomResponse room,
            TimeslotResponse timeslot,
            boolean roomChanged,
            boolean dayChanged,
            boolean timeChanged,
            int disruptionScore) {
    }
}
