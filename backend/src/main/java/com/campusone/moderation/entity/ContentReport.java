package com.campusone.moderation.entity;

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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_reports")
public class ContentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reporter_user_id",
            nullable = false,
            updatable = false)
    private User reporter;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 60)
    private ModerationTargetType targetType;

    @NotNull
    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private ReportReason reason;

    @Size(max = 1000)
    @Column(length = 1000)
    private String details;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus status = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Size(max = 1000)
    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(nullable = false)
    private boolean deleted;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContentReport() {
    }

    public ContentReport(
            User reporter,
            ModerationTargetType targetType,
            UUID targetId,
            ReportReason reason,
            String details) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.details = normalizeOptional(details);
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

    public void markUnderReview(
            User moderator,
            Instant reviewedAt) {
        status = ReportStatus.UNDER_REVIEW;
        reviewedBy = moderator;
        this.reviewedAt = reviewedAt;
    }

    public void resolve(
            User moderator,
            Instant reviewedAt,
            String note) {
        status = ReportStatus.RESOLVED;
        reviewedBy = moderator;
        this.reviewedAt = reviewedAt;
        resolutionNote = note.trim();
    }

    public void dismiss(
            User moderator,
            Instant reviewedAt,
            String note) {
        status = ReportStatus.DISMISSED;
        reviewedBy = moderator;
        this.reviewedAt = reviewedAt;
        resolutionNote = note.trim();
    }

    public boolean isOwnedBy(UUID userId) {
        return reporter.getId().equals(userId);
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public User getReporter() {
        return reporter;
    }

    public ModerationTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public ReportReason getReason() {
        return reason;
    }

    public String getDetails() {
        return details;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
