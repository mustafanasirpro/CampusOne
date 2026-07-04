package com.campusone.ai.entity;

import com.campusone.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_generated_items")
public class AiGeneratedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AiChatSession session;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 40)
    private AiGeneratedItemType itemType;

    @NotBlank
    @Size(min = 3, max = 160)
    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "generated_content",
            nullable = false,
            columnDefinition = "jsonb")
    private JsonNode generatedContent;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiGeneratedItem() {
    }

    public AiGeneratedItem(
            User user,
            AiChatSession session,
            AiGeneratedItemType itemType,
            String title,
            String sourceText,
            JsonNode generatedContent) {
        this.user = user;
        this.session = session;
        this.itemType = itemType;
        this.title = title.trim();
        this.sourceText = normalize(sourceText);
        this.generatedContent = generatedContent.deepCopy();
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

    public void softDelete() {
        deleted = true;
    }

    public boolean isOwnedBy(UUID userId) {
        return user.getId().equals(userId);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public AiChatSession getSession() {
        return session;
    }

    public AiGeneratedItemType getItemType() {
        return itemType;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceText() {
        return sourceText;
    }

    public JsonNode getGeneratedContent() {
        return generatedContent.deepCopy();
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
}
