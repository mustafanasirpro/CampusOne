package com.campusone.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ThemePreference theme = ThemePreference.SYSTEM;

    @NotBlank
    @Size(min = 2, max = 10)
    @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$")
    @Column(nullable = false, length = 10)
    private String language = "en";

    @Column(name = "compact_mode", nullable = false)
    private boolean compactMode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected UserPreference() {
    }

    public UserPreference(User user) {
        this.user = user;
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
            ThemePreference theme,
            String language,
            Boolean compactMode) {
        if (theme != null) {
            this.theme = theme;
        }
        if (language != null) {
            this.language = normalizeLanguage(language);
        }
        if (compactMode != null) {
            this.compactMode = compactMode;
        }
    }

    public static String normalizeLanguage(String language) {
        String trimmed = language.trim();
        String[] parts = trimmed.split("-", 2);
        if (parts.length == 1) {
            return parts[0].toLowerCase(Locale.ROOT);
        }
        return parts[0].toLowerCase(Locale.ROOT)
                + "-"
                + parts[1].toUpperCase(Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ThemePreference getTheme() {
        return theme;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isCompactMode() {
        return compactMode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
