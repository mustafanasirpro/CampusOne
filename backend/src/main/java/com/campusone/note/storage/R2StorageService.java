package com.campusone.note.storage;

import com.campusone.common.exception.StorageOperationException;
import com.campusone.note.entity.FileAsset;
import com.campusone.note.entity.FileAssetStatus;
import com.campusone.note.entity.StorageProvider;
import java.net.URI;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Year;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

public class R2StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;
    private final Clock clock;

    public R2StorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            StorageProperties properties,
            Clock clock) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public StoredObject upload(UUID ownerId, ValidatedNoteFile file) {
        String objectKey = objectKey(ownerId, file.originalFilename());
        String bucket = properties.getR2().getBucket();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(file.mimeType())
                .contentLength(file.sizeBytes())
                .contentDisposition("inline; filename=\"" + safeHeaderFilename(
                        file.originalFilename()) + "\"")
                .metadata(Map.of("sha256", file.checksumSha256()))
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(file.content()));
        } catch (SdkException exception) {
            throw new StorageOperationException(
                    "CampusOne could not store the uploaded PDF. Please try again.",
                    exception);
        }
        return new StoredObject(
                StorageProvider.S3_COMPATIBLE,
                bucket,
                objectKey,
                file.originalFilename(),
                file.mimeType(),
                file.sizeBytes(),
                file.checksumSha256());
    }

    @Override
    public String createDownloadUrl(FileAsset fileAsset) {
        requireDownloadable(fileAsset);
        String publicBaseUrl = properties.getR2().getPublicBaseUrl();
        if (!publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "")
                    + "/"
                    + fileAsset.getObjectKey();
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(fileAsset.getBucketName())
                .key(fileAsset.getObjectKey())
                .responseContentType(fileAsset.getMimeType())
                .responseContentDisposition(
                        "inline; filename=\""
                                + safeHeaderFilename(fileAsset.getOriginalFilename())
                                + "\"")
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.getDownloadUrlTtl())
                .getObjectRequest(getObjectRequest)
                .build();
        try {
            return s3Presigner.presignGetObject(presignRequest)
                    .url()
                    .toExternalForm();
        } catch (SdkException exception) {
            throw new StorageOperationException(
                    "CampusOne could not prepare this PDF for download.",
                    exception);
        }
    }

    @Override
    public void delete(StoredObject storedObject) {
        if (storedObject.storageProvider() != StorageProvider.S3_COMPATIBLE) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(storedObject.bucketName())
                    .key(storedObject.objectKey())
                    .build());
        } catch (SdkException exception) {
            throw new StorageOperationException(
                    "CampusOne could not clean up an incomplete PDF upload.",
                    exception);
        }
    }

    @Override
    public void close() {
        s3Presigner.close();
        s3Client.close();
    }

    private String objectKey(UUID ownerId, String originalFilename) {
        int year = Year.now(clock).getValue();
        return "notes/"
                + ownerId
                + "/"
                + year
                + "/"
                + UUID.randomUUID()
                + "-"
                + safeObjectFilename(originalFilename);
    }

    private String safeObjectFilename(String filename) {
        String ascii = Normalizer.normalize(filename, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("-+\\.", ".")
                .replaceAll("^[.-]+|[.-]+$", "")
                .toLowerCase(Locale.ROOT);
        if (ascii.isBlank()) {
            return "document.pdf";
        }
        if (ascii.length() <= 120) {
            return ascii;
        }
        return ascii.substring(0, 116) + ".pdf";
    }

    private String safeHeaderFilename(String filename) {
        String safe = filename.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        return safe.isBlank() ? "document.pdf" : safe;
    }

    private void requireDownloadable(FileAsset fileAsset) {
        if (fileAsset.getStatus() != FileAssetStatus.READY
                || fileAsset.getStorageProvider() != StorageProvider.S3_COMPATIBLE
                || !properties.getR2().getBucket().equals(fileAsset.getBucketName())
                || !fileAsset.getObjectKey().startsWith("notes/")) {
            throw new StorageOperationException(
                    "This note file is not available for download.");
        }
    }
}
