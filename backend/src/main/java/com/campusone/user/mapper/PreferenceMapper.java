package com.campusone.user.mapper;

import com.campusone.user.dto.response.PreferenceResponse;
import com.campusone.user.entity.ThemePreference;
import com.campusone.user.entity.UserPreference;
import org.springframework.stereotype.Component;

@Component
public class PreferenceMapper {

    public PreferenceResponse toResponse(UserPreference preference) {
        if (preference == null) {
            return defaults();
        }
        return new PreferenceResponse(
                preference.getTheme(),
                preference.getLanguage(),
                preference.isCompactMode());
    }

    public PreferenceResponse defaults() {
        return new PreferenceResponse(
                ThemePreference.SYSTEM,
                "en",
                false);
    }
}
