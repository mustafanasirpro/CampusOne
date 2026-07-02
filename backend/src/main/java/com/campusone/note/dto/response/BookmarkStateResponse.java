package com.campusone.note.dto.response;

import java.util.UUID;

public record BookmarkStateResponse(
        UUID noteId,
        boolean bookmarked) {
}
