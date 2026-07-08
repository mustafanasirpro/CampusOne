package com.campusone.common.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.gamification.service.GamificationService;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.repository.ModeratorRepository;
import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import com.campusone.notification.service.NotificationService;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommunityIntegrationServiceTest {

    private static final UUID SUBMITTER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID MODERATOR_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000002");
    private static final UUID FALLBACK_ADMIN_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000003");
    private static final UUID NOTE_ID = UUID.fromString(
            "30000000-0000-4000-8000-000000000001");

    @Mock
    private GamificationService gamificationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ModeratorRepository moderatorRepository;

    @Mock
    private UserRepository userRepository;

    private CommunityIntegrationService integrationService;

    @BeforeEach
    void setUp() {
        integrationService = new CommunityIntegrationService(
                gamificationService,
                notificationService,
                moderatorRepository,
                userRepository,
                "admin@example.com");
    }

    @Test
    void noteSubmittedForApproval_notifiesModeratorsAndFallbackAdmins() {
        when(moderatorRepository.findActiveModeratorUserIds())
                .thenReturn(List.of(MODERATOR_ID, SUBMITTER_ID));
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(user(
                        FALLBACK_ADMIN_ID,
                        "admin@example.com")));

        integrationService.noteSubmittedForApproval(
                SUBMITTER_ID,
                NOTE_ID,
                "OOP Notes");

        verify(notificationService).createBulkNotifications(
                eq(new LinkedHashSet<>(List.of(
                        MODERATOR_ID,
                        FALLBACK_ADMIN_ID))),
                eq(NotificationType.ADMIN_MESSAGE),
                eq("New note pending approval"),
                eq("A student submitted \"OOP Notes\" for admin review."),
                eq(NotificationTargetType.NOTE),
                eq(NOTE_ID),
                eq("/admin"));
    }

    @Test
    void contentApprovalReviewed_marksAdminNotificationHandledAndNotifiesSubmitter() {
        integrationService.contentApprovalReviewed(
                SUBMITTER_ID,
                MODERATOR_ID,
                ModerationTargetType.NOTE,
                NOTE_ID,
                "OOP Notes",
                true);

        verify(notificationService).markUnreadTargetNotificationsRead(
                NotificationType.ADMIN_MESSAGE,
                NotificationTargetType.NOTE,
                NOTE_ID);
        verify(notificationService).createNotification(
                SUBMITTER_ID,
                NotificationType.ADMIN_MESSAGE,
                "Your submission was approved",
                "\"OOP Notes\" is now visible on CampusOne.",
                NotificationTargetType.NOTE,
                NOTE_ID,
                "/notes/" + NOTE_ID);
    }

    private User user(UUID userId, String email) {
        User user = new User(email, "hash");
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
