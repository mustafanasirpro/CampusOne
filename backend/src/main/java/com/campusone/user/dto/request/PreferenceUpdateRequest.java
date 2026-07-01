package com.campusone.user.dto.request;

import com.campusone.user.entity.ThemePreference;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PreferenceUpdateRequest(
        ThemePreference theme,

        @Size(min = 2, max = 10)
        @Pattern(regexp = "^[A-Za-z]{2,3}(-[A-Za-z]{2})?$")
        String language,

        Boolean compactMode) {

    public PreferenceUpdateRequest {
        language = language == null ? null : language.trim();
    }
}
