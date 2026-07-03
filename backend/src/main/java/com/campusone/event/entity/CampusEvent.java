package com.campusone.event.entity;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class CampusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_user_id", nullable = false, updatable = false)
    private User organizer;

    @NotBlank
    @Size(min = 5, max = 160)
    @Column(nullable = false, length = 160)
    private String title;

    @NotBlank
    @Size(min = 10, max = 5000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Size(min = 2, max = 255)
    @Column(nullable = false, length = 255)
    private String location;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Positive
    @Column(nullable = false)
    private int capacity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventVisibility visibility;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventStatus status = EventStatus.UPCOMING;

    @PositiveOrZero
    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private int version;

    protected CampusEvent() {
    }

    public CampusEvent(
            User organizer,
            String title,
            String description,
            String location,
            Instant startTime,
            Instant endTime,
            int capacity,
            EventVisibility visibility) {
        this.organizer = organizer;
        this.title = title.trim();
        this.description = description.trim();
        this.location = location.trim();
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.visibility = visibility;
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
            String description,
            String location,
            Instant startTime,
            Instant endTime,
            Integer capacity,
            EventVisibility visibility,
            EventStatus status) {
        if (title != null) {
            this.title = title.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
        if (location != null) {
            this.location = location.trim();
        }
        if (startTime != null) {
            this.startTime = startTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }
        if (capacity != null) {
            this.capacity = capacity;
        }
        if (visibility != null) {
            this.visibility = visibility;
        }
        if (status != null) {
            this.status = status;
        }
    }

    public void incrementParticipantCount() {
        participantCount = Math.addExact(participantCount, 1);
    }

    public void decrementParticipantCount() {
        participantCount = Math.max(0, participantCount - 1);
    }

    public void softDelete() {
        deleted = true;
    }

    public boolean isOwnedBy(UUID userId) {
        return organizer.getId().equals(userId);
    }

    public boolean isAtCapacity() {
        return participantCount >= capacity;
    }

    public UUID getId() {
        return id;
    }

    public User getOrganizer() {
        return organizer;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public EventVisibility getVisibility() {
        return visibility;
    }

    public EventStatus getStatus() {
        return status;
    }

    public int getParticipantCount() {
        return participantCount;
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
