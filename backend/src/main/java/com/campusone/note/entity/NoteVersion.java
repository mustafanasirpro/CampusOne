package com.campusone.note.entity;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "note_versions",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_note_versions_revision",
                    columnNames = {"note_id", "revision_number"}),
            @UniqueConstraint(
                    name = "uk_note_versions_file_asset",
                    columnNames = "file_asset_id")
        })
public class NoteVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_id", nullable = false, updatable = false)
    private Note note;

    @Min(1)
    @Column(name = "revision_number", nullable = false, updatable = false)
    private int revisionNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_asset_id", nullable = false, unique = true, updatable = false)
    private FileAsset fileAsset;

    @NotBlank
    @Size(min = 5, max = 160)
    @Column(nullable = false, length = 160, updatable = false)
    private String title;

    @NotBlank
    @Size(min = 10, max = 2000)
    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String description;

    @NotBlank
    @Size(min = 2, max = 120)
    @Column(name = "teacher_name", nullable = false, length = 120, updatable = false)
    private String teacherName;

    @Min(1)
    @Max(8)
    @Column(nullable = false, updatable = false)
    private short semester;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 16, updatable = false)
    private NoteFileType fileType;

    @Size(max = 500)
    @Column(name = "change_summary", length = 500, updatable = false)
    private String changeSummary;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NoteVersion() {
    }

    public NoteVersion(
            Note note,
            int revisionNumber,
            FileAsset fileAsset,
            String title,
            String description,
            String teacherName,
            int semester,
            NoteFileType fileType,
            String changeSummary,
            User createdBy) {
        this.note = note;
        this.revisionNumber = revisionNumber;
        this.fileAsset = fileAsset;
        this.title = title.trim();
        this.description = description.trim();
        this.teacherName = teacherName.trim();
        this.semester = (short) semester;
        this.fileType = fileType;
        this.changeSummary = normalizeOptional(changeSummary);
        this.createdBy = createdBy;
    }

    public static NoteVersion initial(Note note) {
        return new NoteVersion(
                note,
                1,
                note.getFileAsset(),
                note.getTitle(),
                note.getDescription(),
                note.getTeacherName(),
                note.getSemester(),
                note.getFileType(),
                "Initial version",
                note.getUploader());
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public Note getNote() {
        return note;
    }

    public int getRevisionNumber() {
        return revisionNumber;
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

    public String getChangeSummary() {
        return changeSummary;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
