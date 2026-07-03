package com.campusone.notification.service;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.notification.dto.request.CreateNotificationRequest;
import com.campusone.notification.dto.request.NotificationSort;
import com.campusone.notification.dto.response.NotificationBulkActionResponse;
import com.campusone.notification.dto.response.NotificationDetailResponse;
import com.campusone.notification.dto.response.NotificationPageResponse;
import com.campusone.notification.dto.response.NotificationSummaryResponse;
import com.campusone.notification.dto.response.NotificationUnreadCountResponse;
import com.campusone.notification.entity.Notification;
import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import com.campusone.notification.exception.NotificationConflictException;
import com.campusone.notification.mapper.NotificationMapper;
import com.campusone.notification.repository.NotificationRepository;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final Clock clock;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            NotificationMapper notificationMapper,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationMapper = notificationMapper;
        this.clock = clock;
    }

    @Transactional
    public NotificationDetailResponse createSelfNotification(
            UUID userId,
            CreateNotificationRequest request) {
        return createNotification(
                userId,
                request.type(),
                request.title(),
                request.message(),
                request.targetType(),
                request.targetId(),
                request.actionUrl());
    }

    @Transactional
    public NotificationDetailResponse createNotification(
            UUID recipientUserId,
            NotificationType type,
            String title,
            String message,
            NotificationTargetType targetType,
            UUID targetId,
            String actionUrl) {
        validateNotification(
                recipientUserId,
                type,
                title,
                message,
                targetType,
                targetId,
                actionUrl);
        User recipient = requireUser(recipientUserId);
        Notification notification = notificationRepository.save(
                new Notification(
                        recipient,
                        type,
                        title,
                        message,
                        targetType,
                        targetId,
                        actionUrl));
        return notificationMapper.toDetail(notification);
    }

    @Transactional
    public List<NotificationDetailResponse> createBulkNotifications(
            Collection<UUID> recipientUserIds,
            NotificationType type,
            String title,
            String message,
            NotificationTargetType targetType,
            UUID targetId,
            String actionUrl) {
        Set<UUID> uniqueRecipientIds = uniqueRecipientIds(recipientUserIds);
        if (uniqueRecipientIds.isEmpty()) {
            return List.of();
        }
        validateNotification(
                uniqueRecipientIds.iterator().next(),
                type,
                title,
                message,
                targetType,
                targetId,
                actionUrl);

        Map<UUID, User> recipients = userRepository
                .findAllById(uniqueRecipientIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        if (recipients.size() != uniqueRecipientIds.size()) {
            throw new ResourceNotFoundException(
                    "One or more notification recipients");
        }

        List<Notification> notifications = uniqueRecipientIds.stream()
                .map(recipientId -> new Notification(
                        recipients.get(recipientId),
                        type,
                        title,
                        message,
                        targetType,
                        targetId,
                        actionUrl))
                .toList();
        return notificationRepository.saveAll(notifications)
                .stream()
                .map(notificationMapper::toDetail)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse listMyNotifications(
            UUID userId,
            Boolean unreadOnly,
            NotificationType type,
            NotificationTargetType targetType,
            int page,
            int size,
            NotificationSort sort) {
        Page<Notification> notifications =
                notificationRepository.findForRecipient(
                        userId,
                        unreadOnly,
                        type,
                        targetType,
                        PageRequest.of(page, size, sort.toSort()));
        List<NotificationSummaryResponse> content =
                notifications.getContent().stream()
                        .map(notificationMapper::toSummary)
                        .toList();
        return new NotificationPageResponse(
                content,
                notifications.getNumber(),
                notifications.getSize(),
                notifications.getTotalElements(),
                notifications.getTotalPages(),
                notifications.isFirst(),
                notifications.isLast());
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getNotification(
            UUID userId,
            UUID notificationId) {
        Notification notification = requireNotification(notificationId);
        requireOwner(notification, userId);
        return notificationMapper.toDetail(notification);
    }

    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount(UUID userId) {
        return new NotificationUnreadCountResponse(
                notificationRepository
                        .countByRecipientIdAndReadAtIsNullAndDeletedFalse(
                                userId));
    }

    @Transactional
    public NotificationDetailResponse markRead(
            UUID userId,
            UUID notificationId) {
        Notification notification =
                requireNotificationForUpdate(notificationId);
        requireOwner(notification, userId);
        notification.markRead(clock.instant());
        return notificationMapper.toDetail(notification);
    }

    @Transactional
    public NotificationDetailResponse markUnread(
            UUID userId,
            UUID notificationId) {
        Notification notification =
                requireNotificationForUpdate(notificationId);
        requireOwner(notification, userId);
        notification.markUnread();
        return notificationMapper.toDetail(notification);
    }

    @Transactional
    public NotificationBulkActionResponse markAllRead(UUID userId) {
        int updatedCount = notificationRepository.markAllRead(
                userId,
                clock.instant());
        return new NotificationBulkActionResponse(updatedCount);
    }

    @Transactional
    public void deleteNotification(
            UUID userId,
            UUID notificationId) {
        Notification notification =
                requireNotificationForUpdate(notificationId);
        requireOwner(notification, userId);
        notification.softDelete();
    }

    private Set<UUID> uniqueRecipientIds(
            Collection<UUID> recipientUserIds) {
        if (recipientUserIds == null) {
            throw conflict(
                    "INVALID_NOTIFICATION_RECIPIENTS",
                    "Recipient user IDs are required.");
        }
        LinkedHashSet<UUID> uniqueIds =
                new LinkedHashSet<>(recipientUserIds);
        if (uniqueIds.contains(null)) {
            throw conflict(
                    "INVALID_NOTIFICATION_RECIPIENTS",
                    "Recipient user IDs cannot contain null values.");
        }
        return uniqueIds;
    }

    private void validateNotification(
            UUID recipientUserId,
            NotificationType type,
            String title,
            String message,
            NotificationTargetType targetType,
            UUID targetId,
            String actionUrl) {
        if (recipientUserId == null) {
            throw conflict(
                    "INVALID_NOTIFICATION_RECIPIENT",
                    "A recipient user ID is required.");
        }
        if (type == null) {
            throw conflict(
                    "INVALID_NOTIFICATION_TYPE",
                    "A notification type is required.");
        }
        if (title == null
                || title.trim().length() < 3
                || title.trim().length() > 160) {
            throw conflict(
                    "INVALID_NOTIFICATION_CONTENT",
                    "Notification title must contain 3 to 160 characters.");
        }
        if (message == null
                || message.trim().length() < 3
                || message.trim().length() > 1000) {
            throw conflict(
                    "INVALID_NOTIFICATION_CONTENT",
                    "Notification message must contain 3 to 1000 characters.");
        }
        if (actionUrl != null && actionUrl.trim().length() > 1000) {
            throw conflict(
                    "INVALID_NOTIFICATION_ACTION_URL",
                    "Notification action URL cannot exceed 1000 characters.");
        }
        if (targetId != null && targetType == null) {
            throw conflict(
                    "INVALID_NOTIFICATION_TARGET",
                    "A target type is required when a target ID is provided.");
        }
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
    }

    private Notification requireNotification(UUID notificationId) {
        return notificationRepository.findActiveById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification"));
    }

    private Notification requireNotificationForUpdate(UUID notificationId) {
        return notificationRepository.findActiveByIdForUpdate(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification"));
    }

    private void requireOwner(Notification notification, UUID userId) {
        if (!notification.isOwnedBy(userId)) {
            throw new AccessDeniedException(
                    "Only the notification recipient may access this notification.");
        }
    }

    private NotificationConflictException conflict(
            String code,
            String message) {
        return new NotificationConflictException(code, message);
    }
}
