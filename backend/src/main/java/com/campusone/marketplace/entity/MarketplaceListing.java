package com.campusone.marketplace.entity;

import com.campusone.user.entity.User;
import com.campusone.note.storage.StoredObject;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "marketplace_listings")
public class MarketplaceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false, updatable = false)
    private User seller;

    @NotBlank
    @Size(min = 5, max = 160)
    @Column(nullable = false, length = 160)
    private String title;

    @NotBlank
    @Size(min = 10, max = 5000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MarketplaceCategory category;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @DecimalMax("10000000.00")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    private String currency;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false, length = 16)
    private MarketplaceItemCondition condition;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MarketplaceListingStatus status = MarketplaceListingStatus.ACTIVE;

    @OneToMany(
            mappedBy = "listing",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = 50)
    private List<MarketplaceListingImage> images = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected MarketplaceListing() {
    }

    public MarketplaceListing(
            User seller,
            String title,
            String description,
            MarketplaceCategory category,
            BigDecimal price,
            String currency,
            MarketplaceItemCondition condition) {
        this.seller = seller;
        this.title = title.trim();
        this.description = description.trim();
        this.category = category;
        this.price = price;
        this.currency = currency.trim().toUpperCase(Locale.ROOT);
        this.condition = condition;
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

    public void update(
            String title,
            String description,
            MarketplaceCategory category,
            BigDecimal price,
            String currency,
            MarketplaceItemCondition condition,
            MarketplaceListingStatus status) {
        if (title != null) {
            this.title = title.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
        if (category != null) {
            this.category = category;
        }
        if (price != null) {
            this.price = price;
        }
        if (currency != null) {
            this.currency = currency.trim().toUpperCase(Locale.ROOT);
        }
        if (condition != null) {
            this.condition = condition;
        }
        if (status != null) {
            this.status = status;
        }
    }

    public void clearImages() {
        images.clear();
    }

    public void addImage(String imageUrl, String altText, int displayOrder) {
        images.add(new MarketplaceListingImage(
                this,
                imageUrl,
                altText,
                displayOrder));
    }

    public void addUploadedImage(
            StoredObject storedObject,
            String imageUrl,
            String altText,
            int displayOrder) {
        images.add(new MarketplaceListingImage(
                this,
                storedObject,
                imageUrl,
                altText,
                displayOrder));
    }

    public void softDelete(Instant deletedAt) {
        status = MarketplaceListingStatus.DELETED;
        this.deletedAt = deletedAt;
    }

    public void submitForReview() {
        status = MarketplaceListingStatus.PENDING_REVIEW;
    }

    public void approve() {
        status = MarketplaceListingStatus.ACTIVE;
    }

    public void reject() {
        status = MarketplaceListingStatus.REJECTED;
    }

    public boolean isOwnedBy(UUID userId) {
        return seller.getId().equals(userId);
    }

    public UUID getId() {
        return id;
    }

    public User getSeller() {
        return seller;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public MarketplaceCategory getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public MarketplaceItemCondition getCondition() {
        return condition;
    }

    public MarketplaceListingStatus getStatus() {
        return status;
    }

    public List<MarketplaceListingImage> getImages() {
        return images;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public long getVersion() {
        return version;
    }
}
