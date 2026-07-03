package com.campusone.notification.dto.response;

import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationDetailResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        NotificationTargetType targetType,
        UUID targetId,
        String actionUrl,
        boolean isRead,
        Instant readAt,
        Instant createdAt,
        Instant updatedAt) {
}
