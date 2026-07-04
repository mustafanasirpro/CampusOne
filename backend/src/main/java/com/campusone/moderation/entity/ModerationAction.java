package com.campusone.moderation.entity;

import com.campusone.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
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
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "moderation_actions")
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "moderator_user_id",
            nullable = false,
            updatable = false)
    private User moderator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", updatable = false)
    private ContentReport report;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 60)
    private ModerationActionType actionType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 60)
    private ModerationTargetType targetType;

    @NotNull
    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @Size(max = 1000)
    @Column(length = 1000)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ModerationAction() {
    }

    public ModerationAction(
            User moderator,
            ContentReport report,
            ModerationActionType actionType,
            ModerationTargetType targetType,
            UUID targetId,
            String reason,
            JsonNode metadata) {
        this.moderator = moderator;
        this.report = report;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = normalizeOptional(reason);
        this.metadata = metadata == null ? null : metadata.deepCopy();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public User getModerator() {
        return moderator;
    }

    public ContentReport getReport() {
        return report;
    }

    public ModerationActionType getActionType() {
        return actionType;
    }

    public ModerationTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getReason() {
        return reason;
    }

    public JsonNode getMetadata() {
        return metadata == null ? null : metadata.deepCopy();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
