package com.campusone.security;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AccessTokenClaims(
        UUID userId,
        String email,
        Set<String> roles,
        Instant expiresAt) {
}
