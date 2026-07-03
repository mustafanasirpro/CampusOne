package com.campusone.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.notification.dto.request.CreateNotificationRequest;
import com.campusone.notification.dto.request.NotificationSort;
import com.campusone.notification.entity.Notification;
import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import com.campusone.notification.mapper.NotificationMapper;
import com.campusone.notification.repository.NotificationRepository;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final UUID RECIPIENT_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000002");
    private static final UUID NOTIFICATION_ID = UUID.fromString(
            "70000000-0000-4000-8000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-04T08:00:00Z");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;
    private User recipient;
    private User otherUser;
    private Notification notification;

    @BeforeEach
    void setUp() {
        recipient = user(RECIPIENT_ID, "student@example.com");
        otherUser = user(OTHER_USER_ID, "other@example.com");
        notification = notification(recipient);
        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                new NotificationMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createSelfNotification_validRequest_createsForCurrentUser() {
        when(userRepository.findById(RECIPIENT_ID))
                .thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification saved = invocation.getArgument(0);
                    setPersistenceFields(saved, NOTIFICATION_ID);
                    return saved;
                });

        var response = notificationService.createSelfNotification(
                RECIPIENT_ID,
                createRequest());

        assertThat(response.id()).isEqualTo(NOTIFICATION_ID);
        assertThat(response.type())
                .isEqualTo(NotificationType.USER_REMINDER);
        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient().getId())
                .isEqualTo(RECIPIENT_ID);
    }

    @Test
    void listMyNotifications_returnsCurrentUserPage() {
        stubList(null, null, null);

        var response = notificationService.listMyNotifications(
                RECIPIENT_ID,
                null,
                null,
                null,
                0,
                20,
                NotificationSort.NEWEST);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id())
                .isEqualTo(NOTIFICATION_ID);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void listMyNotifications_filtersUnreadNotifications() {
        stubList(true, null, null);

        notificationService.listMyNotifications(
                RECIPIENT_ID,
                true,
                null,
                null,
                0,
                20,
                NotificationSort.NEWEST);

        verify(notificationRepository).findForRecipient(
                eq(RECIPIENT_ID),
                eq(true),
                eq(null),
                eq(null),
                any(Pageable.class));
    }

    @Test
    void listMyNotifications_filtersByNotificationType() {
        stubList(null, NotificationType.EVENT_REMINDER, null);

        notificationService.listMyNotifications(
                RECIPIENT_ID,
                null,
                NotificationType.EVENT_REMINDER,
                null,
                0,
                20,
                NotificationSort.NEWEST);

        verify(notificationRepository).findForRecipient(
                eq(RECIPIENT_ID),
                eq(null),
                eq(NotificationType.EVENT_REMINDER),
                eq(null),
                any(Pageable.class));
    }

    @Test
    void listMyNotifications_filtersByTargetType() {
        stubList(null, null, NotificationTargetType.EVENT);

        notificationService.listMyNotifications(
                RECIPIENT_ID,
                null,
                null,
                NotificationTargetType.EVENT,
                0,
                20,
                NotificationSort.NEWEST);

        verify(notificationRepository).findForRecipient(
                eq(RECIPIENT_ID),
                eq(null),
                eq(null),
                eq(NotificationTargetType.EVENT),
                any(Pageable.class));
    }

    @Test
    void getNotification_ownedNotification_returnsDetail() {
        when(notificationRepository.findActiveById(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        var response = notificationService.getNotification(
                RECIPIENT_ID,
                NOTIFICATION_ID);

        assertThat(response.id()).isEqualTo(NOTIFICATION_ID);
        assertThat(response.isRead()).isFalse();
    }

    @Test
    void getNotification_otherUserNotification_rejectsAccess() {
        when(notificationRepository.findActiveById(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.getNotification(
                OTHER_USER_ID,
                NOTIFICATION_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getNotification_deletedNotification_isNotFound() {
        when(notificationRepository.findActiveById(NOTIFICATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getNotification(
                RECIPIENT_ID,
                NOTIFICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUnreadCount_countsOnlyCurrentUserActiveUnreadNotifications() {
        when(notificationRepository
                .countByRecipientIdAndReadAtIsNullAndDeletedFalse(
                        RECIPIENT_ID))
                .thenReturn(4L);

        var response = notificationService.getUnreadCount(RECIPIENT_ID);

        assertThat(response.unreadCount()).isEqualTo(4);
    }

    @Test
    void markRead_unreadNotification_setsCurrentTime() {
        when(notificationRepository.findActiveByIdForUpdate(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        var response = notificationService.markRead(
                RECIPIENT_ID,
                NOTIFICATION_ID);

        assertThat(response.isRead()).isTrue();
        assertThat(response.readAt()).isEqualTo(NOW);
    }

    @Test
    void markRead_alreadyReadNotification_isIdempotent() {
        Instant originalReadAt = NOW.minusSeconds(3600);
        notification.markRead(originalReadAt);
        when(notificationRepository.findActiveByIdForUpdate(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        var response = notificationService.markRead(
                RECIPIENT_ID,
                NOTIFICATION_ID);

        assertThat(response.readAt()).isEqualTo(originalReadAt);
    }

    @Test
    void markUnread_readNotification_clearsReadTime() {
        notification.markRead(NOW.minusSeconds(3600));
        when(notificationRepository.findActiveByIdForUpdate(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        var response = notificationService.markUnread(
                RECIPIENT_ID,
                NOTIFICATION_ID);

        assertThat(response.isRead()).isFalse();
        assertThat(response.readAt()).isNull();
    }

    @Test
    void markUnread_alreadyUnreadNotification_isIdempotent() {
        when(notificationRepository.findActiveByIdForUpdate(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        var response = notificationService.markUnread(
                RECIPIENT_ID,
                NOTIFICATION_ID);

        assertThat(response.isRead()).isFalse();
    }

    @Test
    void markAllRead_returnsUpdatedCount() {
        when(notificationRepository.markAllRead(RECIPIENT_ID, NOW))
                .thenReturn(3);

        var response = notificationService.markAllRead(RECIPIENT_ID);

        assertThat(response.updatedCount()).isEqualTo(3);
    }

    @Test
    void deleteNotification_ownedNotification_softDeletes() {
        when(notificationRepository.findActiveByIdForUpdate(NOTIFICATION_ID))
                .thenReturn(Optional.of(notification));

        notificationService.deleteNotification(
                RECIPIENT_ID,
                NOTIFICATION_ID);

        assertThat(notification.isDeleted()).isTrue();
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void createNotification_internalApi_createsForAnotherUser() {
        when(userRepository.findById(OTHER_USER_ID))
                .thenReturn(Optional.of(otherUser));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification saved = invocation.getArgument(0);
                    setPersistenceFields(saved, NOTIFICATION_ID);
                    return saved;
                });

        var response = notificationService.createNotification(
                OTHER_USER_ID,
                NotificationType.DISCUSSION_REPLY,
                "New discussion reply",
                "A student replied to your database normalization question.",
                NotificationTargetType.DISCUSSION_QUESTION,
                TARGET_ID,
                "/discussions/" + TARGET_ID);

        assertThat(response.id()).isEqualTo(NOTIFICATION_ID);
        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipient().getId())
                .isEqualTo(OTHER_USER_ID);
    }

    @Test
    void createBulkNotifications_deduplicatesRecipients() {
        when(userRepository.findAllById(any()))
                .thenReturn(List.of(recipient, otherUser));
        when(notificationRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var responses = notificationService.createBulkNotifications(
                List.of(RECIPIENT_ID, OTHER_USER_ID, RECIPIENT_ID),
                NotificationType.SYSTEM,
                "Campus maintenance notice",
                "Campus services will be unavailable briefly tonight.",
                NotificationTargetType.SYSTEM,
                TARGET_ID,
                "/status");

        assertThat(responses).hasSize(2);
        verify(notificationRepository).saveAll(argThat(saved -> {
            List<UUID> recipientIds = new java.util.ArrayList<>();
            saved.forEach(item ->
                    recipientIds.add(item.getRecipient().getId()));
            return recipientIds.equals(
                    List.of(RECIPIENT_ID, OTHER_USER_ID));
        }));
    }

    private void stubList(
            Boolean unreadOnly,
            NotificationType type,
            NotificationTargetType targetType) {
        when(notificationRepository.findForRecipient(
                eq(RECIPIENT_ID),
                eq(unreadOnly),
                eq(type),
                eq(targetType),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));
    }

    private CreateNotificationRequest createRequest() {
        return new CreateNotificationRequest(
                NotificationType.USER_REMINDER,
                "Review database normalization",
                "Review the third normal form examples before class.",
                NotificationTargetType.NOTE,
                TARGET_ID,
                "/notes/" + TARGET_ID);
    }

    private Notification notification(User owner) {
        Notification result = new Notification(
                owner,
                NotificationType.EVENT_REMINDER,
                "AI workshop starts tomorrow",
                "Your registered AI workshop starts tomorrow at 10 AM.",
                NotificationTargetType.EVENT,
                TARGET_ID,
                "/events/" + TARGET_ID);
        setPersistenceFields(result, NOTIFICATION_ID);
        return result;
    }

    private User user(UUID id, String email) {
        User result = new User(
                email,
                "$2a$12$abcdefghijklmnopqrstuvabcdefghijklmnopqrstuvabcd");
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private void setPersistenceFields(
            Notification target,
            UUID id) {
        ReflectionTestUtils.setField(target, "id", id);
        ReflectionTestUtils.setField(target, "createdAt", NOW);
        ReflectionTestUtils.setField(target, "updatedAt", NOW);
    }
}
