package com.campusone.note.storage;

import com.campusone.note.entity.StorageProvider;

public record StoredObject(
        StorageProvider storageProvider,
        String bucketName,
        String objectKey,
        String originalFilename,
        String mimeType,
        long sizeBytes,
        String checksumSha256) {
}
