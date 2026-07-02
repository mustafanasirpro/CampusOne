package com.campusone.note.entity;

import com.campusone.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "note_ratings")
public class NoteRating {

    @EmbeddedId
    private NoteRatingId id;

    @NotNull
    @MapsId("noteId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @NotNull
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private short rating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected NoteRating() {
    }

    public NoteRating(Note note, User user, int rating) {
        this.id = new NoteRatingId(note.getId(), user.getId());
        this.note = note;
        this.user = user;
        this.rating = (short) rating;
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

    public void updateRating(int rating) {
        this.rating = (short) rating;
    }

    public NoteRatingId getId() {
        return id;
    }

    public Note getNote() {
        return note;
    }

    public User getUser() {
        return user;
    }

    public int getRating() {
        return rating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
