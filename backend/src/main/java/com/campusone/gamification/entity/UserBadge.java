package com.campusone.gamification.entity;

import com.campusone.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_badges")
public class UserBadge {

    @EmbeddedId
    private UserBadgeId id;

    @NotNull
    @MapsId("badgeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false, updatable = false)
    private Badge badge;

    @NotNull
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "awarded_at", nullable = false, updatable = false)
    private Instant awardedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 60)
    private GamificationSourceType sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    protected UserBadge() {
    }

    public UserBadge(
            Badge badge,
            User user,
            GamificationSourceType sourceType,
            UUID sourceId) {
        this.id = new UserBadgeId(badge.getId(), user.getId());
        this.badge = badge;
        this.user = user;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
    }

    @PrePersist
    void onCreate() {
        awardedAt = Instant.now();
    }

    public UserBadgeId getId() {
        return id;
    }

    public Badge getBadge() {
        return badge;
    }

    public User getUser() {
        return user;
    }

    public Instant getAwardedAt() {
        return awardedAt;
    }

    public GamificationSourceType getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }
}
