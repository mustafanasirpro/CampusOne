package com.campusone.gamification.entity;

import com.campusone.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "gamification_profiles")
public class GamificationProfile {

    private static final ZoneId STREAK_ZONE =
            ZoneId.of("Asia/Karachi");

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @NotNull
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @PositiveOrZero
    @Column(name = "total_xp", nullable = false)
    private int totalXp;

    @Column(nullable = false)
    private int level = 1;

    @PositiveOrZero
    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @PositiveOrZero
    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GamificationProfile() {
    }

    public GamificationProfile(User user) {
        this.user = user;
        this.userId = user.getId();
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

    public void awardXp(
            int points,
            int recalculatedLevel,
            Instant activityAt) {
        totalXp = Math.addExact(totalXp, points);
        level = recalculatedLevel;
        updateStreak(activityAt);
        lastActivityAt = activityAt;
    }

    private void updateStreak(Instant activityAt) {
        LocalDate activityDate = activityAt
                .atZone(STREAK_ZONE)
                .toLocalDate();
        if (lastActivityAt == null) {
            currentStreak = 1;
        } else {
            LocalDate lastActivityDate = lastActivityAt
                    .atZone(STREAK_ZONE)
                    .toLocalDate();
            long days = ChronoUnit.DAYS.between(
                    lastActivityDate,
                    activityDate);
            if (days == 1) {
                currentStreak = Math.addExact(currentStreak, 1);
            } else if (days > 1 || days < 0) {
                currentStreak = 1;
            }
        }
        longestStreak = Math.max(longestStreak, currentStreak);
    }

    public UUID getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public int getLevel() {
        return level;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
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
