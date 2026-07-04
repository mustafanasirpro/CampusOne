package com.campusone.ai.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_chat_messages")
public class AiChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private AiChatSession session;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiMessageRole role;

    @NotBlank
    @Size(max = 10000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @PositiveOrZero
    @Column(name = "token_estimate", nullable = false)
    private int tokenEstimate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiChatMessage() {
    }

    public AiChatMessage(
            AiChatSession session,
            User user,
            AiMessageRole role,
            String content,
            int tokenEstimate) {
        this.session = session;
        this.user = user;
        this.role = role;
        this.content = content.trim();
        this.tokenEstimate = tokenEstimate;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public AiChatSession getSession() {
        return session;
    }

    public User getUser() {
        return user;
    }

    public AiMessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public int getTokenEstimate() {
        return tokenEstimate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
