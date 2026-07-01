package com.campusone.user.dto.request;

import com.campusone.user.entity.ProfileVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateProfileRequest(
        @Size(min = 2, max = 80)
        String fullName,

        @Size(max = 500)
        String bio,

        UUID universityId,

        UUID departmentId,

        @Min(1)
        @Max(8)
        Integer semester,

        @Size(max = 2048)
        @Pattern(regexp = "^$|(?i:https?://\\S+)$")
        String avatarUrl,

        @Size(max = 2048)
        @Pattern(regexp = "^$|(?i:https?://\\S+)$")
        String coverImageUrl,

        @Size(max = 100)
        String location,

        ProfileVisibility visibility,

        @Valid
        PreferenceUpdateRequest preferences) {

    public UpdateProfileRequest {
        fullName = trim(fullName);
        bio = trim(bio);
        avatarUrl = trim(avatarUrl);
        coverImageUrl = trim(coverImageUrl);
        location = trim(location);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
