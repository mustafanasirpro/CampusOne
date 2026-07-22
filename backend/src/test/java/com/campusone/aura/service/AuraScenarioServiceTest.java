package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.aura.dto.AuraDtos.TimetableVersionResponse;
import com.campusone.aura.dto.AuraScenarioDtos;
import com.campusone.aura.dto.AuraScenarioDtos.EmergencyRepairResponse;
import com.campusone.aura.dto.AuraScenarioDtos.WhatIfResponse;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ClashDetectionContext;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.repository.AuraScenarioRepository;
import java.time.Instant;
import java.time.LocalTime;
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
    @Mock AuraService auraService;

    private AuraScenarioService service;

    @BeforeEach
    void setUp() {
        service = new AuraScenarioService(
                authorizationService, repository, scenarioRepository,
                clashDetector, auraService);
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
        when(scenarioRepository.isVersionCurrent(VERSION_ID)).thenReturn(true);
        when(repository.resourceBelongsToUniversity(
                ScopedResource.ROOM, ROOM_ID, UNIVERSITY_ID)).thenReturn(true);
        when(scenarioRepository.countAffectedSessions(
                VERSION_ID, "ROOM_CLOSURE", ROOM_ID)).thenReturn(1);
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
                org.mockito.ArgumentMatchers.eq(1))).thenReturn(requestId);
        when(repository.nextVersionNumber(TERM_ID)).thenReturn(4);
        when(repository.cloneVersion(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(VERSION_ID),
                org.mockito.ArgumentMatchers.eq(4),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq("EMERGENCY_REPAIR")))
                .thenReturn(draftId);
        UUID sessionId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID instructorId = UUID.randomUUID();
        UUID originalSlot = UUID.randomUUID();
        UUID alternateRoom = UUID.randomUUID();
        UUID alternateSlot = UUID.randomUUID();
        var sessionRow = new com.campusone.aura.dto.AuraDtos.SessionResponse(
                sessionId, draftId, offeringId, requirementId,
                "CS101", "Programming", sectionId, "Section A",
                instructorId, "Instructor", ROOM_ID, "Closed room",
                "CLASSROOM", originalSlot, 1, LocalTime.of(9, 0),
                LocalTime.of(10, 0), false, "EMERGENCY_REPAIR");
        when(scenarioRepository.listAffectedSessionIds(
                draftId, "ROOM_CLOSURE", ROOM_ID)).thenReturn(List.of(sessionId));
        when(repository.findSession(sessionId)).thenReturn(Optional.of(sessionRow));
        when(repository.listRooms(UNIVERSITY_ID)).thenReturn(List.of(
                new com.campusone.aura.dto.AuraDtos.RoomResponse(
                        ROOM_ID, UNIVERSITY_ID, "A", "Closed room", 40,
                        "CLASSROOM", List.of(), true, 0),
                new com.campusone.aura.dto.AuraDtos.RoomResponse(
                        alternateRoom, UNIVERSITY_ID, "B", "Alternate room", 40,
                        "CLASSROOM", List.of(), true, 0)));
        when(repository.listTimeslots(UNIVERSITY_ID)).thenReturn(List.of(
                new com.campusone.aura.dto.AuraDtos.TimeslotResponse(
                        originalSlot, UNIVERSITY_ID, 1, LocalTime.of(9, 0),
                        LocalTime.of(10, 0), "Monday 09:00", true, 0),
                new com.campusone.aura.dto.AuraDtos.TimeslotResponse(
                        alternateSlot, UNIVERSITY_ID, 1, LocalTime.of(10, 0),
                        LocalTime.of(11, 0), "Monday 10:00", true, 0)));
        when(auraService.previewMove(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(sessionId),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.campusone.aura.dto.AuraDtos.ManualMovePreviewResponse(
                        true, "Safe", List.of()));
        when(repository.listSessions(draftId)).thenReturn(List.of());
        var context = ClashDetectionContext.empty();
        when(repository.clashDetectionContext(draftId)).thenReturn(context);
        when(clashDetector.detect(List.of(), context)).thenReturn(List.of());
        when(scenarioRepository.findEmergency(requestId)).thenReturn(Optional.of(
                new EmergencyRepairResponse(
                        requestId, TERM_ID, VERSION_ID, draftId,
                        "ROOM_CLOSURE", ROOM_ID, "Water leak",
                        "DRAFT_READY", 1, 1,
                        "1 affected session was reassigned in the review draft.",
                        Instant.now())));

        service.createEmergencyDraft(
                USER_ID, TERM_ID,
                new AuraScenarioDtos.EmergencyRepairRequest(
                        VERSION_ID, "ROOM_CLOSURE", ROOM_ID, "Water leak"));

        verify(scenarioRepository).completeEmergencyDraft(
                requestId, draftId, "ROOM_CLOSURE", ROOM_ID);
        verify(scenarioRepository).markEmergencyResult(
                requestId, "DRAFT_READY", 1,
                "1 affected session was reassigned in the review draft.");
        verify(auraService).applyMove(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(sessionId),
                org.mockito.ArgumentMatchers.argThat(move ->
                        move.roomId().equals(alternateRoom)));
        verify(repository).replaceClashes(draftId, List.of());
    }

    @Test
    void emergencyRepair_rejectsStalePublishedSource() {
        scopedVersion();
        when(scenarioRepository.isPublishedVersionForTerm(VERSION_ID, TERM_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createEmergencyDraft(
                USER_ID, TERM_ID,
                new AuraScenarioDtos.EmergencyRepairRequest(
                        VERSION_ID, "ROOM_CLOSURE", ROOM_ID, "Water leak")))
                .isInstanceOf(AuraStateException.class)
                .hasMessageContaining("Generate and publish a current version");

        verify(scenarioRepository, never()).insertEmergency(
                any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
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
