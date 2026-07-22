package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.aura.dto.AuraRegistrationDtos;
import com.campusone.aura.dto.AuraRegistrationDtos.PersonalTimetableEntry;
import com.campusone.aura.dto.AuraRegistrationDtos.StudentRegistrationResponse;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.repository.AuraRegistrationRepository;
import com.campusone.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuraRegistrationServiceTest {

    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID UNIVERSITY_ID = UUID.randomUUID();
    private static final UUID TERM_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID OFFERING_ID = UUID.randomUUID();
    private static final UUID REGISTRATION_ID = UUID.randomUUID();

    @Mock
    private AuraAuthorizationService authorizationService;
    @Mock
    private AuraJdbcRepository auraRepository;
    @Mock
    private AuraRegistrationRepository registrationRepository;

    private AuraRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new AuraRegistrationService(
                authorizationService, auraRepository, registrationRepository);
    }

    @Test
    void createRegistration_normalizesTypeAndEnforcesScope() {
        stubAdminTermScope();
        var request = request(" repeater ");
        when(registrationRepository.studentBelongsToUniversity(
                STUDENT_ID, UNIVERSITY_ID)).thenReturn(true);
        when(registrationRepository.offeringBelongsToTerm(OFFERING_ID, TERM_ID))
                .thenReturn(true);
        when(registrationRepository.sectionBelongsToTermUniversity(
                null, TERM_ID, UNIVERSITY_ID)).thenReturn(true);
        when(registrationRepository.groupMatchesOfferingAndType(
                null, OFFERING_ID, "LECTURE")).thenReturn(true);
        when(registrationRepository.groupMatchesOfferingAndType(
                null, OFFERING_ID, "LAB")).thenReturn(true);
        when(registrationRepository.groupMatchesOfferingAndType(
                null, OFFERING_ID, "TUTORIAL")).thenReturn(true);
        when(registrationRepository.findRegistration(any()))
                .thenReturn(Optional.of(registration("REPEATER")));

        StudentRegistrationResponse response = service.createRegistration(
                ACTOR_ID, request);

        assertThat(response.registrationType()).isEqualTo("REPEATER");
        verify(registrationRepository).insertRegistration(
                any(), eq(UNIVERSITY_ID), eq(ACTOR_ID), eq(request), eq("REPEATER"));
    }

    @Test
    void createRegistration_rejectsStudentFromAnotherUniversity() {
        stubAdminTermScope();
        when(registrationRepository.studentBelongsToUniversity(
                STUDENT_ID, UNIVERSITY_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.createRegistration(
                ACTOR_ID, request("REPEATER")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(registrationRepository, never()).insertRegistration(
                any(), any(), any(), any(), any());
    }

    @Test
    void personalTimetable_marksOverlappingRegistrationSessions() {
        when(authorizationService.requireUniversity(STUDENT_ID))
                .thenReturn(UNIVERSITY_ID);
        when(auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, TERM_ID, UNIVERSITY_ID)).thenReturn(true);
        PersonalTimetableEntry first = entry(
                UUID.randomUUID(), "CS101", LocalTime.of(9, 0), LocalTime.of(10, 0));
        PersonalTimetableEntry second = entry(
                UUID.randomUUID(), "MTH101", LocalTime.of(9, 30), LocalTime.of(10, 30));
        when(registrationRepository.personalTimetable(TERM_ID, STUDENT_ID))
                .thenReturn(List.of(first, second));

        var response = service.personalTimetable(STUDENT_ID, TERM_ID);

        assertThat(response.clashes()).hasSize(1);
        assertThat(response.sessions()).allMatch(PersonalTimetableEntry::personalClash);
    }

    @Test
    void personalTimetable_doesNotFlagDisjointOddAndEvenWeekSessions() {
        when(authorizationService.requireUniversity(STUDENT_ID))
                .thenReturn(UNIVERSITY_ID);
        when(auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, TERM_ID, UNIVERSITY_ID)).thenReturn(true);
        PersonalTimetableEntry odd = patternedEntry(
                UUID.randomUUID(), "CS101", "ODD_WEEK");
        PersonalTimetableEntry even = patternedEntry(
                UUID.randomUUID(), "MTH101", "EVEN_WEEK");
        when(registrationRepository.personalTimetable(TERM_ID, STUDENT_ID))
                .thenReturn(List.of(odd, even));

        var response = service.personalTimetable(STUDENT_ID, TERM_ID);

        assertThat(response.clashes()).isEmpty();
        assertThat(response.sessions()).noneMatch(PersonalTimetableEntry::personalClash);
    }

    @Test
    void listMyRegistrations_cannotCrossUniversityBoundary() {
        when(authorizationService.requireUniversity(STUDENT_ID))
                .thenReturn(UNIVERSITY_ID);
        when(auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, TERM_ID, UNIVERSITY_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.listMyRegistrations(STUDENT_ID, TERM_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void personalTimetableCalendar_exportsOnlyConfiguredTeachingWeeks() {
        when(authorizationService.requireUniversity(STUDENT_ID))
                .thenReturn(UNIVERSITY_ID);
        when(auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, TERM_ID, UNIVERSITY_ID)).thenReturn(true);
        when(auraRepository.findTerm(TERM_ID)).thenReturn(Optional.of(
                new com.campusone.aura.dto.AuraDtos.TermResponse(
                        TERM_ID,
                        UNIVERSITY_ID,
                        "2026-FALL",
                        "Fall 2026",
                        LocalDate.of(2026, 9, 7),
                        LocalDate.of(2026, 9, 28),
                        "PUBLISHED",
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        0L)));
        when(auraRepository.termTimezone(TERM_ID)).thenReturn("Asia/Karachi");
        PersonalTimetableEntry oddWeeks = new PersonalTimetableEntry(
                UUID.randomUUID(), OFFERING_ID, "CS101", "Programming",
                "Instructor", "Section A", "Room 1", 1,
                LocalTime.of(9, 0), LocalTime.of(10, 0), "REPEATER",
                "ODD_WEEK", List.of(), false);
        when(registrationRepository.personalTimetable(TERM_ID, STUDENT_ID))
                .thenReturn(List.of(oddWeeks));

        String calendar = new String(service.personalTimetableCalendar(
                STUDENT_ID, TERM_ID), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(calendar).contains("BEGIN:VCALENDAR")
                .contains("DTSTART;TZID=Asia/Karachi:20260907T090000")
                .contains("DTSTART;TZID=Asia/Karachi:20260921T090000")
                .doesNotContain("20260914T090000")
                .doesNotContain("20260928T090000");
    }

    private AuraRegistrationDtos.CreateRegistrationRequest request(String type) {
        return new AuraRegistrationDtos.CreateRegistrationRequest(
                TERM_ID,
                STUDENT_ID,
                OFFERING_ID,
                type,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private void stubAdminTermScope() {
        when(authorizationService.requireAdminUniversity(ACTOR_ID))
                .thenReturn(UNIVERSITY_ID);
        when(auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, TERM_ID, UNIVERSITY_ID)).thenReturn(true);
    }

    private StudentRegistrationResponse registration(String type) {
        return new StudentRegistrationResponse(
                REGISTRATION_ID,
                TERM_ID,
                STUDENT_ID,
                "Student One",
                OFFERING_ID,
                "CS101",
                "Programming Fundamentals",
                type,
                "ACTIVE",
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                0);
    }

    private PersonalTimetableEntry entry(
            UUID sessionId,
            String courseCode,
            LocalTime startsAt,
            LocalTime endsAt) {
        return new PersonalTimetableEntry(
                sessionId,
                UUID.randomUUID(),
                courseCode,
                courseCode + " title",
                "Instructor",
                "Section A",
                "Room 1",
                1,
                startsAt,
                endsAt,
                "REPEATER",
                false);
    }

    private PersonalTimetableEntry patternedEntry(
            UUID sessionId,
            String courseCode,
            String weekPattern) {
        return new PersonalTimetableEntry(
                sessionId, UUID.randomUUID(), courseCode,
                courseCode + " title", "Instructor", "Section A", "Room 1",
                1, LocalTime.of(9, 0), LocalTime.of(10, 0), "REPEATER",
                weekPattern, List.of(), false);
    }
}
