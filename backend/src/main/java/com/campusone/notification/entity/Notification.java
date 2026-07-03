package com.campusone.notification.entity;

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
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false, updatable = false)
    private User recipient;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @NotBlank
    @Size(min = 3, max = 160)
    @Column(nullable = false, length = 160)
    private String title;

    @NotBlank
    @Size(min = 3, max = 1000)
    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 50)
    private NotificationTargetType targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Size(max = 1000)
    @Column(name = "action_url", length = 1000)
    private String actionUrl;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(nullable = false)
    private boolean deleted;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Notification() {
    }

    public Notification(
            User recipient,
            NotificationType type,
            String title,
            String message,
            NotificationTargetType targetType,
            UUID targetId,
            String actionUrl) {
        this.recipient = recipient;
        this.type = type;
        this.title = title.trim();
        this.message = message.trim();
        this.targetType = targetType;
        this.targetId = targetId;
        this.actionUrl = normalizeOptional(actionUrl);
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

    public void markRead(Instant instant) {
        if (readAt == null) {
            readAt = instant;
        }
    }

    public void markUnread() {
        readAt = null;
    }

    public void softDelete() {
        deleted = true;
    }

    public boolean isOwnedBy(UUID userId) {
        return recipient.getId().equals(userId);
    }

    public boolean isRead() {
        return readAt != null;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public User getRecipient() {
        return recipient;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public NotificationTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public Instant getReadAt() {
        return readAt;
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
