package com.campusone.discussion.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "discussion_questions")
public class DiscussionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false, updatable = false)
    private User author;

    @NotBlank
    @Size(min = 5, max = 180)
    @Column(nullable = false, length = 180)
    private String title;

    @NotBlank
    @Size(min = 10, max = 5000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DiscussionCategory category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiscussionQuestionStatus status = DiscussionQuestionStatus.OPEN;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_answer_id")
    private DiscussionAnswer acceptedAnswer;

    @PositiveOrZero
    @Column(name = "answer_count", nullable = false)
    private int answerCount;

    @Column(name = "vote_score", nullable = false)
    private int voteScore;

    @PositiveOrZero
    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private int version;

    protected DiscussionQuestion() {
    }

    public DiscussionQuestion(
            User author,
            String title,
            String body,
            DiscussionCategory category) {
        this.author = author;
        this.title = title.trim();
        this.body = body.trim();
        this.category = category;
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

    public void update(
            String title,
            String body,
            DiscussionCategory category,
            DiscussionQuestionStatus status) {
        if (title != null) {
            this.title = title.trim();
        }
        if (body != null) {
            this.body = body.trim();
        }
        if (category != null) {
            this.category = category;
        }
        if (status != null) {
            if (status != DiscussionQuestionStatus.RESOLVED
                    && acceptedAnswer != null) {
                acceptedAnswer.unmarkAccepted();
                acceptedAnswer = null;
            }
            this.status = status;
        }
    }

    public void softDelete() {
        deleted = true;
    }

    public void recordView() {
        viewCount = Math.addExact(viewCount, 1);
    }

    public void incrementAnswerCount() {
        answerCount = Math.addExact(answerCount, 1);
    }

    public void decrementAnswerCount() {
        answerCount = Math.max(0, answerCount - 1);
    }

    public void applyVoteChange(int scoreChange) {
        voteScore = Math.addExact(voteScore, scoreChange);
    }

    public void acceptAnswer(DiscussionAnswer answer) {
        if (acceptedAnswer != null && acceptedAnswer != answer) {
            acceptedAnswer.unmarkAccepted();
        }
        acceptedAnswer = answer;
        answer.markAccepted();
        status = DiscussionQuestionStatus.RESOLVED;
    }

    public void unacceptAnswer() {
        if (acceptedAnswer != null) {
            acceptedAnswer.unmarkAccepted();
            acceptedAnswer = null;
        }
        status = DiscussionQuestionStatus.OPEN;
    }

    public boolean isOwnedBy(UUID userId) {
        return author.getId().equals(userId);
    }

    public UUID getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public DiscussionCategory getCategory() {
        return category;
    }

    public DiscussionQuestionStatus getStatus() {
        return status;
    }

    public DiscussionAnswer getAcceptedAnswer() {
        return acceptedAnswer;
    }

    public UUID getAcceptedAnswerId() {
        return acceptedAnswer == null ? null : acceptedAnswer.getId();
    }

    public int getAnswerCount() {
        return answerCount;
    }

    public int getVoteScore() {
        return voteScore;
    }

    public int getViewCount() {
        return viewCount;
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
