package com.campusone.lostfound.entity;

import com.campusone.academic.entity.University;
import com.campusone.user.entity.User;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "lost_found_items")
public class LostFoundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_user_id", nullable = false, updatable = false)
    private User reporter;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "university_id", nullable = false, updatable = false)
    private University university;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 16)
    private LostFoundItemType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LostFoundCategory category;

    @NotBlank
    @Size(min = 5, max = 160)
    @Column(nullable = false, length = 160)
    private String title;

    @NotBlank
    @Size(min = 10, max = 2000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Size(min = 2, max = 255)
    @Column(name = "location_text", nullable = false, length = 255)
    private String locationText;

    @NotNull
    @Column(name = "item_date", nullable = false)
    private LocalDate itemDate;

    @Size(max = 80)
    @Column(length = 80)
    private String brand;

    @Size(max = 60)
    @Column(length = 60)
    private String color;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LostFoundItemStatus status = LostFoundItemStatus.PENDING_REVIEW;

    @Size(max = 500)
    @Column(name = "moderation_reason", length = 500)
    private String moderationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by_user_id")
    private User moderatedBy;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = 50)
    private List<LostFoundItemImage> images = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected LostFoundItem() {
    }

    public LostFoundItem(
            User reporter,
            University university,
            LostFoundItemType type,
            LostFoundCategory category,
            String title,
            String description,
            String locationText,
            LocalDate itemDate,
            String brand,
            String color) {
        this.reporter = reporter;
        this.university = university;
        this.type = type;
        this.category = category;
        this.title = title.trim();
        this.description = description.trim();
        this.locationText = locationText.trim();
        this.itemDate = itemDate;
        this.brand = normalizeOptional(brand);
        this.color = normalizeOptional(color);
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
            LostFoundCategory category,
            String title,
            String description,
            String locationText,
            LocalDate itemDate,
            String brand,
            String color) {
        if (category != null) this.category = category;
        if (title != null) this.title = title.trim();
        if (description != null) this.description = description.trim();
        if (locationText != null) this.locationText = locationText.trim();
        if (itemDate != null) this.itemDate = itemDate;
        if (brand != null) this.brand = normalizeOptional(brand);
        if (color != null) this.color = normalizeOptional(color);
    }

    public void replaceImages(List<LostFoundItemImage> replacementImages) {
        images.clear();
        replacementImages.forEach(image -> image.setItem(this));
        images.addAll(replacementImages);
    }

    public void approve(User moderator, Instant now, Instant expiresAt) {
        status = LostFoundItemStatus.PUBLISHED;
        moderationReason = null;
        moderatedBy = moderator;
        moderatedAt = now;
        publishedAt = publishedAt == null ? now : publishedAt;
        this.expiresAt = expiresAt;
    }

    public void reject(User moderator, String reason, Instant now) {
        status = LostFoundItemStatus.REJECTED;
        moderationReason = reason;
        moderatedBy = moderator;
        moderatedAt = now;
    }

    public void resubmitForReview() {
        status = LostFoundItemStatus.PENDING_REVIEW;
        moderationReason = null;
        moderatedBy = null;
        moderatedAt = null;
    }

    public void startClaim() {
        if (status == LostFoundItemStatus.PUBLISHED) {
            status = LostFoundItemStatus.CLAIM_IN_PROGRESS;
        }
    }

    public void reopenAfterRejectedClaim() {
        if (status == LostFoundItemStatus.CLAIM_IN_PROGRESS) {
            status = LostFoundItemStatus.PUBLISHED;
        }
    }

    public void reopenPublished() {
        status = LostFoundItemStatus.PUBLISHED;
    }

    public void resolve(Instant now) {
        status = LostFoundItemStatus.RESOLVED;
        resolvedAt = now;
    }

    public void close() {
        status = LostFoundItemStatus.CLOSED;
    }

    public void archive() {
        status = LostFoundItemStatus.ARCHIVED;
    }

    public void softDelete(Instant now) {
        status = LostFoundItemStatus.DELETED;
        deletedAt = now;
    }

    public boolean isOwnedBy(UUID userId) {
        return reporter.getId().equals(userId);
    }

    public boolean isSameUniversity(UUID universityId) {
        return university.getId().equals(universityId);
    }

    public boolean isPubliclyVisibleAt(Instant now) {
        return deletedAt == null
                && status == LostFoundItemStatus.PUBLISHED
                && (expiresAt == null || expiresAt.isAfter(now));
    }

    public boolean isClaimableAt(Instant now) {
        return isPubliclyVisibleAt(now);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public UUID getId() { return id; }
    public User getReporter() { return reporter; }
    public University getUniversity() { return university; }
    public LostFoundItemType getType() { return type; }
    public LostFoundCategory getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocationText() { return locationText; }
    public LocalDate getItemDate() { return itemDate; }
    public String getBrand() { return brand; }
    public String getColor() { return color; }
    public LostFoundItemStatus getStatus() { return status; }
    public String getModerationReason() { return moderationReason; }
    public User getModeratedBy() { return moderatedBy; }
    public Instant getModeratedAt() { return moderatedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public List<LostFoundItemImage> getImages() { return images; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
