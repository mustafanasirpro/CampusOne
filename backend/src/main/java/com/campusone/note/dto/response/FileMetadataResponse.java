package com.campusone.note.dto.response;

import com.campusone.note.entity.FileAssetStatus;
import com.campusone.note.entity.StorageProvider;
import java.time.Instant;
import java.util.UUID;

public record FileMetadataResponse(
        UUID id,
        StorageProvider storageProvider,
        String originalFilename,
        String mimeType,
        long sizeBytes,
        FileAssetStatus status,
        Instant createdAt) {
}
