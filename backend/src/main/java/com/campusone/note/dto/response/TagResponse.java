package com.campusone.note.dto.response;

import java.util.UUID;

public record TagResponse(
        UUID id,
        String name) {
}
