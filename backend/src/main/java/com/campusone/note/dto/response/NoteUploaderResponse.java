package com.campusone.note.dto.response;

import java.util.UUID;

public record NoteUploaderResponse(
        UUID userId,
        String fullName,
        String avatarUrl,
        String university) {
}
