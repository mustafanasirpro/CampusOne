package com.campusone.ai.provider;

import com.campusone.ai.entity.AiSessionMode;

public record AiProviderRequest(
        String title,
        String input,
        String context,
        int count,
        int days,
        int dailyMinutes,
        AiSessionMode mode) {
}
