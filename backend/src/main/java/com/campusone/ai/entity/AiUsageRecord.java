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
@Table(name = "ai_usage_records")
public class AiUsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AiUsageFeature feature;

    @PositiveOrZero
    @Column(name = "input_token_estimate", nullable = false)
    private int inputTokenEstimate;

    @PositiveOrZero
    @Column(name = "output_token_estimate", nullable = false)
    private int outputTokenEstimate;

    @NotBlank
    @Size(min = 2, max = 60)
    @Column(nullable = false, length = 60)
    private String provider;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiUsageRecord() {
    }

    public AiUsageRecord(
            User user,
            AiUsageFeature feature,
            int inputTokenEstimate,
            int outputTokenEstimate,
            String provider) {
        this.user = user;
        this.feature = feature;
        this.inputTokenEstimate = inputTokenEstimate;
        this.outputTokenEstimate = outputTokenEstimate;
        this.provider = provider.trim();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public AiUsageFeature getFeature() {
        return feature;
    }

    public int getInputTokenEstimate() {
        return inputTokenEstimate;
    }

    public int getOutputTokenEstimate() {
        return outputTokenEstimate;
    }

    public String getProvider() {
        return provider;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
