package com.campusone.note.entity;

import com.campusone.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "note_download_events")
public class NoteDownloadEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_id", nullable = false, updatable = false)
    private Note note;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_asset_id", nullable = false, updatable = false)
    private FileAsset fileAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    private User user;

    @Pattern(regexp = "^[0-9a-f]{64}$")
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_fingerprint_hash", length = 64, updatable = false)
    private String requestFingerprintHash;

    @Pattern(regexp = "^[0-9a-f]{64}$")
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_agent_hash", length = 64, updatable = false)
    private String userAgentHash;

    @Column(name = "downloaded_at", nullable = false, updatable = false)
    private Instant downloadedAt;

    protected NoteDownloadEvent() {
    }

    public NoteDownloadEvent(
            Note note,
            User user,
            String requestFingerprintHash,
            String userAgentHash) {
        this.note = note;
        this.fileAsset = note.getFileAsset();
        this.user = user;
        this.requestFingerprintHash = requestFingerprintHash;
        this.userAgentHash = userAgentHash;
    }

    @PrePersist
    void onCreate() {
        downloadedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Note getNote() {
        return note;
    }

    public FileAsset getFileAsset() {
        return fileAsset;
    }

    public User getUser() {
        return user;
    }

    public String getRequestFingerprintHash() {
        return requestFingerprintHash;
    }

    public String getUserAgentHash() {
        return userAgentHash;
    }

    public Instant getDownloadedAt() {
        return downloadedAt;
    }
}
