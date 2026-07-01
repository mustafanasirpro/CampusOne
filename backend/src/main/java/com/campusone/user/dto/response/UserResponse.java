package com.campusone.user.dto.response;

import com.campusone.user.entity.AccountStatus;
import com.campusone.user.entity.RoleName;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        AccountStatus accountStatus,
        boolean emailVerified,
        Set<RoleName> roles,
        Instant createdAt,
        Instant updatedAt) {
}
