package com.campusone.internship.service;

import com.campusone.common.service.CommunityIntegrationService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.internship.dto.request.CreateInternshipRequest;
import com.campusone.internship.dto.request.InternshipSort;
import com.campusone.internship.dto.request.UpdateInternshipRequest;
import com.campusone.internship.entity.Internship;
import com.campusone.internship.entity.InternshipStatus;
import com.campusone.internship.entity.InternshipType;
import com.campusone.internship.entity.SavedInternship;
import com.campusone.internship.entity.SavedInternshipId;
import com.campusone.internship.entity.WorkMode;
import com.campusone.internship.exception.InternshipConflictException;
import com.campusone.internship.mapper.InternshipMapper;
import com.campusone.internship.repository.InternshipRepository;
import com.campusone.internship.repository.SavedInternshipRepository;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.math.BigDecimal;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InternshipServiceTest {

    private static final UUID POSTER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID STUDENT_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000002");
    private static final UUID INTERNSHIP_ID = UUID.fromString(
            "60000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-03T12:00:00Z");
    private static final Instant DEADLINE =
            Instant.parse("2026-09-30T23:59:59Z");

    @Mock
    private InternshipRepository internshipRepository;

    @Mock
    private SavedInternshipRepository savedRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunityIntegrationService integrationService;

    private InternshipService internshipService;
    private User poster;
    private User student;
    private Internship internship;

    @BeforeEach
    void setUp() {
        poster = user(POSTER_ID, "poster@example.com");
        student = user(STUDENT_ID, "student@example.com");
        internship = internship();
        internshipService = new InternshipService(
                internshipRepository,
                savedRepository,
                userRepository,
                new InternshipMapper(),
                integrationService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(savedRepository.findSavedInternshipIds(
                        any(UUID.class),
                        any()))
                .thenReturn(List.of());
    }

    @Test
    void createInternship_validRequest_createsOpenInternship() {
        when(userRepository.findById(POSTER_ID))
                .thenReturn(Optional.of(poster));
        when(internshipRepository.save(any(Internship.class)))
                .thenAnswer(invocation -> {
                    Internship saved = invocation.getArgument(0);
                    setPersistenceFields(saved);
                    return saved;
                });

        var response = internshipService.createInternship(
                POSTER_ID,
                createRequest());

        assertThat(response.id()).isEqualTo(INTERNSHIP_ID);
        assertThat(response.status()).isEqualTo(InternshipStatus.OPEN);
        assertThat(response.ownedByCurrentUser()).isTrue();
    }

    @Test
    void listInternships_returnsPage() {
        stubVisibleQuery(
                null,
                null,
                null,
                null,
                null);

        var response = internshipService.listInternships(
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20,
                InternshipSort.NEWEST);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id())
                .isEqualTo(INTERNSHIP_ID);
    }

    @Test
    void listInternships_searchNormalizesPattern() {
        stubVisibleQuery(
                null,
                null,
                null,
                null,
                "%systems%");

        internshipService.listInternships(
                null,
                null,
                null,
                null,
                null,
                " Systems ",
                0,
                20,
                InternshipSort.NEWEST);

        verify(internshipRepository).findVisibleInternships(
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq("%systems%"),
                org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    void listInternships_filtersByStatus() {
        stubVisibleQuery(
                InternshipStatus.OPEN,
                null,
                null,
                null,
                null);

        var response = internshipService.listInternships(
                null,
                InternshipStatus.OPEN,
                null,
                null,
                null,
                null,
                0,
                20,
                InternshipSort.NEWEST);

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void listInternships_filtersByInternshipType() {
        stubVisibleQuery(
                null,
                InternshipType.SUMMER,
                null,
                null,
                null);

        var response = internshipService.listInternships(
                null,
                null,
                InternshipType.SUMMER,
                null,
                null,
                null,
                0,
                20,
                InternshipSort.NEWEST);

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void listInternships_filtersByWorkMode() {
        stubVisibleQuery(
                null,
                null,
                WorkMode.HYBRID,
                null,
                null);

        var response = internshipService.listInternships(
                null,
                null,
                null,
                WorkMode.HYBRID,
                null,
                null,
                0,
                20,
                InternshipSort.NEWEST);

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void listInternships_filtersByPaidState() {
        stubVisibleQuery(
                null,
                null,
                null,
                true,
                null);

        var response = internshipService.listInternships(
                null,
                null,
                null,
                null,
                true,
                null,
                0,
                20,
                InternshipSort.NEWEST);

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void getInternship_activeInternship_returnsDetail() {
        when(internshipRepository.findActiveById(INTERNSHIP_ID))
                .thenReturn(Optional.of(internship));

        var response = internshipService.getInternship(
                INTERNSHIP_ID,
                null);

        assertThat(response.id()).isEqualTo(INTERNSHIP_ID);
    }

    @Test
    void listMyInternships_returnsPostedInternships() {
        when(internshipRepository.findPostedByUser(
                eq(POSTER_ID),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(internship)));

        var response = internshipService.listMyInternships(
                POSTER_ID,
                null,
                null,
                null,
                null,
                null,
                0,
                20,
                InternshipSort.NEWEST);

        assertThat(response.content().getFirst().ownedByCurrentUser()).isTrue();
    }

    @Test
    void updateInternship_ownerCanUpdate() {
        when(internshipRepository.findActiveByIdForUpdate(INTERNSHIP_ID))
                .thenReturn(Optional.of(internship));

        var response = internshipService.updateInternship(
                POSTER_ID,
                INTERNSHIP_ID,
                new UpdateInternshipRequest(
                        "Senior Java Backend Intern",
                        null,
                        null,
                        "Islamabad",
                        null,
                        WorkMode.REMOTE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        InternshipStatus.CLOSED));

        assertThat(response.title()).isEqualTo("Senior Java Backend Intern");
        assertThat(response.location()).isEqualTo("Islamabad");
        assertThat(response.workMode()).isEqualTo(WorkMode.REMOTE);
        assertThat(response.status()).isEqualTo(InternshipStatus.CLOSED);
    }

    @Test
    void updateInternship_nonOwnerIsRejected() {
        when(internshipRepository.findActiveByIdForUpdate(INTERNSHIP_ID))
                .thenReturn(Optional.of(internship));

        assertThatThrownBy(() -> internshipService.updateInternship(
                STUDENT_ID,
                INTERNSHIP_ID,
                new UpdateInternshipRequest(
                        "Unauthorized internship update",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteInternship_ownerSoftDeletesInternship() {
        when(internshipRepository.findActiveByIdForUpdate(INTERNSHIP_ID))
                .thenReturn(Optional.of(internship));

        internshipService.deleteInternship(POSTER_ID, INTERNSHIP_ID);

        assertThat(internship.isDeleted()).isTrue();
    }

    @Test
    void getInternship_deletedInternshipIsNotFound() {
        when(internshipRepository.findActiveById(INTERNSHIP_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> internshipService.getInternship(
                INTERNSHIP_ID,
                null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveInternship_validSave_returnsSavedState() {
        when(internshipRepository.findActiveByIdForUpdate(INTERNSHIP_ID))
                .thenReturn(Optional.of(internship));
        when(userRepository.findById(STUDENT_ID))
                .thenReturn(Optional.of(student));
        when(savedRepository.save(any(SavedInternship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = internshipService.saveInternship(
                STUDENT_ID,
                INTERNSHIP_ID);

        assertThat(response.saved()).isTrue();
        assertThat(response.internshipId()).isEqualTo(INTERNSHIP_ID);
    }

    @Test
    void saveInternship_duplicateSaveIsRejected() {
        SavedInternshipId id =
                new SavedInternshipId(INTERNSHIP_ID, STUDENT_ID);
        when(internshipRepository.findActiveByIdForUpdate(INTERNSHIP_ID))
                .thenReturn(Optional.of(internship));
        when(savedRepository.existsById(id)).thenReturn(true);

        assertThatThrownBy(() -> internshipService.saveInternship(
                STUDENT_ID,
                INTERNSHIP_ID))
                .isInstanceOf(InternshipConflictException.class)
                .extracting(exception ->
                        ((InternshipConflictException) exception).getCode())
                .isEqualTo("INTERNSHIP_ALREADY_SAVED");
    }

    @Test
    void unsaveInternship_existingSave_deletesSave() {
        SavedInternshipId id =
                new SavedInternshipId(INTERNSHIP_ID, STUDENT_ID);
        SavedInternship saved = new SavedInternship(internship, student);
        when(internshipRepository.findActiveByIdForUpdate(INTERNSHIP_ID))
                .thenReturn(Optional.of(internship));
        when(savedRepository.findById(id)).thenReturn(Optional.of(saved));

        internshipService.unsaveInternship(STUDENT_ID, INTERNSHIP_ID);

        verify(savedRepository).delete(saved);
    }

    @Test
    void unsaveInternship_notSavedIsRejected() {
        when(internshipRepository.findActiveByIdForUpdate(INTERNSHIP_ID))
                .thenReturn(Optional.of(internship));

        assertThatThrownBy(() -> internshipService.unsaveInternship(
                STUDENT_ID,
                INTERNSHIP_ID))
                .isInstanceOf(InternshipConflictException.class)
                .extracting(exception ->
                        ((InternshipConflictException) exception).getCode())
                .isEqualTo("INTERNSHIP_NOT_SAVED");
    }

    @Test
    void listSavedInternships_returnsSavedPage() {
        when(internshipRepository.findSavedByUser(
                eq(STUDENT_ID),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(internship)));
        when(savedRepository.findSavedInternshipIds(
                STUDENT_ID,
                List.of(INTERNSHIP_ID)))
                .thenReturn(List.of(INTERNSHIP_ID));

        var response = internshipService.listSavedInternships(
                STUDENT_ID,
                0,
                20,
                InternshipSort.NEWEST);

        assertThat(response.content().getFirst().savedByCurrentUser()).isTrue();
    }

    @Test
    void getSavedState_unsavedInternship_returnsFalse() {
        when(internshipRepository.findActiveById(INTERNSHIP_ID))
                .thenReturn(Optional.of(internship));

        var response = internshipService.getSavedState(
                STUDENT_ID,
                INTERNSHIP_ID);

        assertThat(response.saved()).isFalse();
        assertThat(response.userId()).isEqualTo(STUDENT_ID);
    }

    private void stubVisibleQuery(
            InternshipStatus status,
            InternshipType type,
            WorkMode mode,
            Boolean paid,
            String searchPattern) {
        when(internshipRepository.findVisibleInternships(
                eq(status),
                eq(type),
                eq(mode),
                eq(paid),
                eq(searchPattern),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(internship)));
    }

    private CreateInternshipRequest createRequest() {
        return new CreateInternshipRequest(
                "Java Backend Intern",
                "Systems Limited",
                "Build and test production-quality Spring Boot services.",
                "Lahore",
                InternshipType.SUMMER,
                WorkMode.HYBRID,
                true,
                new BigDecimal("35000.00"),
                "PKR",
                "https://example.com/apply",
                DEADLINE);
    }

    private Internship internship() {
        Internship result = new Internship(
                poster,
                "Java Backend Intern",
                "Systems Limited",
                "Build and test production-quality Spring Boot services.",
                "Lahore",
                InternshipType.SUMMER,
                WorkMode.HYBRID,
                true,
                new BigDecimal("35000.00"),
                "PKR",
                "https://example.com/apply",
                DEADLINE);
        setPersistenceFields(result);
        return result;
    }

    private User user(UUID id, String email) {
        User user = new User(email, "$2a$12$encoded-password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void setPersistenceFields(Internship target) {
        ReflectionTestUtils.setField(target, "id", INTERNSHIP_ID);
        ReflectionTestUtils.setField(target, "createdAt", NOW);
        ReflectionTestUtils.setField(target, "updatedAt", NOW);
    }
}
