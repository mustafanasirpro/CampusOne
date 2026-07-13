package com.campusone.lostfound.dto.response;

import java.util.Map;

public record LostFoundStatsResponse(
        Map<String, Long> statusCounts) {
}
