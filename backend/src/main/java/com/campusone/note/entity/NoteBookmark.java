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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "note_bookmarks")
public class NoteBookmark {

    @EmbeddedId
    private NoteBookmarkId id;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NoteBookmark() {
    }

    public NoteBookmark(Note note, User user) {
        this.id = new NoteBookmarkId(note.getId(), user.getId());
        this.note = note;
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public NoteBookmarkId getId() {
        return id;
    }

    public Note getNote() {
        return note;
    }

    public User getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
