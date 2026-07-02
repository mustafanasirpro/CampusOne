package com.campusone.discussion.entity;

import com.campusone.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "discussion_answers")
public class DiscussionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, updatable = false)
    private DiscussionQuestion question;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false, updatable = false)
    private User author;

    @NotBlank
    @Size(min = 10, max = 5000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean accepted;

    @Column(name = "vote_score", nullable = false)
    private int voteScore;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private int version;

    protected DiscussionAnswer() {
    }

    public DiscussionAnswer(
            DiscussionQuestion question,
            User author,
            String body) {
        this.question = question;
        this.author = author;
        this.body = body.trim();
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

    public void updateBody(String body) {
        this.body = body.trim();
    }

    public void softDelete() {
        accepted = false;
        deleted = true;
    }

    public void applyVoteChange(int scoreChange) {
        voteScore = Math.addExact(voteScore, scoreChange);
    }

    void markAccepted() {
        accepted = true;
    }

    void unmarkAccepted() {
        accepted = false;
    }

    public boolean isOwnedBy(UUID userId) {
        return author.getId().equals(userId);
    }

    public UUID getId() {
        return id;
    }

    public DiscussionQuestion getQuestion() {
        return question;
    }

    public User getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public int getVoteScore() {
        return voteScore;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getVersion() {
        return version;
    }
}
