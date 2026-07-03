package com.campusone.notification.dto.request;

import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateNotificationRequest(
        @NotNull
        NotificationType type,

        @NotBlank
        @Size(min = 3, max = 160)
        String title,

        @NotBlank
        @Size(min = 3, max = 1000)
        String message,

        NotificationTargetType targetType,

        UUID targetId,

        @Size(max = 1000)
        String actionUrl) {

    public CreateNotificationRequest {
        title = trim(title);
        message = trim(message);
        actionUrl = normalizeOptional(actionUrl);
    }

    @JsonIgnore
    @AssertTrue(message = "targetType is required when targetId is provided")
    public boolean isTargetValid() {
        return targetId == null || targetType != null;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
