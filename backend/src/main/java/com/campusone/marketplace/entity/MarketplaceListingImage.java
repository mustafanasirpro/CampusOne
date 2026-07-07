package com.campusone.marketplace.entity;

import com.campusone.note.entity.StorageProvider;
import com.campusone.note.storage.StoredObject;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_listing_images")
public class MarketplaceListingImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false, updatable = false)
    private MarketplaceListing listing;

    @Size(max = 2048)
    @Column(name = "image_url", length = 2048, updatable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", length = 20, updatable = false)
    private StorageProvider storageProvider;

    @Size(max = 100)
    @Column(name = "bucket_name", length = 100, updatable = false)
    private String bucketName;

    @Size(max = 1024)
    @Column(name = "object_key", length = 1024, updatable = false)
    private String objectKey;

    @Size(max = 255)
    @Column(name = "original_filename", length = 255, updatable = false)
    private String originalFilename;

    @Size(max = 127)
    @Column(name = "mime_type", length = 127, updatable = false)
    private String mimeType;

    @Column(name = "size_bytes", updatable = false)
    private Long sizeBytes;

    @Size(max = 160)
    @Column(name = "alt_text", length = 160, updatable = false)
    private String altText;

    @Column(name = "display_order", nullable = false, updatable = false)
    private short displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MarketplaceListingImage() {
    }

    MarketplaceListingImage(
            MarketplaceListing listing,
            String imageUrl,
            String altText,
            int displayOrder) {
        this.listing = listing;
        this.imageUrl = imageUrl.trim();
        this.altText = normalizeOptional(altText);
        this.displayOrder = (short) displayOrder;
    }

    MarketplaceListingImage(
            MarketplaceListing listing,
            StoredObject storedObject,
            String imageUrl,
            String altText,
            int displayOrder) {
        this.listing = listing;
        this.imageUrl = normalizeOptional(imageUrl);
        this.storageProvider = storedObject.storageProvider();
        this.bucketName = storedObject.bucketName();
        this.objectKey = storedObject.objectKey();
        this.originalFilename = storedObject.originalFilename();
        this.mimeType = storedObject.mimeType();
        this.sizeBytes = storedObject.sizeBytes();
        this.altText = normalizeOptional(altText);
        this.displayOrder = (short) displayOrder;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public MarketplaceListing getListing() {
        return listing;
    }

    public String getImageUrl() {
        return imageUrl;
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

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getAltText() {
        return altText;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
