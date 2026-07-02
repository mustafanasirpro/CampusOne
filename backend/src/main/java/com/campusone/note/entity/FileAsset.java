package com.campusone.note.entity;

import com.campusone.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "file_assets")
public class FileAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 20, updatable = false)
    private StorageProvider storageProvider;

    @NotBlank
    @Size(min = 3, max = 100)
    @Column(name = "bucket_name", nullable = false, length = 100, updatable = false)
    private String bucketName;

    @NotBlank
    @Size(max = 1024)
    @Column(name = "object_key", nullable = false, length = 1024, updatable = false)
    private String objectKey;

    @NotBlank
    @Size(max = 255)
    @Column(name = "original_filename", nullable = false, length = 255, updatable = false)
    private String originalFilename;

    @NotBlank
    @Size(min = 3, max = 127)
    @Column(name = "mime_type", nullable = false, length = 127, updatable = false)
    private String mimeType;

    @Positive
    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Pattern(regexp = "^[0-9a-f]{64}$")
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileAssetStatus status = FileAssetStatus.PENDING;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected FileAsset() {
    }

    public FileAsset(
            User owner,
            StorageProvider storageProvider,
            String bucketName,
            String objectKey,
            String originalFilename,
            String mimeType,
            long sizeBytes,
            String checksumSha256,
            Instant expiresAt) {
        this.owner = owner;
        this.storageProvider = storageProvider;
        this.bucketName = bucketName.trim();
        this.objectKey = objectKey.trim();
        this.originalFilename = originalFilename.trim();
        this.mimeType = mimeType.trim().toLowerCase(java.util.Locale.ROOT);
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = normalizeOptional(checksumSha256);
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public FileAssetStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
