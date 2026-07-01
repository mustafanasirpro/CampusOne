package com.campusone.user.dto.response;

import com.campusone.user.entity.ThemePreference;

public record PreferenceResponse(
        ThemePreference theme,
        String language,
        boolean compactMode) {
}
