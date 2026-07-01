package com.campusone.auth.dto.response;

import java.util.Set;

public record UserSummaryResponse(
        String fullName,
        String email,
        Set<String> roles) {
}
