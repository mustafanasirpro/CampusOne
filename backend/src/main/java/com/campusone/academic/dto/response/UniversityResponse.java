package com.campusone.academic.dto.response;

import java.util.UUID;

public record UniversityResponse(
        UUID id,
        String name,
        String shortName,
        String city,
        String website,
        boolean active) {
}
