package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.aura.dto.AuraOperationsDtos;
import com.campusone.aura.dto.AuraOperationsDtos.RepairImpact;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraOperationsRepository;
import com.campusone.aura.repository.AuraOperationsRepository.RepairPlanRow;
import com.campusone.aura.repository.AuraOperationsRepository.RepairSourceState;
import com.campusone.common.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

    private AuraRepairService service;

    @BeforeEach
    void setUp() {
        service = new AuraRepairService(
                authorization, coreRepository, repository, auraService,
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
}
