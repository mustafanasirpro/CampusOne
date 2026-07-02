package com.campusone.note.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DownloadEventResponse(
        UUID eventId,
        UUID noteId,
        Instant downloadedAt,
        long downloadCount) {
}
