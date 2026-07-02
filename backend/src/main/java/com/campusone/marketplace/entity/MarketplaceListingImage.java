package com.campusone.marketplace.entity;

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
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    @Size(max = 2048)
    @Column(name = "image_url", nullable = false, length = 2048, updatable = false)
    private String imageUrl;

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
