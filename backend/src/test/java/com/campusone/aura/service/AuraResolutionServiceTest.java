package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.aura.dto.AuraResolutionDtos;
import com.campusone.aura.dto.AuraResolutionDtos.ResolutionCaseResponse;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.repository.AuraResolutionRepository;
import com.campusone.aura.repository.AuraResolutionRepository.OfferingCandidate;
import com.campusone.aura.repository.AuraResolutionRepository.GroupCandidate;
import com.campusone.aura.repository.AuraResolutionRepository.RegistrationScope;
import com.campusone.aura.repository.AuraResolutionRepository.SuggestionTarget;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuraResolutionServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID UNIVERSITY_ID = UUID.randomUUID();
    private static final UUID TERM_ID = UUID.randomUUID();
    private static final UUID REGISTRATION_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFERING_ID = UUID.randomUUID();
    private static final UUID TARGET_OFFERING_ID = UUID.randomUUID();
    private static final UUID SUGGESTION_ID = UUID.randomUUID();

    @Mock AuraAuthorizationService authorizationService;
    @Mock AuraJdbcRepository auraRepository;
    @Mock AuraResolutionRepository repository;
    @Mock AuraNotificationService notificationService;

    private AuraResolutionService service;

    @BeforeEach
    void setUp() {
        service = new AuraResolutionService(
                authorizationService, auraRepository, repository,
                notificationService);
    }

    @Test
    void requestResolution_createsUniversityScopedStudentCaseAndNotifiesAdmins() {
        when(authorizationService.requireUniversity(USER_ID)).thenReturn(UNIVERSITY_ID);
        when(auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, TERM_ID, UNIVERSITY_ID)).thenReturn(true);
        when(repository.findRegistrationScope(REGISTRATION_ID))
                .thenReturn(Optional.of(registration(USER_ID)));
        when(repository.insertCase(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("REPEATER_CLASH"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(USER_ID))).thenReturn(CASE_ID);
        when(repository.findCase(CASE_ID)).thenReturn(Optional.of(caseResponse("OPEN", 0)));

        service.requestResolution(USER_ID, new AuraResolutionDtos.CreateResolutionCaseRequest(
                TERM_ID, REGISTRATION_ID, "repeater-clash", "Two classes overlap."));

        verify(notificationService).notifyUniversityAdmins(
                UNIVERSITY_ID, USER_ID, "New timetable resolution request",
                "A student timetable clash is ready for review.", CASE_ID);
    }

    @Test
    void requestResolution_deniesIdorAgainstAnotherStudentsRegistration() {
        when(authorizationService.requireUniversity(USER_ID)).thenReturn(UNIVERSITY_ID);
        when(auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, TERM_ID, UNIVERSITY_ID)).thenReturn(true);
        when(repository.findRegistrationScope(REGISTRATION_ID))
                .thenReturn(Optional.of(registration(UUID.randomUUID())));

        assertThatThrownBy(() -> service.requestResolution(
                USER_ID,
                new AuraResolutionDtos.CreateResolutionCaseRequest(
                        TERM_ID, REGISTRATION_ID, "REPEATER_CLASH", "Overlap")))
                .hasMessageContaining("registration was not found");

        verify(repository, never()).insertCase(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void analyze_ranksStudentOnlyParallelOfferingBeforeInstitutionalChanges() {
        when(authorizationService.requireAdminUniversity(ADMIN_ID))
                .thenReturn(UNIVERSITY_ID);
        when(repository.findCase(CASE_ID))
                .thenReturn(Optional.of(caseResponse("OPEN", 0)),
                        Optional.of(caseResponse("SUGGESTED", 1)));
        when(repository.findRegistrationScope(REGISTRATION_ID))
                .thenReturn(Optional.of(registration(USER_ID)));
        OfferingCandidate candidate = new OfferingCandidate(
                TARGET_OFFERING_ID, "CS101-B", "Programming",
                UUID.randomUUID(), "BSCS-1B", 10, true);
        when(repository.parallelOfferingCandidates(
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of(candidate));
        when(repository.updateCaseStatus(
                CASE_ID, 0, "OPEN", "SUGGESTED", ADMIN_ID,
                "Ranked safe student-only alternatives were generated."))
                .thenReturn(true);

        service.analyze(ADMIN_ID, CASE_ID);

        verify(repository).replaceSuggestions(CASE_ID, List.of(candidate), List.of());
        verify(repository).insertAction(
                CASE_ID, null, ADMIN_ID, "ANALYZED",
                "Generated ranked student-only transfer alternatives.");
    }

    @Test
    void apply_revalidatesCapacityAndPersonalClashesBeforeTransfer() {
        when(authorizationService.requireAdminUniversity(ADMIN_ID))
                .thenReturn(UNIVERSITY_ID);
        when(repository.findCase(CASE_ID))
                .thenReturn(Optional.of(caseResponse("APPROVED", 1)));
        when(repository.findRegistrationScope(REGISTRATION_ID))
                .thenReturn(Optional.of(registration(USER_ID)));
        when(repository.findSuggestion(CASE_ID, SUGGESTION_ID))
                .thenReturn(Optional.of(new SuggestionTarget(
                        SUGGESTION_ID, TARGET_OFFERING_ID, UUID.randomUUID(), true)));
        when(repository.parallelOfferingCandidates(
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                        new OfferingCandidate(TARGET_OFFERING_ID, "CS101-B",
                                "Programming", UUID.randomUUID(), "BSCS-1B",
                                0, true)));

        assertThatThrownBy(() -> service.apply(
                ADMIN_ID, CASE_ID,
                new AuraResolutionDtos.ResolutionDecisionRequest(
                        SUGGESTION_ID, "Apply transfer", 1)))
                .isInstanceOf(AuraStateException.class)
                .hasMessageContaining("no longer safe");

        verify(repository, never()).applyOfferingTransfer(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void apply_movesOnlyStudentToSafeAlternateLabGroup() {
        UUID groupId = UUID.randomUUID();
        when(authorizationService.requireAdminUniversity(ADMIN_ID))
                .thenReturn(UNIVERSITY_ID);
        when(repository.findCase(CASE_ID))
                .thenReturn(Optional.of(caseResponse("APPROVED", 1)),
                        Optional.of(caseResponse("APPLIED", 2)));
        when(repository.findRegistrationScope(REGISTRATION_ID))
                .thenReturn(Optional.of(registration(USER_ID)));
        when(repository.findSuggestion(CASE_ID, SUGGESTION_ID))
                .thenReturn(Optional.of(new SuggestionTarget(
                        SUGGESTION_ID, "ALTERNATE_LAB", null, null,
                        groupId, true)));
        GroupCandidate group = new GroupCandidate(
                groupId, "LAB", "L2", "Lab group L2", 4, true);
        when(repository.alternateGroupCandidates(
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of(group));

        service.apply(
                ADMIN_ID, CASE_ID,
                new AuraResolutionDtos.ResolutionDecisionRequest(
                        SUGGESTION_ID, "Use the safe lab group", 1));

        verify(repository).applyGroupTransfer(
                CASE_ID, REGISTRATION_ID,
                new SuggestionTarget(
                        SUGGESTION_ID, "ALTERNATE_LAB", null, null,
                        groupId, true),
                "LAB", ADMIN_ID, "Use the safe lab group");
        verify(repository, never()).applyOfferingTransfer(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private RegistrationScope registration(UUID studentId) {
        return new RegistrationScope(
                REGISTRATION_ID, UNIVERSITY_ID, TERM_ID, studentId,
                OFFERING_ID, UUID.randomUUID(), 0);
    }

    private ResolutionCaseResponse caseResponse(String status, long version) {
        return new ResolutionCaseResponse(
                CASE_ID, TERM_ID, USER_ID, "Student", REGISTRATION_ID,
                status, "REPEATER_CLASH", "Overlap", null, version,
                Instant.now(), Instant.now(), List.of());
    }
}
