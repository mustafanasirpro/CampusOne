package com.campusone.moderation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.common.exception.NoteManagementAccessDeniedException;
import com.campusone.common.service.CommunityIntegrationService;
import com.campusone.discussion.repository.DiscussionQuestionRepository;
import com.campusone.event.repository.CampusEventRepository;
import com.campusone.internship.repository.InternshipRepository;
import com.campusone.lostfound.repository.LostFoundItemRepository;
import com.campusone.lostfound.service.LostFoundService;
import com.campusone.marketplace.repository.MarketplaceListingRepository;
import com.campusone.moderation.dto.response.PendingApprovalItemResponse;
import com.campusone.moderation.dto.response.ReporterSummaryResponse;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.repository.ModerationActionRepository;
import com.campusone.note.entity.Note;
import com.campusone.note.entity.NoteModerationStatus;
import com.campusone.note.repository.NoteRepository;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContentApprovalServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID SUBMITTER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000002");
    private static final UUID NOTE_ID = UUID.fromString(
            "30000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private MarketplaceListingRepository listingRepository;

    @Mock
    private CampusEventRepository eventRepository;

    @Mock
    private DiscussionQuestionRepository questionRepository;

    @Mock
    private InternshipRepository internshipRepository;

    @Mock
    private LostFoundItemRepository lostFoundItemRepository;

    @Mock
    private ModerationActionRepository actionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NoteAdminAuthorizationService adminAuthorizationService;

    @Mock
    private ModeratorAuthorizationService moderatorAuthorizationService;

    @Mock
    private CommunityIntegrationService integrationService;

    @Mock
    private LostFoundService lostFoundService;

    @Mock
    private Note note;

    private User admin;
    private User submitter;
    private ContentApprovalService contentApprovalService;

    @BeforeEach
    void setUp() {
        admin = user(ADMIN_ID, "admin@example.com");
        submitter = user(SUBMITTER_ID, "student@example.com");
        contentApprovalService = new ContentApprovalService(
                noteRepository,
                listingRepository,
                eventRepository,
                questionRepository,
                internshipRepository,
                lostFoundItemRepository,
                actionRepository,
                userRepository,
                adminAuthorizationService,
                moderatorAuthorizationService,
                integrationService,
                lostFoundService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void approveNote_marksApprovalNotificationHandledAndNotifiesSubmitter() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(noteRepository.findActiveByIdForUpdate(NOTE_ID))
                .thenReturn(Optional.of(note));
        stubNote(NoteModerationStatus.APPROVED);

        contentApprovalService.approve(
                ADMIN_ID,
                ModerationTargetType.NOTE,
                NOTE_ID);

        verify(adminAuthorizationService).requireAdmin(
                ADMIN_ID,
                "admin@example.com");
        verify(note).approve(admin, NOW);
        verify(actionRepository).save(any());
        verify(integrationService).contentApprovalReviewed(
                SUBMITTER_ID,
                ADMIN_ID,
                ModerationTargetType.NOTE,
                NOTE_ID,
                "OOP Notes",
                true);
    }

    @Test
    void approveLostFoundItem_usesModeratorAuthorizationWithoutBroadeningNoteAdminRules() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        PendingApprovalItemResponse approvalItem =
                new PendingApprovalItemResponse(
                        NOTE_ID,
                        ModerationTargetType.LOST_FOUND_ITEM,
                        "Blue umbrella",
                        "Found near the library.",
                        new ReporterSummaryResponse(SUBMITTER_ID, null),
                        NOW,
                        "PUBLISHED",
                        null,
                        "/lost-found/" + NOTE_ID);
        when(lostFoundService.approveItem(admin, NOTE_ID))
                .thenReturn(approvalItem);

        contentApprovalService.approve(
                ADMIN_ID,
                ModerationTargetType.LOST_FOUND_ITEM,
                NOTE_ID);

        verify(moderatorAuthorizationService).requireActiveModerator(ADMIN_ID);
        verify(adminAuthorizationService, never()).requireAdmin(any(), any());
        verify(lostFoundService).approveItem(admin, NOTE_ID);
        verify(integrationService).contentApprovalReviewed(
                SUBMITTER_ID,
                ADMIN_ID,
                ModerationTargetType.LOST_FOUND_ITEM,
                NOTE_ID,
                "Blue umbrella",
                true);
    }

    @Test
    void listPending_forModeratorDefaultsToLostFoundQueueOnly() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(adminAuthorizationService.canManage(ADMIN_ID, "admin@example.com"))
                .thenReturn(false);
        when(lostFoundItemRepository.findAllByStatusAndDeletedAtIsNull(
                com.campusone.lostfound.entity.LostFoundItemStatus
                        .PENDING_REVIEW,
                PageRequest.of(0, 20)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        var response = contentApprovalService.listPending(
                ADMIN_ID,
                null,
                0,
                20);

        assertThat(response.content()).isEmpty();
        verify(moderatorAuthorizationService).requireActiveModerator(ADMIN_ID);
        verify(lostFoundItemRepository).findAllByStatusAndDeletedAtIsNull(
                com.campusone.lostfound.entity.LostFoundItemStatus
                        .PENDING_REVIEW,
                PageRequest.of(0, 20));
        verify(noteRepository, never())
                .findAllByModerationStatusAndDeletedAtIsNull(any(), any());
        verify(listingRepository, never())
                .findAllByStatusAndDeletedAtIsNull(any(), any());
        verify(eventRepository, never())
                .findAllByStatusAndDeletedFalse(any(), any());
        verify(questionRepository, never())
                .findAllByStatusAndDeletedFalse(any(), any());
        verify(internshipRepository, never())
                .findAllByStatusAndDeletedFalse(any(), any());
    }

    @Test
    void approveNote_normalUserCannotApprove() {
        when(userRepository.findById(SUBMITTER_ID))
                .thenReturn(Optional.of(submitter));
        org.mockito.Mockito.doThrow(new NoteManagementAccessDeniedException())
                .when(adminAuthorizationService)
                .requireAdmin(SUBMITTER_ID, "student@example.com");

        assertThatThrownBy(() -> contentApprovalService.approve(
                SUBMITTER_ID,
                ModerationTargetType.NOTE,
                NOTE_ID))
                .isInstanceOf(NoteManagementAccessDeniedException.class);
    }

    private void stubNote(NoteModerationStatus status) {
        when(note.getId()).thenReturn(NOTE_ID);
        when(note.getTitle()).thenReturn("OOP Notes");
        when(note.getDescription()).thenReturn("Object oriented notes");
        when(note.getUploader()).thenReturn(submitter);
        when(note.getCreatedAt()).thenReturn(NOW);
        when(note.getModerationStatus()).thenReturn(status);
    }

    private User user(UUID userId, String email) {
        User user = new User(email, "hash");
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
