package com.campusone.auth.service;

import com.campusone.auth.entity.RefreshToken;
import com.campusone.user.entity.User;
import java.time.Instant;

public record IssuedRefreshToken(
        String token,
        Instant expiresAt,
        User user,
        RefreshToken refreshToken) {

    public IssuedRefreshToken(
            String token,
            Instant expiresAt,
            User user) {
        this(token, expiresAt, user, null);
    }
}
