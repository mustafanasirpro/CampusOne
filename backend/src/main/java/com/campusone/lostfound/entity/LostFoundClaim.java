package com.campusone.lostfound.entity;

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
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lost_found_claims")
public class LostFoundClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private LostFoundItem item;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claimant_user_id", nullable = false, updatable = false)
    private User claimant;

    @NotBlank
    @Size(min = 10, max = 2000)
    @Column(name = "proof_text", nullable = false, length = 2000)
    private String proofText;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private LostFoundClaimStatus status = LostFoundClaimStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Size(max = 1000)
    @Column(name = "reviewer_note", length = 1000)
    private String reviewerNote;

    @Size(max = 1000)
    @Column(name = "handover_note", length = 1000)
    private String handoverNote;

    @Column(name = "reporter_handover_confirmed_at")
    private Instant reporterHandoverConfirmedAt;

    @Column(name = "claimant_handover_confirmed_at")
    private Instant claimantHandoverConfirmedAt;

    @Column(name = "handover_completed_at")
    private Instant handoverCompletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected LostFoundClaim() {
    }

    public LostFoundClaim(
            LostFoundItem item,
            User claimant,
            String proofText) {
        this.item = item;
        this.claimant = claimant;
        this.proofText = proofText.trim();
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

    public void approve(User reviewer, String note, Instant now) {
        status = LostFoundClaimStatus.APPROVED;
        reviewedBy = reviewer;
        reviewerNote = normalize(note);
        reviewedAt = now;
    }

    public void reject(User reviewer, String note, Instant now) {
        status = LostFoundClaimStatus.REJECTED;
        reviewedBy = reviewer;
        reviewerNote = normalize(note);
        reviewedAt = now;
    }

    public void cancel() {
        status = LostFoundClaimStatus.CANCELLED;
    }

    public void confirmReporterHandover(Instant now) {
        reporterHandoverConfirmedAt = reporterHandoverConfirmedAt == null
                ? now
                : reporterHandoverConfirmedAt;
    }

    public void confirmClaimantHandover(Instant now) {
        claimantHandoverConfirmedAt = claimantHandoverConfirmedAt == null
                ? now
                : claimantHandoverConfirmedAt;
    }

    public boolean hasBothHandoverConfirmations() {
        return reporterHandoverConfirmedAt != null
                && claimantHandoverConfirmedAt != null;
    }

    public void complete(String note, Instant now) {
        status = LostFoundClaimStatus.COMPLETED;
        handoverNote = normalize(note);
        handoverCompletedAt = now;
    }

    public boolean isClaimant(UUID userId) {
        return claimant.getId().equals(userId);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public LostFoundItem getItem() { return item; }
    public User getClaimant() { return claimant; }
    public String getProofText() { return proofText; }
    public LostFoundClaimStatus getStatus() { return status; }
    public User getReviewedBy() { return reviewedBy; }
    public String getReviewerNote() { return reviewerNote; }
    public String getHandoverNote() { return handoverNote; }
    public Instant getReporterHandoverConfirmedAt() {
        return reporterHandoverConfirmedAt;
    }
    public Instant getClaimantHandoverConfirmedAt() {
        return claimantHandoverConfirmedAt;
    }
    public Instant getHandoverCompletedAt() { return handoverCompletedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
