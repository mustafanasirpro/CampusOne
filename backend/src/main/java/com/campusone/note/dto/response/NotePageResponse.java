package com.campusone.note.dto.response;

import java.util.List;

public record NotePageResponse(
        List<NoteSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
