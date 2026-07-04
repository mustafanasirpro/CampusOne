package com.campusone.gamification.dto.response;

import java.util.List;

public record XpHistoryPageResponse(
        List<XpTransactionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
