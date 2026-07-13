package com.campusone.lostfound.entity;

import com.campusone.note.entity.StorageProvider;
import com.campusone.note.storage.StoredObject;
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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lost_found_item_images")
public class LostFoundItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private LostFoundItem item;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 20)
    private StorageProvider storageProvider;

    @NotBlank
    @Size(max = 100)
    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    @NotBlank
    @Size(max = 1024)
    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @NotBlank
    @Size(max = 255)
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @NotBlank
    @Size(max = 127)
    @Column(name = "mime_type", nullable = false, length = 127)
    private String mimeType;

    @Positive
    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Size(min = 64, max = 64)
    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LostFoundItemImage() {
    }

    public LostFoundItemImage(StoredObject storedObject, int displayOrder) {
        this.storageProvider = storedObject.storageProvider();
        this.bucketName = storedObject.bucketName();
        this.objectKey = storedObject.objectKey();
        this.originalFilename = storedObject.originalFilename();
        this.mimeType = storedObject.mimeType();
        this.fileSizeBytes = storedObject.sizeBytes();
        this.checksumSha256 = storedObject.checksumSha256();
        this.displayOrder = displayOrder;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void setItem(LostFoundItem item) {
        this.item = item;
    }

    public UUID getId() { return id; }
    public LostFoundItem getItem() { return item; }
    public StorageProvider getStorageProvider() { return storageProvider; }
    public String getBucketName() { return bucketName; }
    public String getObjectKey() { return objectKey; }
    public String getOriginalFilename() { return originalFilename; }
    public String getMimeType() { return mimeType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getChecksumSha256() { return checksumSha256; }
    public int getDisplayOrder() { return displayOrder; }
    public Instant getCreatedAt() { return createdAt; }
}
