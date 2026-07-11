package com.campusone.note.repository;

import java.util.List;
import java.util.UUID;

public record NoteSearchResult(
        List<UUID> noteIds,
        long totalElements) {
}
