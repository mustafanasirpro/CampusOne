package com.campusone.note.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class NoteRatingId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "note_id", nullable = false)
    private UUID noteId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected NoteRatingId() {
    }

    public NoteRatingId(UUID noteId, UUID userId) {
        this.noteId = noteId;
        this.userId = userId;
    }

    public UUID getNoteId() {
        return noteId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NoteRatingId that)) {
            return false;
        }
        return Objects.equals(noteId, that.noteId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noteId, userId);
    }
}
