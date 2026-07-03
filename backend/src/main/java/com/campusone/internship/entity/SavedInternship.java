package com.campusone.internship.entity;

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
import java.time.Instant;

@Entity
@Table(name = "saved_internships")
public class SavedInternship {

    @EmbeddedId
    private SavedInternshipId id;

    @MapsId("internshipId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internship_id", nullable = false)
    private Internship internship;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;

    protected SavedInternship() {
    }

    public SavedInternship(Internship internship, User user) {
        this.id = new SavedInternshipId(
                internship.getId(),
                user.getId());
        this.internship = internship;
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        savedAt = Instant.now();
    }

    public SavedInternshipId getId() {
        return id;
    }

    public Internship getInternship() {
        return internship;
    }

    public User getUser() {
        return user;
    }

    public Instant getSavedAt() {
        return savedAt;
    }
}
