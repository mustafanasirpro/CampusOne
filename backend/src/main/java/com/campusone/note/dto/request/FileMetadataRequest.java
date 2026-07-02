package com.campusone.note.dto.request;

import com.campusone.note.entity.StorageProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;

public record FileMetadataRequest(
        @NotNull
        StorageProvider storageProvider,

        @NotBlank
        @Size(min = 3, max = 100)
        String bucketName,

        @NotBlank
        @Size(max = 1024)
        String objectKey,

        @NotBlank
        @Size(max = 255)
        String originalFilename,

        @NotBlank
        @Size(min = 3, max = 127)
        @Pattern(regexp = "^[^/\\s]+/[^/\\s]+$")
        String mimeType,

        @NotNull
        @Positive
        Long sizeBytes,

        @Pattern(regexp = "^[0-9a-f]{64}$")
        String checksumSha256,

        Instant expiresAt) {

    public FileMetadataRequest {
        bucketName = trim(bucketName);
        objectKey = trim(objectKey);
        originalFilename = trim(originalFilename);
        mimeType = mimeType == null
                ? null
                : mimeType.trim().toLowerCase(Locale.ROOT);
        checksumSha256 = checksumSha256 == null || checksumSha256.isBlank()
                ? null
                : checksumSha256.trim().toLowerCase(Locale.ROOT);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
