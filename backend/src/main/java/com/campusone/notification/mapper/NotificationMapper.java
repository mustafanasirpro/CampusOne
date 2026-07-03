package com.campusone.notification.mapper;

import com.campusone.notification.dto.response.NotificationDetailResponse;
import com.campusone.notification.dto.response.NotificationSummaryResponse;
import com.campusone.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationSummaryResponse toSummary(Notification notification) {
        return new NotificationSummaryResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getActionUrl(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt());
    }

    public NotificationDetailResponse toDetail(Notification notification) {
        return new NotificationDetailResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getActionUrl(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt());
    }
}
