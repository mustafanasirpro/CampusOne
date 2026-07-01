package com.campusone.auth.service;

import com.campusone.auth.dto.response.AuthResponse;
import java.time.Instant;

public record AuthenticationResult(
        AuthResponse response,
        String refreshToken,
        Instant refreshTokenExpiresAt) {
}
