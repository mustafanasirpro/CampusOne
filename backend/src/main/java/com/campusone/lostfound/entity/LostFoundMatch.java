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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "lost_found_matches")
public class LostFoundMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lost_item_id", nullable = false)
    private LostFoundItem lostItem;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "found_item_id", nullable = false)
    private LostFoundItem foundItem;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    private int score;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode reasons;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private LostFoundMatchStatus status = LostFoundMatchStatus.SUGGESTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_changed_by_user_id")
    private User statusChangedBy;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected LostFoundMatch() {
    }

    public LostFoundMatch(
            LostFoundItem lostItem,
            LostFoundItem foundItem,
            int score,
            JsonNode reasons) {
        this.lostItem = lostItem;
        this.foundItem = foundItem;
        this.score = score;
        this.reasons = reasons == null ? null : reasons.deepCopy();
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

    public void refreshSuggestion(int score, JsonNode reasons) {
        if (status == LostFoundMatchStatus.REJECTED) {
            return;
        }
        this.score = score;
        this.reasons = reasons == null ? null : reasons.deepCopy();
        this.status = LostFoundMatchStatus.SUGGESTED;
    }

    public void confirm(User user, Instant now) {
        status = LostFoundMatchStatus.CONFIRMED;
        statusChangedBy = user;
        statusChangedAt = now;
    }

    public void reject(User user, Instant now) {
        status = LostFoundMatchStatus.REJECTED;
        statusChangedBy = user;
        statusChangedAt = now;
    }

    public boolean involvesUser(UUID userId) {
        return lostItem.isOwnedBy(userId) || foundItem.isOwnedBy(userId);
    }

    public UUID getId() { return id; }
    public LostFoundItem getLostItem() { return lostItem; }
    public LostFoundItem getFoundItem() { return foundItem; }
    public int getScore() { return score; }
    public JsonNode getReasons() {
        return reasons == null ? null : reasons.deepCopy();
    }
    public LostFoundMatchStatus getStatus() { return status; }
    public User getStatusChangedBy() { return statusChangedBy; }
    public Instant getStatusChangedAt() { return statusChangedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
