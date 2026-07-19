package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.aura.dto.AuraDtos;
import com.campusone.aura.dto.AuraDtos.ReadinessResponse;
import com.campusone.aura.dto.AuraDtos.TermResponse;
import com.campusone.aura.dto.AuraDtos.TimetableVersionResponse;
import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
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
    private static final UUID UNIVERSITY_ID = UUID.fromString(
            "40000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_UNIVERSITY_ID = UUID.fromString(
            "40000000-0000-4000-8000-000000000002");
    private static final UUID SECTION_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final UUID TIMESLOT_ID = UUID.fromString(
            "60000000-0000-4000-8000-000000000001");
    private static final UUID ROOM_ID = UUID.fromString(
            "70000000-0000-4000-8000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString(
            "70000000-0000-4000-8000-000000000002");
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

    @Mock
    private AuraGenerationPersistenceService generationPersistenceService;

    @Mock
    private AuraNotificationService notificationService;

    private AuraService service;

    @BeforeEach
    void setUp() {
        service = new AuraService(
                authorizationService,
                repository,
                readinessValidator,
                solverService,
                clashDetector,
                generationPersistenceService,
                notificationService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(authorizationService.requireAdminUniversity(USER_ID))
                .thenReturn(UNIVERSITY_ID);
    }

    @Test
    void startGeneration_rejectsWhenRunAlreadyActiveForTerm() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.TERM,
                TERM_ID,
                UNIVERSITY_ID)).thenReturn(true);
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
    void createTerm_rejectsCrossUniversityRequest() {
        AuraDtos.CreateTermRequest request = new AuraDtos.CreateTermRequest(
                OTHER_UNIVERSITY_ID,
                "FALL-2026",
                "Fall 2026",
                java.time.LocalDate.of(2026, 9, 1),
                java.time.LocalDate.of(2026, 12, 31));

        assertThatThrownBy(() -> service.createTerm(USER_ID, request))
                .isInstanceOf(
                        com.campusone.common.exception.ResourceNotFoundException.class)
                .hasMessage("University scheduling data was not found.");

        verify(repository, never()).insertTerm(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listTerms_usesAuthenticatedAdminsUniversityScope() {
        when(repository.countTerms(UNIVERSITY_ID)).thenReturn(0L);
        when(repository.listTerms(UNIVERSITY_ID, 0, 20))
                .thenReturn(List.of());

        service.listTerms(USER_ID, 0, 20);

        verify(repository).countTerms(UNIVERSITY_ID);
        verify(repository).listTerms(UNIVERSITY_ID, 0, 20);
    }

    @Test
    void publishVersion_rejectsNonDraftVersion() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.VERSION,
                VERSION_ID,
                UNIVERSITY_ID)).thenReturn(true);
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
        when(repository.resourceBelongsToUniversity(
                ScopedResource.VERSION,
                VERSION_ID,
                UNIVERSITY_ID)).thenReturn(true);
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
    void publishVersion_rejectsStaleSchedulingData() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.VERSION,
                VERSION_ID,
                UNIVERSITY_ID)).thenReturn(true);
        when(repository.findVersion(VERSION_ID)).thenReturn(Optional.of(
                version("DRAFT")));
        when(repository.isVersionStale(VERSION_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.publishVersion(USER_ID, VERSION_ID))
                .isInstanceOf(AuraStateException.class)
                .hasMessageContaining("older scheduling data");

        verify(repository, never()).publishVersion(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishVersion_rejectsMissingOccurrences() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.VERSION,
                VERSION_ID,
                UNIVERSITY_ID)).thenReturn(true);
        when(repository.findVersion(VERSION_ID)).thenReturn(Optional.of(
                version("DRAFT")));
        when(repository.countExpectedOccurrences(TERM_ID)).thenReturn(8L);
        when(repository.countScheduledSessions(VERSION_ID)).thenReturn(7L);

        assertThatThrownBy(() -> service.publishVersion(USER_ID, VERSION_ID))
                .isInstanceOf(AuraStateException.class)
                .hasMessage("Schedule every required session before publishing this timetable.");

        verify(repository, never()).publishVersion(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishVersion_rejectsIncorrectPerRequirementOccurrences() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.VERSION,
                VERSION_ID,
                UNIVERSITY_ID)).thenReturn(true);
        when(repository.findVersion(VERSION_ID)).thenReturn(Optional.of(
                version("DRAFT")));
        when(repository.countExpectedOccurrences(TERM_ID)).thenReturn(8L);
        when(repository.countScheduledSessions(VERSION_ID)).thenReturn(8L);
        when(repository.countOccurrenceIntegrityViolations(VERSION_ID))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.publishVersion(USER_ID, VERSION_ID))
                .isInstanceOf(AuraStateException.class)
                .hasMessageContaining("exactly its required weekly occurrences");

        verify(repository, never()).publishVersion(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishVersion_notifiesStudentsAfterSafePublication() {
        UUID studentId = UUID.randomUUID();
        when(repository.resourceBelongsToUniversity(
                ScopedResource.VERSION,
                VERSION_ID,
                UNIVERSITY_ID)).thenReturn(true);
        when(repository.findVersion(VERSION_ID)).thenReturn(
                Optional.of(version("DRAFT")),
                Optional.of(version("PUBLISHED")));
        when(repository.activeStudentUserIds(TERM_ID))
                .thenReturn(List.of(studentId));
        when(repository.publishVersion(VERSION_ID, TERM_ID)).thenReturn(true);

        service.publishVersion(USER_ID, VERSION_ID);

        verify(repository).publishVersion(VERSION_ID, TERM_ID);
        verify(notificationService).notifyTimetablePublished(
                List.of(studentId), USER_ID, VERSION_ID);
    }

    @Test
    void upsertSectionAvailability_rejectsCrossUniversityTimeslot() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.SECTION,
                SECTION_ID,
                UNIVERSITY_ID)).thenReturn(true);
        when(repository.resourceBelongsToUniversity(
                ScopedResource.TIMESLOT,
                TIMESLOT_ID,
                UNIVERSITY_ID)).thenReturn(true);
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

    @Test
    void createCalendarException_rejectsDatesOutsideTerm() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.TERM,
                TERM_ID,
                UNIVERSITY_ID)).thenReturn(true);
        when(repository.findTerm(TERM_ID)).thenReturn(Optional.of(term()));

        AuraDtos.CreateCalendarExceptionRequest request =
                new AuraDtos.CreateCalendarExceptionRequest(
                        TERM_ID,
                        "HOLIDAY",
                        LocalDate.of(2026, 8, 31),
                        LocalDate.of(2026, 9, 1),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "University closure");

        assertThatThrownBy(() -> service.createCalendarException(USER_ID, request))
                .isInstanceOf(AuraStateException.class)
                .hasMessage("Calendar exception dates must fall within the academic term.");
    }

    @Test
    void replaceRoomFacilities_rejectsUnknownFacility() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.ROOM,
                ROOM_ID,
                UNIVERSITY_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.replaceRoomFacilities(
                USER_ID,
                ROOM_ID,
                new AuraDtos.ReplaceFacilitiesRequest(List.of("SWIMMING_POOL"))))
                .isInstanceOf(AuraStateException.class)
                .hasMessage("Choose a supported room facility.");

        verify(repository, never()).replaceRoomFacilities(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void previewMove_rejectsPublishedVersionMutation() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.SESSION,
                SESSION_ID,
                UNIVERSITY_ID)).thenReturn(true);
        when(repository.resourceBelongsToUniversity(
                ScopedResource.ROOM,
                ROOM_ID,
                UNIVERSITY_ID)).thenReturn(true);
        when(repository.resourceBelongsToUniversity(
                ScopedResource.TIMESLOT,
                TIMESLOT_ID,
                UNIVERSITY_ID)).thenReturn(true);
        when(repository.findSession(SESSION_ID))
                .thenReturn(Optional.of(session()));
        when(repository.findVersion(VERSION_ID))
                .thenReturn(Optional.of(version("PUBLISHED")));

        assertThatThrownBy(() -> service.previewMove(
                USER_ID,
                SESSION_ID,
                new AuraDtos.ManualMovePreviewRequest(ROOM_ID, TIMESLOT_ID)))
                .isInstanceOf(AuraStateException.class)
                .hasMessage("Published and archived timetable versions cannot be changed.");

        verify(repository, never()).moveSession(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
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

    private TermResponse term() {
        return new TermResponse(
                TERM_ID,
                UNIVERSITY_ID,
                "FALL-2026",
                "Fall 2026",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                "DRAFT",
                NOW,
                NOW);
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

    private SessionResponse session() {
        return new SessionResponse(
                SESSION_ID,
                VERSION_ID,
                UUID.fromString("71000000-0000-4000-8000-000000000001"),
                UUID.fromString("71000000-0000-4000-8000-000000000002"),
                "CS101",
                "Programming Fundamentals",
                SECTION_ID,
                "BSCS-1A",
                UUID.fromString("71000000-0000-4000-8000-000000000003"),
                "Dr Ahmed",
                ROOM_ID,
                "Room 101",
                "CLASSROOM",
                TIMESLOT_ID,
                1,
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(10, 0),
                false,
                "SOLVER");
    }
}
