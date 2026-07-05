package com.campusone.note.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.note.entity.FileAsset;
import com.campusone.note.entity.FileAssetStatus;
import com.campusone.note.entity.StorageProvider;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class R2StorageServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private FileAsset fileAsset;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private StorageProperties properties;
    private R2StorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setProvider("r2");
        properties.getR2().setBucket("campusone-notes");
        properties.getR2().setPublicBaseUrl("https://files.campusone.example/");
        storageService = new R2StorageService(
                s3Client,
                s3Presigner,
                properties,
                Clock.fixed(
                        Instant.parse("2026-07-05T12:00:00Z"),
                        ZoneOffset.UTC));
    }

    @Test
    void upload_usesOwnerScopedSafeObjectKeyAndDerivedMetadata() {
        byte[] content = "%PDF-1.7\nNotes".getBytes(StandardCharsets.US_ASCII);
        ValidatedNoteFile file = new ValidatedNoteFile(
                "OOP Final (2026).pdf",
                "application/pdf",
                content,
                "a".repeat(64));

        StoredObject storedObject = storageService.upload(USER_ID, file);

        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(
                requestCaptor.capture(),
                any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket())
                .isEqualTo("campusone-notes");
        assertThat(requestCaptor.getValue().key())
                .matches("notes/" + USER_ID + "/2026/[0-9a-f-]+-oop-final-2026.pdf");
        assertThat(storedObject.storageProvider())
                .isEqualTo(StorageProvider.S3_COMPATIBLE);
        assertThat(storedObject.checksumSha256()).isEqualTo("a".repeat(64));
    }

    @Test
    void createDownloadUrl_readyManagedObject_usesPublicBaseUrl() {
        when(fileAsset.getStatus()).thenReturn(FileAssetStatus.READY);
        when(fileAsset.getStorageProvider())
                .thenReturn(StorageProvider.S3_COMPATIBLE);
        when(fileAsset.getBucketName()).thenReturn("campusone-notes");
        when(fileAsset.getObjectKey())
                .thenReturn("notes/user/2026/file.pdf");

        String url = storageService.createDownloadUrl(fileAsset);

        assertThat(url).isEqualTo(
                "https://files.campusone.example/notes/user/2026/file.pdf");
    }

    @Test
    void createDownloadUrl_privateBucket_returnsShortLivedPresignedUrl()
            throws Exception {
        properties.getR2().setPublicBaseUrl("");
        when(fileAsset.getStatus()).thenReturn(FileAssetStatus.READY);
        when(fileAsset.getStorageProvider())
                .thenReturn(StorageProvider.S3_COMPATIBLE);
        when(fileAsset.getBucketName()).thenReturn("campusone-notes");
        when(fileAsset.getObjectKey())
                .thenReturn("notes/user/2026/file.pdf");
        when(fileAsset.getMimeType()).thenReturn("application/pdf");
        when(fileAsset.getOriginalFilename()).thenReturn("file.pdf");
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedGetObjectRequest);
        when(presignedGetObjectRequest.url()).thenReturn(
                java.net.URI.create(
                        "https://account.r2.cloudflarestorage.com/"
                                + "campusone-notes/notes/user/2026/file.pdf"
                                + "?X-Amz-Signature=test")
                        .toURL());

        String url = storageService.createDownloadUrl(fileAsset);

        assertThat(url).contains("X-Amz-Signature=test");
        verify(s3Presigner).presignGetObject(
                any(GetObjectPresignRequest.class));
    }
}
