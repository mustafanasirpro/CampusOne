package com.campusone.notification.dto.response;

import java.util.List;

public record NotificationPageResponse(
        List<NotificationSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
