package com.campusone.lostfound.dto.response;

import java.util.List;

public record LostFoundClaimPageResponse(
        List<LostFoundClaimResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
