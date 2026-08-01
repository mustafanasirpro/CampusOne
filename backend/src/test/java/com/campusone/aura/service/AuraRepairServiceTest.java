package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.aura.dto.AuraOperationsDtos;
import com.campusone.aura.dto.AuraOperationsDtos.RepairImpact;
import com.campusone.aura.dto.AuraOperationsDtos.RepairMove;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraOperationsRepository;
import com.campusone.aura.repository.AuraOperationsRepository.RepairPlanRow;
import com.campusone.aura.repository.AuraOperationsRepository.RepairSourceState;
import com.campusone.common.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuraRepairServiceTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID UNIVERSITY = UUID.randomUUID();
    private static final UUID TERM = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-07-21T12:00:00Z");

    @Mock AuraAuthorizationService authorization;
    @Mock AuraJdbcRepository coreRepository;
    @Mock AuraOperationsRepository repository;
    @Mock AuraService auraService;
    @Mock AuraNotificationService notificationService;

    private AuraRepairService service;

    @BeforeEach
    void setUp() {
        service = new AuraRepairService(
                authorization, coreRepository, repository, auraService,
                notificationService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(authorization.requireAdminUniversity(ACTOR)).thenReturn(UNIVERSITY);
    }

    @Test
    void preview_rejectsCrossUniversityVersionWithoutLeakingIt() {
        when(repository.repairSourceState(VERSION)).thenReturn(Optional.of(
                new RepairSourceState(
                        VERSION, TERM, UUID.randomUUID(), "DRAFT", 0, 0, 0)));

        assertThatThrownBy(() -> service.preview(
                ACTOR, VERSION,
                new AuraOperationsDtos.RepairPreviewRequest(
                        null, UUID.randomUUID(), "Resolve conflict")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Timetable version");
        verify(auraService, never()).cloneVersion(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void preview_rejectsStaleSchedulingRevisionBeforeCreatingDraft() {
        when(repository.repairSourceState(VERSION)).thenReturn(Optional.of(
                new RepairSourceState(VERSION, TERM, UNIVERSITY, "DRAFT", 2, 4, 5)));

        assertThatThrownBy(() -> service.preview(
                ACTOR, VERSION,
                new AuraOperationsDtos.RepairPreviewRequest(
                        null, UUID.randomUUID(), "Resolve conflict")))
                .isInstanceOf(AuraStateException.class)
                .hasMessageContaining("Scheduling data changed");
        verify(auraService, never()).cloneVersion(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void apply_rejectsExpiredPreviewWithoutMovingSessions() {
        UUID planId = UUID.randomUUID();
        var row = new RepairPlanRow(
                planId, UNIVERSITY, TERM, VERSION, UUID.randomUUID(), ACTOR,
                "SESSION", UUID.randomUUID(), "PREVIEWED", 3, 2, 2, 3,
                List.of(), new RepairImpact(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                "hash", "Reason", NOW.minusSeconds(1), null);
        when(repository.lockRepairPlan(planId, UNIVERSITY)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.apply(
                ACTOR, planId, new AuraOperationsDtos.ApplyRepairRequest("token")))
                .isInstanceOf(AuraStateException.class)
                .hasMessageContaining("expired");
        verify(auraService, never()).applyMove(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void apply_notifiesAffectedRegisteredStudentsAfterReviewedMove()
            throws Exception {
        UUID planId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        UUID originalRoom = UUID.randomUUID();
        UUID originalSlot = UUID.randomUUID();
        UUID proposedRoom = UUID.randomUUID();
        UUID proposedSlot = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        String token = "review-token";
        String hash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(token.getBytes(StandardCharsets.UTF_8)));
        RepairMove move = new RepairMove(
                sessionId, originalRoom, originalSlot,
                proposedRoom, proposedSlot, 1, 10);
        RepairImpact impact = new RepairImpact(
                1, 1, 1, 1, 1, 0, 1, 0, 1, 10);
        var preview = new RepairPlanRow(
                planId, UNIVERSITY, TERM, VERSION, draftId, ACTOR,
                "SESSION", sessionId, "PREVIEWED", 3, 2, 2, 3,
                List.of(move), impact, hash, "Move session",
                NOW.plusSeconds(300), null);
        var applied = new RepairPlanRow(
                planId, UNIVERSITY, TERM, VERSION, draftId, ACTOR,
                "SESSION", sessionId, "APPLIED", 3, 2, 2, 3,
                List.of(move), impact, hash, "Move session",
                NOW.plusSeconds(300), NOW);
        when(repository.lockRepairPlan(planId, UNIVERSITY))
                .thenReturn(Optional.of(preview));
        when(coreRepository.findSession(sessionId)).thenReturn(Optional.of(
                new com.campusone.aura.dto.AuraDtos.SessionResponse(
                        sessionId, draftId, offeringId, UUID.randomUUID(),
                        "CS101", "Programming", UUID.randomUUID(), "Section A",
                        UUID.randomUUID(), "Instructor", originalRoom, "Room A",
                        "CLASSROOM", originalSlot, 1, LocalTime.of(9, 0),
                        LocalTime.of(10, 0), false, "REPAIR")));
        when(repository.activeStudentUserIdsForOfferings(
                org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(studentId));
        when(repository.findRepairPlan(planId, UNIVERSITY))
                .thenReturn(Optional.of(applied));

        service.apply(
                ACTOR,
                planId,
                new AuraOperationsDtos.ApplyRepairRequest(token));

        verify(notificationService).notifyStudent(
                studentId,
                "A timetable adjustment is being reviewed",
                "A proposed timetable change may affect one of your registered courses.",
                draftId);
    }
}
