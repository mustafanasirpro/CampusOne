package com.campusone.lostfound.dto.response;

import java.util.UUID;

public record LostFoundImageResponse(
        UUID id,
        String imageUrl,
        String originalFilename,
        String mimeType,
        long fileSizeBytes,
        int displayOrder) {
}
