package com.campusone.gamification.dto.response;

import com.campusone.gamification.entity.LeaderboardPeriod;
import java.util.List;

public record LeaderboardPageResponse(
        LeaderboardPeriod period,
        List<LeaderboardEntryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
