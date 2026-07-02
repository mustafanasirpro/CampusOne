package com.campusone.discussion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class DiscussionQuestionVoteId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected DiscussionQuestionVoteId() {
    }

    public DiscussionQuestionVoteId(UUID questionId, UUID userId) {
        this.questionId = questionId;
        this.userId = userId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DiscussionQuestionVoteId that)) {
            return false;
        }
        return Objects.equals(questionId, that.questionId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionId, userId);
    }
}
