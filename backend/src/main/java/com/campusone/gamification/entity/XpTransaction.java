package com.campusone.gamification.entity;

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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "xp_transactions")
public class XpTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 60)
    private GamificationActionType actionType;

    @Positive
    @Column(nullable = false)
    private int points;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 60)
    private GamificationSourceType sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected XpTransaction() {
    }

    public XpTransaction(
            User user,
            GamificationActionType actionType,
            int points,
            GamificationSourceType sourceType,
            UUID sourceId,
            String description) {
        this.user = user;
        this.actionType = actionType;
        this.points = points;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.description = normalize(description);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public GamificationActionType getActionType() {
        return actionType;
    }

    public int getPoints() {
        return points;
    }

    public GamificationSourceType getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
