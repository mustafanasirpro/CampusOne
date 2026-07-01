package com.campusone.auth.dto.response;

public record AuthResponse(
        String accessToken,
        long expiresIn,
        UserSummaryResponse user) {
}
