package com.campusone.internship.entity;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "internships")
public class Internship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poster_user_id", nullable = false, updatable = false)
    private User poster;

    @NotBlank
    @Size(min = 5, max = 180)
    @Column(nullable = false, length = 180)
    private String title;

    @NotBlank
    @Size(min = 2, max = 160)
    @Column(name = "company_name", nullable = false, length = 160)
    private String companyName;

    @NotBlank
    @Size(min = 20, max = 5000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Size(min = 2, max = 255)
    @Column(nullable = false, length = 255)
    private String location;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "internship_type", nullable = false, length = 30)
    private InternshipType internshipType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 30)
    private WorkMode workMode;

    @Column(nullable = false)
    private boolean paid;

    @DecimalMin("0.00")
    @Column(name = "stipend_amount", precision = 12, scale = 2)
    private BigDecimal stipendAmount;

    @Size(min = 3, max = 10)
    @Column(length = 10)
    private String currency;

    @NotBlank
    @Size(max = 1000)
    @Column(name = "apply_url", nullable = false, length = 1000)
    private String applyUrl;

    @NotNull
    @Column(nullable = false)
    private Instant deadline;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InternshipStatus status = InternshipStatus.OPEN;

    @Column(nullable = false)
    private boolean deleted;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Internship() {
    }

    public Internship(
            User poster,
            String title,
            String companyName,
            String description,
            String location,
            InternshipType internshipType,
            WorkMode workMode,
            boolean paid,
            BigDecimal stipendAmount,
            String currency,
            String applyUrl,
            Instant deadline) {
        this.poster = poster;
        this.title = title.trim();
        this.companyName = companyName.trim();
        this.description = description.trim();
        this.location = location.trim();
        this.internshipType = internshipType;
        this.workMode = workMode;
        this.paid = paid;
        this.stipendAmount = stipendAmount;
        this.currency = normalizeCurrency(currency);
        this.applyUrl = applyUrl.trim();
        this.deadline = deadline;
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
            String companyName,
            String description,
            String location,
            InternshipType internshipType,
            WorkMode workMode,
            Boolean paid,
            BigDecimal stipendAmount,
            String currency,
            String applyUrl,
            Instant deadline,
            InternshipStatus status) {
        if (title != null) {
            this.title = title.trim();
        }
        if (companyName != null) {
            this.companyName = companyName.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
        if (location != null) {
            this.location = location.trim();
        }
        if (internshipType != null) {
            this.internshipType = internshipType;
        }
        if (workMode != null) {
            this.workMode = workMode;
        }
        if (paid != null) {
            this.paid = paid;
        }
        if (stipendAmount != null) {
            this.stipendAmount = stipendAmount;
        }
        if (currency != null) {
            this.currency = normalizeCurrency(currency);
        }
        if (applyUrl != null) {
            this.applyUrl = applyUrl.trim();
        }
        if (deadline != null) {
            this.deadline = deadline;
        }
        if (status != null) {
            this.status = status;
        }
    }

    public void softDelete() {
        deleted = true;
    }

    public void submitForReview() {
        status = InternshipStatus.PENDING_REVIEW;
    }

    public void approve() {
        status = InternshipStatus.OPEN;
    }

    public void reject() {
        status = InternshipStatus.REJECTED;
    }

    public boolean isOwnedBy(UUID userId) {
        return poster.getId().equals(userId);
    }

    private static String normalizeCurrency(String value) {
        return value == null
                ? null
                : value.trim().toUpperCase(Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public User getPoster() {
        return poster;
    }

    public String getTitle() {
        return title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public InternshipType getInternshipType() {
        return internshipType;
    }

    public WorkMode getWorkMode() {
        return workMode;
    }

    public boolean isPaid() {
        return paid;
    }

    public BigDecimal getStipendAmount() {
        return stipendAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getApplyUrl() {
        return applyUrl;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public InternshipStatus getStatus() {
        return status;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
