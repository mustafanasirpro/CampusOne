package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.aura.dto.AuraOperationsDtos;
import com.campusone.aura.dto.AuraOperationsDtos.BuildingResponse;
import com.campusone.aura.dto.AuraOperationsDtos.MutationResponse;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.repository.AuraOperationsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuraOperationsServiceTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID UNIVERSITY = UUID.randomUUID();
    private static final UUID TERM = UUID.randomUUID();

    @Mock AuraAuthorizationService authorization;
    @Mock AuraJdbcRepository coreRepository;
    @Mock AuraOperationsRepository repository;

    private AuraOperationsService service;

    @BeforeEach
    void setUp() {
        service = new AuraOperationsService(authorization, coreRepository, repository);
        when(authorization.requireAdminUniversity(ACTOR)).thenReturn(UNIVERSITY);
    }

    @Test
    void updateTerm_rejectsInvalidDatesBeforeWriting() {
        var request = new AuraOperationsDtos.UpdateTermRequest(
                "FA26", "Fall 2026", LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 9, 1), "Asia/Karachi", "READY", 0);

        assertThatThrownBy(() -> service.updateTerm(ACTOR, TERM, request))
                .isInstanceOf(AuraStateException.class)
                .hasMessageContaining("Start date");
    }

    @Test
    void updateTerm_usesOptimisticMutationAndWritesAudit() {
        var request = new AuraOperationsDtos.UpdateTermRequest(
                "FA26", "Fall 2026", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 1), "Asia/Karachi", "READY", 2);
        var response = new MutationResponse(
                TERM, "TERM", true, 3, Instant.parse("2026-07-21T00:00:00Z"));
        when(repository.updateTerm(TERM, UNIVERSITY, request))
                .thenReturn(Optional.of(response));
        when(repository.termForResource("TERM", TERM)).thenReturn(Optional.of(TERM));

        assertThat(service.updateTerm(ACTOR, TERM, request)).isEqualTo(response);
        verify(repository).insertAudit(
                org.mockito.ArgumentMatchers.eq(UNIVERSITY),
                org.mockito.ArgumentMatchers.eq(TERM),
                org.mockito.ArgumentMatchers.eq(ACTOR),
                org.mockito.ArgumentMatchers.eq("TERM_UPDATED"),
                org.mockito.ArgumentMatchers.eq("TERM"),
                org.mockito.ArgumentMatchers.eq(TERM),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("SUCCESS"),
                org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    void createBuilding_returnsPersistedUniversityScopedRecord() {
        var request = new AuraOperationsDtos.CreateBuildingRequest("NORTH", "North Block", 5);
        UUID id = UUID.randomUUID();
        var expected = new BuildingResponse(id, "NORTH", "North Block", 5, true, 0);
        when(repository.insertBuilding(UNIVERSITY, request)).thenReturn(id);
        when(repository.listBuildings(UNIVERSITY)).thenReturn(List.of(expected));

        assertThat(service.createBuilding(ACTOR, request)).isEqualTo(expected);
    }

    @Test
    void createTeachingGroup_rejectsCrossUniversityOffering() {
        UUID offering = UUID.randomUUID();
        var request = new AuraOperationsDtos.UpsertTeachingGroupRequest(
                offering, "LAB", "L1", "Lab group 1", 30, true, null);
        when(coreRepository.resourceBelongsToUniversity(
                ScopedResource.OFFERING, offering, UNIVERSITY)).thenReturn(false);

        assertThatThrownBy(() -> service.createTeachingGroup(ACTOR, request))
                .hasMessageContaining("Offering");
    }

    @Test
    void scopedTimetable_rejectsUnscopedVersion() {
        UUID version = UUID.randomUUID();
        when(coreRepository.resourceBelongsToUniversity(
                ScopedResource.VERSION, version, UNIVERSITY)).thenReturn(false);

        assertThatThrownBy(() -> service.scopedTimetable(
                ACTOR, version, "WEEK", null, null))
                .hasMessageContaining("Timetable version");
    }

    @Test
    void setActiveState_isScopedAndOptimisticallyAudited() {
        UUID room = UUID.randomUUID();
        var request = new AuraOperationsDtos.ActiveStateRequest(
                false, 4, "Temporarily unavailable");
        var expected = new MutationResponse(
                room, "ROOM", false, 5, Instant.parse("2026-07-21T00:00:00Z"));
        when(coreRepository.resourceBelongsToUniversity(
                ScopedResource.ROOM, room, UNIVERSITY)).thenReturn(true);
        when(repository.setActiveState("ROOM", room, false, 4))
                .thenReturn(Optional.of(expected));

        assertThat(service.setActiveState(ACTOR, "room", room, request))
                .isEqualTo(expected);
        verify(repository).insertAudit(
                org.mockito.ArgumentMatchers.eq(UNIVERSITY),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(ACTOR),
                org.mockito.ArgumentMatchers.eq("ROOM_DEACTIVATED"),
                org.mockito.ArgumentMatchers.eq("ROOM"),
                org.mockito.ArgumentMatchers.eq(room),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("SUCCESS"),
                org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    void setActiveState_rejectsCrossUniversityResource() {
        UUID room = UUID.randomUUID();
        when(coreRepository.resourceBelongsToUniversity(
                ScopedResource.ROOM, room, UNIVERSITY)).thenReturn(false);

        assertThatThrownBy(() -> service.setActiveState(
                ACTOR, "ROOM", room,
                new AuraOperationsDtos.ActiveStateRequest(false, 0, "Unavailable")))
                .hasMessageContaining("Scheduling resource");
    }
}
