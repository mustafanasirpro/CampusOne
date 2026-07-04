package com.campusone.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserBadgeId implements Serializable {

    @Column(name = "badge_id", nullable = false)
    private UUID badgeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected UserBadgeId() {
    }

    public UserBadgeId(UUID badgeId, UUID userId) {
        this.badgeId = badgeId;
        this.userId = userId;
    }

    public UUID getBadgeId() {
        return badgeId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UserBadgeId that)) {
            return false;
        }
        return Objects.equals(badgeId, that.badgeId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(badgeId, userId);
    }
}
