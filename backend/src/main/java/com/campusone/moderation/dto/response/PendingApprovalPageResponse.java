package com.campusone.moderation.dto.response;

import java.util.List;

public record PendingApprovalPageResponse(
        List<PendingApprovalItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
