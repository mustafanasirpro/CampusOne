package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.aura.dto.AuraDtos;
import com.campusone.aura.dto.AuraDtos.ReadinessResponse;
import com.campusone.aura.dto.AuraDtos.TermResponse;
import com.campusone.aura.dto.AuraDtos.TimetableVersionResponse;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
class AuraServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID TERM_ID = UUID.fromString(
            "20000000-0000-4000-8000-000000000001");
    private static final UUID VERSION_ID = UUID.fromString(
            "30000000-0000-4000-8000-000000000001");
    private static final UUID SECTION_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final UUID TIMESLOT_ID = UUID.fromString(
            "60000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-15T00:00:00Z");

    @Mock
    private AuraAuthorizationService authorizationService;

    @Mock
    private AuraJdbcRepository repository;

    @Mock
    private AuraReadinessValidator readinessValidator;

    @Mock
    private AuraSolverService solverService;

    @Mock
    private AuraClashDetector clashDetector;

    private AuraService service;

    @BeforeEach
    void setUp() {
        service = new AuraService(
                authorizationService,
                repository,
                readinessValidator,
                solverService,
                clashDetector,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void startGeneration_rejectsWhenRunAlreadyActiveForTerm() {
        when(repository.findTerm(TERM_ID)).thenReturn(Optional.of(term()));
        when(readinessValidator.validate(TERM_ID)).thenReturn(ready());
        when(repository.hasActiveGenerationRun(TERM_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.startGeneration(
                USER_ID,
                TERM_ID,
                new AuraDtos.GenerateTimetableRequest(30, "test run")))
                .isInstanceOf(AuraStateException.class)
                .hasMessage("A generation run is already active for this term.");

        verify(repository, never()).insertRun(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void publishVersion_rejectsNonDraftVersion() {
        when(repository.findVersion(VERSION_ID)).thenReturn(Optional.of(
                version("PUBLISHED")));

        assertThatThrownBy(() -> service.publishVersion(USER_ID, VERSION_ID))
                .isInstanceOf(AuraStateException.class)
                .hasMessage("Only draft timetable versions can be published.");

        verify(repository, never()).publishVersion(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishVersion_rejectsOpenHardClashes() {
        when(repository.findVersion(VERSION_ID)).thenReturn(Optional.of(
                version("DRAFT")));
        when(repository.countOpenHardClashes(VERSION_ID)).thenReturn(2L);

        assertThatThrownBy(() -> service.publishVersion(USER_ID, VERSION_ID))
                .isInstanceOf(AuraStateException.class)
                .hasMessage("Resolve all hard timetable clashes before publishing.");

        verify(repository, never()).publishVersion(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void upsertSectionAvailability_rejectsCrossUniversityTimeslot() {
        when(repository.sectionAndTimeslotShareUniversity(
                SECTION_ID,
                TIMESLOT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.upsertSectionAvailability(
                USER_ID,
                new AuraDtos.CreateSectionAvailabilityRequest(
                        SECTION_ID,
                        TIMESLOT_ID,
                        "UNAVAILABLE",
                        "Shared hall exam setup")))
                .isInstanceOf(AuraStateException.class)
                .hasMessage("Section availability must use a timeslot from the same university.");

        verify(repository, never()).upsertSectionAvailability(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private TermResponse term() {
        return new TermResponse(
                TERM_ID,
                UUID.fromString("40000000-0000-4000-8000-000000000001"),
                "FALL-2026",
                "Fall 2026",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                "DRAFT",
                NOW,
                NOW);
    }

    private ReadinessResponse ready() {
        return new ReadinessResponse(
                TERM_ID,
                true,
                List.of(),
                4,
                20,
                6,
                12,
                24);
    }

    private TimetableVersionResponse version(String status) {
        return new TimetableVersionResponse(
                VERSION_ID,
                TERM_ID,
                null,
                1,
                status,
                "0hard/0medium/0soft",
                null,
                NOW,
                null);
    }
}
