package com.campusone.discussion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class DiscussionAnswerVoteId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "answer_id", nullable = false)
    private UUID answerId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected DiscussionAnswerVoteId() {
    }

    public DiscussionAnswerVoteId(UUID answerId, UUID userId) {
        this.answerId = answerId;
        this.userId = userId;
    }

    public UUID getAnswerId() {
        return answerId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DiscussionAnswerVoteId that)) {
            return false;
        }
        return Objects.equals(answerId, that.answerId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(answerId, userId);
    }
}
