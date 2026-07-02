package com.campusone.note.entity;

import com.campusone.academic.entity.Course;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false, updatable = false)
    private User uploader;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_asset_id", nullable = false, unique = true, updatable = false)
    private FileAsset fileAsset;

    @NotBlank
    @Size(min = 5, max = 160)
    @Column(nullable = false, length = 160)
    private String title;

    @NotBlank
    @Size(min = 10, max = 2000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Size(min = 2, max = 120)
    @Column(name = "teacher_name", nullable = false, length = 120)
    private String teacherName;

    @Min(1)
    @Max(8)
    @Column(nullable = false)
    private short semester;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 16)
    private NoteFileType fileType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NoteVisibility visibility = NoteVisibility.PUBLIC;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private NoteModerationStatus moderationStatus = NoteModerationStatus.PENDING;

    @Size(max = 500)
    @Column(name = "moderation_reason", length = 500)
    private String moderationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by")
    private User moderatedBy;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @PositiveOrZero
    @Column(name = "rating_count", nullable = false)
    private long ratingCount;

    @PositiveOrZero
    @Column(name = "rating_sum", nullable = false)
    private long ratingSum;

    @Column(
            name = "average_rating",
            nullable = false,
            precision = 3,
            scale = 2,
            insertable = false,
            updatable = false)
    private BigDecimal averageRating;

    @PositiveOrZero
    @Column(name = "download_count", nullable = false)
    private long downloadCount;

    @Min(1)
    @Column(name = "content_version", nullable = false)
    private int contentVersion = 1;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "note_tags",
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @BatchSize(size = 50)
    private Set<Tag> tags = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Note() {
    }

    public Note(
            User uploader,
            Course course,
            FileAsset fileAsset,
            String title,
            String description,
            String teacherName,
            int semester,
            NoteFileType fileType,
            NoteVisibility visibility) {
        this.uploader = uploader;
        this.course = course;
        this.fileAsset = fileAsset;
        this.title = title.trim();
        this.description = description.trim();
        this.teacherName = teacherName.trim();
        this.semester = (short) semester;
        this.fileType = fileType;
        this.visibility = visibility == null ? NoteVisibility.PUBLIC : visibility;
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

    public void updateMetadata(
            Course course,
            String title,
            String description,
            String teacherName,
            Integer semester,
            NoteFileType fileType,
            NoteVisibility visibility) {
        if (course != null) {
            this.course = course;
        }
        if (title != null) {
            this.title = title.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
        if (teacherName != null) {
            this.teacherName = teacherName.trim();
        }
        if (semester != null) {
            this.semester = semester.shortValue();
        }
        if (fileType != null) {
            this.fileType = fileType;
        }
        if (visibility != null) {
            this.visibility = visibility;
        }
    }

    public NoteModerationStatus resubmitForReview() {
        NoteModerationStatus previousStatus = moderationStatus;
        if (moderationStatus == NoteModerationStatus.APPROVED
                || moderationStatus == NoteModerationStatus.REJECTED) {
            moderationStatus = NoteModerationStatus.PENDING;
            moderationReason = null;
            moderatedBy = null;
            moderatedAt = null;
            publishedAt = null;
        }
        return previousStatus;
    }

    public void approve(User moderator, Instant approvedAt) {
        moderationStatus = NoteModerationStatus.APPROVED;
        moderationReason = null;
        moderatedBy = moderator;
        moderatedAt = approvedAt;
        publishedAt = approvedAt;
    }

    public void reject(User moderator, String reason, Instant rejectedAt) {
        moderationStatus = NoteModerationStatus.REJECTED;
        moderationReason = reason.trim();
        moderatedBy = moderator;
        moderatedAt = rejectedAt;
        publishedAt = null;
    }

    public void hide(User moderator, String reason, Instant hiddenAt) {
        moderationStatus = NoteModerationStatus.HIDDEN;
        moderationReason = reason == null || reason.isBlank()
                ? null
                : reason.trim();
        moderatedBy = moderator;
        moderatedAt = hiddenAt;
    }

    public void replaceTags(Collection<Tag> replacementTags) {
        tags.clear();
        tags.addAll(replacementTags);
    }

    public void softDelete(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void applyRating(Integer previousRating, int newRating) {
        if (previousRating == null) {
            ratingCount++;
            ratingSum += newRating;
        } else {
            ratingSum += newRating - previousRating;
        }
    }

    public void recordDownload() {
        downloadCount++;
    }

    public boolean isPubliclyVisible() {
        return deletedAt == null
                && moderationStatus == NoteModerationStatus.APPROVED
                && visibility == NoteVisibility.PUBLIC;
    }

    public boolean isOwnedBy(UUID userId) {
        return uploader.getId().equals(userId);
    }

    public BigDecimal calculateAverageRating() {
        if (ratingCount == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(ratingSum)
                .divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public User getUploader() {
        return uploader;
    }

    public Course getCourse() {
        return course;
    }

    public FileAsset getFileAsset() {
        return fileAsset;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public int getSemester() {
        return semester;
    }

    public NoteFileType getFileType() {
        return fileType;
    }

    public NoteVisibility getVisibility() {
        return visibility;
    }

    public NoteModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public String getModerationReason() {
        return moderationReason;
    }

    public User getModeratedBy() {
        return moderatedBy;
    }

    public Instant getModeratedAt() {
        return moderatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public long getRatingCount() {
        return ratingCount;
    }

    public long getRatingSum() {
        return ratingSum;
    }

    public BigDecimal getAverageRating() {
        return averageRating == null ? calculateAverageRating() : averageRating;
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public int getContentVersion() {
        return contentVersion;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public long getVersion() {
        return version;
    }
}
