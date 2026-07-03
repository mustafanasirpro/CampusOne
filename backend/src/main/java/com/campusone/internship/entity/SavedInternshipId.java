package com.campusone.internship.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SavedInternshipId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "internship_id", nullable = false)
    private UUID internshipId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected SavedInternshipId() {
    }

    public SavedInternshipId(UUID internshipId, UUID userId) {
        this.internshipId = internshipId;
        this.userId = userId;
    }

    public UUID getInternshipId() {
        return internshipId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SavedInternshipId that)) {
            return false;
        }
        return Objects.equals(internshipId, that.internshipId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internshipId, userId);
    }
}
