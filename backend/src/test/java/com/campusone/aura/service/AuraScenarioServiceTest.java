package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.aura.dto.AuraDtos.TimetableVersionResponse;
import com.campusone.aura.dto.AuraScenarioDtos;
import com.campusone.aura.dto.AuraScenarioDtos.EmergencyRepairResponse;
import com.campusone.aura.dto.AuraScenarioDtos.WhatIfResponse;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.repository.AuraScenarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuraScenarioServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID UNIVERSITY_ID = UUID.randomUUID();
    private static final UUID TERM_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();

    @Mock AuraAuthorizationService authorizationService;
    @Mock AuraJdbcRepository repository;
    @Mock AuraScenarioRepository scenarioRepository;
    @Mock AuraClashDetector clashDetector;

    private AuraScenarioService service;

    @BeforeEach
    void setUp() {
        service = new AuraScenarioService(
                authorizationService, repository, scenarioRepository,
                clashDetector);
        when(authorizationService.requireAdminUniversity(USER_ID))
                .thenReturn(UNIVERSITY_ID);
    }

    @Test
    void runWhatIf_isNonDestructiveAndPersistsOnlyScenarioResults() {
        scopedVersion();
        when(repository.resourceBelongsToUniversity(
                ScopedResource.ROOM, ROOM_ID, UNIVERSITY_ID)).thenReturn(true);
        when(scenarioRepository.countAffectedSessions(
                VERSION_ID, "ROOM_UNAVAILABLE", ROOM_ID)).thenReturn(3);
        UUID resultId = UUID.randomUUID();
        when(scenarioRepository.insertCompletedWhatIf(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(UNIVERSITY_ID),
                org.mockito.ArgumentMatchers.eq(TERM_ID),
                org.mockito.ArgumentMatchers.eq(VERSION_ID),
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq("ROOM_UNAVAILABLE"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.any())).thenReturn(resultId);
        when(scenarioRepository.findWhatIf(resultId)).thenReturn(Optional.of(
                new WhatIfResponse(resultId, TERM_ID, VERSION_ID,
                        "ROOM_UNAVAILABLE", "COMPLETED", 3, 0, 0,
                        "Create a repair draft.", Instant.now(), Instant.now())));

        service.runWhatIf(USER_ID, TERM_ID, new AuraScenarioDtos.WhatIfRequest(
                VERSION_ID, "room-unavailable",
                Map.of("resourceId", ROOM_ID.toString())));

        verify(scenarioRepository).insertCompletedWhatIf(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(UNIVERSITY_ID),
                org.mockito.ArgumentMatchers.eq(TERM_ID),
                org.mockito.ArgumentMatchers.eq(VERSION_ID),
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq("ROOM_UNAVAILABLE"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.contains("3 sessions"));
    }

    @Test
    void emergencyRepair_requiresPublishedSource() {
        scopedVersion();
        when(scenarioRepository.isPublishedVersionForTerm(VERSION_ID, TERM_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> service.createEmergencyDraft(
                USER_ID, TERM_ID,
                new AuraScenarioDtos.EmergencyRepairRequest(
                        VERSION_ID, "ROOM_CLOSURE", ROOM_ID, "Water leak")))
                .isInstanceOf(AuraStateException.class)
                .hasMessageContaining("published timetable");
    }

    @Test
    void emergencyRepair_clonesPublishedVersionAndPinsUnaffectedSessions() {
        scopedVersion();
        when(scenarioRepository.isPublishedVersionForTerm(VERSION_ID, TERM_ID))
                .thenReturn(true);
        when(repository.resourceBelongsToUniversity(
                ScopedResource.ROOM, ROOM_ID, UNIVERSITY_ID)).thenReturn(true);
        when(scenarioRepository.countAffectedSessions(
                VERSION_ID, "ROOM_CLOSURE", ROOM_ID)).thenReturn(2);
        UUID requestId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        when(scenarioRepository.insertEmergency(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(UNIVERSITY_ID),
                org.mockito.ArgumentMatchers.eq(TERM_ID),
                org.mockito.ArgumentMatchers.eq(VERSION_ID),
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq("ROOM_CLOSURE"),
                org.mockito.ArgumentMatchers.eq(ROOM_ID),
                org.mockito.ArgumentMatchers.eq("Water leak"),
                org.mockito.ArgumentMatchers.eq(2))).thenReturn(requestId);
        when(repository.nextVersionNumber(TERM_ID)).thenReturn(4);
        when(repository.cloneVersion(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(VERSION_ID),
                org.mockito.ArgumentMatchers.eq(4),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq("EMERGENCY_REPAIR")))
                .thenReturn(draftId);
        when(repository.listSessions(draftId)).thenReturn(List.of());
        when(clashDetector.detect(List.of())).thenReturn(List.of());
        when(scenarioRepository.findEmergency(requestId)).thenReturn(Optional.of(
                new EmergencyRepairResponse(
                        requestId, TERM_ID, VERSION_ID, draftId,
                        "ROOM_CLOSURE", ROOM_ID, "Water leak",
                        "DRAFT_READY", 2, Instant.now())));

        service.createEmergencyDraft(
                USER_ID, TERM_ID,
                new AuraScenarioDtos.EmergencyRepairRequest(
                        VERSION_ID, "ROOM_CLOSURE", ROOM_ID, "Water leak"));

        verify(scenarioRepository).completeEmergencyDraft(
                requestId, draftId, "ROOM_CLOSURE", ROOM_ID);
        verify(repository).replaceClashes(draftId, List.of());
    }

    private void scopedVersion() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.TERM, TERM_ID, UNIVERSITY_ID)).thenReturn(true);
        when(repository.resourceBelongsToUniversity(
                ScopedResource.VERSION, VERSION_ID, UNIVERSITY_ID)).thenReturn(true);
        when(repository.findVersion(VERSION_ID)).thenReturn(Optional.of(
                new TimetableVersionResponse(
                        VERSION_ID, TERM_ID, null, 1, "PUBLISHED",
                        "0hard/0medium/0soft", null, Instant.now(), Instant.now())));
    }
}
