package com.campusone.note.entity;

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
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "note_moderation_actions")
public class NoteModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_id", nullable = false, updatable = false)
    private Note note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id", updatable = false)
    private User moderator;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private NoteModerationActionType action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20, updatable = false)
    private NoteModerationStatus previousStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20, updatable = false)
    private NoteModerationStatus newStatus;

    @Size(max = 500)
    @Column(length = 500, updatable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NoteModerationAction() {
    }

    public NoteModerationAction(
            Note note,
            User moderator,
            NoteModerationActionType action,
            NoteModerationStatus previousStatus,
            NoteModerationStatus newStatus,
            String reason) {
        this.note = note;
        this.moderator = moderator;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
    }

    public static NoteModerationAction submitted(
            Note note,
            NoteModerationStatus previousStatus) {
        return new NoteModerationAction(
                note,
                null,
                NoteModerationActionType.SUBMITTED,
                previousStatus,
                NoteModerationStatus.PENDING,
                null);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Note getNote() {
        return note;
    }

    public User getModerator() {
        return moderator;
    }

    public NoteModerationActionType getAction() {
        return action;
    }

    public NoteModerationStatus getPreviousStatus() {
        return previousStatus;
    }

    public NoteModerationStatus getNewStatus() {
        return newStatus;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
