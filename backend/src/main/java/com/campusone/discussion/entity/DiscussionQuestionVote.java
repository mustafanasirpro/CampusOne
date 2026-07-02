package com.campusone.discussion.entity;

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
import java.time.Instant;

@Entity
@Table(name = "discussion_question_votes")
public class DiscussionQuestionVote {

    @EmbeddedId
    private DiscussionQuestionVoteId id;

    @MapsId("questionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private DiscussionQuestion question;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "vote_value", nullable = false)
    private short voteValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DiscussionQuestionVote() {
    }

    public DiscussionQuestionVote(
            DiscussionQuestion question,
            User user,
            int voteValue) {
        this.id = new DiscussionQuestionVoteId(
                question.getId(),
                user.getId());
        this.question = question;
        this.user = user;
        this.voteValue = (short) voteValue;
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

    public void updateVote(int voteValue) {
        this.voteValue = (short) voteValue;
    }

    public DiscussionQuestionVoteId getId() {
        return id;
    }

    public int getVoteValue() {
        return voteValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
