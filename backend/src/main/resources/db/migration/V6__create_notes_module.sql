CREATE TABLE file_assets (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    storage_provider VARCHAR(20) NOT NULL,
    bucket_name VARCHAR(100) NOT NULL,
    object_key VARCHAR(1024) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(127) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 CHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_file_assets_storage_object
        UNIQUE (storage_provider, bucket_name, object_key),
    CONSTRAINT fk_file_assets_owner
        FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_file_assets_storage_provider CHECK (
        storage_provider IN ('LOCAL', 'MINIO', 'S3', 'S3_COMPATIBLE')
    ),
    CONSTRAINT chk_file_assets_bucket_name
        CHECK (CHAR_LENGTH(BTRIM(bucket_name)) BETWEEN 3 AND 100),
    CONSTRAINT chk_file_assets_object_key
        CHECK (CHAR_LENGTH(BTRIM(object_key)) BETWEEN 1 AND 1024),
    CONSTRAINT chk_file_assets_original_filename
        CHECK (CHAR_LENGTH(BTRIM(original_filename)) BETWEEN 1 AND 255),
    CONSTRAINT chk_file_assets_mime_type
        CHECK (
            CHAR_LENGTH(BTRIM(mime_type)) BETWEEN 3 AND 127
            AND POSITION('/' IN mime_type) > 1
        ),
    CONSTRAINT chk_file_assets_size
        CHECK (size_bytes > 0),
    CONSTRAINT chk_file_assets_checksum CHECK (
        checksum_sha256 IS NULL
        OR checksum_sha256 ~ '^[0-9a-f]{64}$'
    ),
    CONSTRAINT chk_file_assets_status CHECK (
        status IN (
            'PENDING',
            'QUARANTINED',
            'READY',
            'REJECTED',
            'DELETED'
        )
    ),
    CONSTRAINT chk_file_assets_expiry CHECK (
        expires_at IS NULL
        OR expires_at > created_at
    ),
    CONSTRAINT chk_file_assets_timestamps
        CHECK (updated_at >= created_at),
    CONSTRAINT chk_file_assets_version
        CHECK (version >= 0)
);

CREATE INDEX idx_file_assets_owner_created_at
    ON file_assets (owner_id, created_at DESC);
CREATE INDEX idx_file_assets_status_created_at
    ON file_assets (status, created_at)
    WHERE status IN ('PENDING', 'QUARANTINED');
CREATE INDEX idx_file_assets_expired
    ON file_assets (expires_at)
    WHERE expires_at IS NOT NULL
        AND status IN ('PENDING', 'QUARANTINED');

CREATE TABLE tags (
    id UUID PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    normalized_name VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_tags_normalized_name
        UNIQUE (normalized_name),
    CONSTRAINT chk_tags_name
        CHECK (CHAR_LENGTH(BTRIM(name)) BETWEEN 2 AND 40),
    CONSTRAINT chk_tags_normalized_name CHECK (
        CHAR_LENGTH(normalized_name) BETWEEN 2 AND 40
        AND normalized_name = LOWER(BTRIM(normalized_name))
    ),
    CONSTRAINT chk_tags_timestamps
        CHECK (updated_at >= created_at),
    CONSTRAINT chk_tags_version
        CHECK (version >= 0)
);

CREATE TABLE notes (
    id UUID PRIMARY KEY,
    uploader_id UUID NOT NULL,
    course_id UUID NOT NULL,
    file_asset_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    teacher_name VARCHAR(120) NOT NULL,
    semester SMALLINT NOT NULL,
    file_type VARCHAR(16) NOT NULL,
    visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    moderation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    moderation_reason VARCHAR(500),
    moderated_by UUID,
    moderated_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    rating_count BIGINT NOT NULL DEFAULT 0,
    rating_sum BIGINT NOT NULL DEFAULT 0,
    average_rating NUMERIC(3, 2) GENERATED ALWAYS AS (
        CASE
            WHEN rating_count = 0 THEN 0.00
            ELSE ROUND(rating_sum::NUMERIC / rating_count, 2)
        END
    ) STORED,
    download_count BIGINT NOT NULL DEFAULT 0,
    content_version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_notes_file_asset
        UNIQUE (file_asset_id),
    CONSTRAINT fk_notes_uploader
        FOREIGN KEY (uploader_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_notes_course
        FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_notes_file_asset
        FOREIGN KEY (file_asset_id)
        REFERENCES file_assets (id) ON DELETE RESTRICT,
    CONSTRAINT fk_notes_moderator
        FOREIGN KEY (moderated_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_notes_title
        CHECK (CHAR_LENGTH(BTRIM(title)) BETWEEN 5 AND 160),
    CONSTRAINT chk_notes_description
        CHECK (CHAR_LENGTH(BTRIM(description)) BETWEEN 10 AND 2000),
    CONSTRAINT chk_notes_teacher_name
        CHECK (CHAR_LENGTH(BTRIM(teacher_name)) BETWEEN 2 AND 120),
    CONSTRAINT chk_notes_semester
        CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT chk_notes_file_type CHECK (
        file_type IN (
            'PDF',
            'PPT',
            'PPTX',
            'DOC',
            'DOCX',
            'IMAGE',
            'OTHER'
        )
    ),
    CONSTRAINT chk_notes_visibility CHECK (
        visibility IN ('PUBLIC', 'CAMPUS', 'PRIVATE')
    ),
    CONSTRAINT chk_notes_moderation_status CHECK (
        moderation_status IN (
            'PENDING',
            'APPROVED',
            'REJECTED',
            'HIDDEN'
        )
    ),
    CONSTRAINT chk_notes_rejection_reason CHECK (
        moderation_status <> 'REJECTED'
        OR CHAR_LENGTH(BTRIM(moderation_reason)) BETWEEN 1 AND 500
    ),
    CONSTRAINT chk_notes_moderation_timestamp CHECK (
        moderation_status = 'PENDING'
        OR moderated_at IS NOT NULL
    ),
    CONSTRAINT chk_notes_publication_timestamp CHECK (
        moderation_status <> 'APPROVED'
        OR published_at IS NOT NULL
    ),
    CONSTRAINT chk_notes_rating_aggregates CHECK (
        (rating_count = 0 AND rating_sum = 0)
        OR (
            rating_count > 0
            AND rating_sum BETWEEN rating_count AND rating_count * 5
        )
    ),
    CONSTRAINT chk_notes_download_count
        CHECK (download_count >= 0),
    CONSTRAINT chk_notes_content_version
        CHECK (content_version >= 1),
    CONSTRAINT chk_notes_timestamps CHECK (
        updated_at >= created_at
        AND (moderated_at IS NULL OR moderated_at >= created_at)
        AND (published_at IS NULL OR published_at >= created_at)
        AND (deleted_at IS NULL OR deleted_at >= created_at)
    ),
    CONSTRAINT chk_notes_version
        CHECK (version >= 0)
);

CREATE INDEX idx_notes_uploader_created_at
    ON notes (uploader_id, created_at DESC, id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_notes_course_created_at
    ON notes (course_id, created_at DESC, id)
    WHERE deleted_at IS NULL
        AND moderation_status = 'APPROVED';
CREATE INDEX idx_notes_public_newest
    ON notes (created_at DESC, id)
    WHERE deleted_at IS NULL
        AND moderation_status = 'APPROVED'
        AND visibility = 'PUBLIC';
CREATE INDEX idx_notes_public_rating
    ON notes (average_rating DESC, rating_count DESC, id)
    WHERE deleted_at IS NULL
        AND moderation_status = 'APPROVED'
        AND visibility = 'PUBLIC';
CREATE INDEX idx_notes_moderation_queue
    ON notes (created_at, id)
    WHERE deleted_at IS NULL
        AND moderation_status = 'PENDING';
CREATE INDEX idx_notes_moderated_by
    ON notes (moderated_by)
    WHERE moderated_by IS NOT NULL;

CREATE TABLE note_versions (
    id UUID PRIMARY KEY,
    note_id UUID NOT NULL,
    revision_number INTEGER NOT NULL,
    file_asset_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    teacher_name VARCHAR(120) NOT NULL,
    semester SMALLINT NOT NULL,
    file_type VARCHAR(16) NOT NULL,
    change_summary VARCHAR(500),
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_note_versions_revision
        UNIQUE (note_id, revision_number),
    CONSTRAINT uk_note_versions_file_asset
        UNIQUE (file_asset_id),
    CONSTRAINT fk_note_versions_note
        FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_versions_file_asset
        FOREIGN KEY (file_asset_id)
        REFERENCES file_assets (id) ON DELETE RESTRICT,
    CONSTRAINT fk_note_versions_creator
        FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_note_versions_revision
        CHECK (revision_number >= 1),
    CONSTRAINT chk_note_versions_title
        CHECK (CHAR_LENGTH(BTRIM(title)) BETWEEN 5 AND 160),
    CONSTRAINT chk_note_versions_description
        CHECK (CHAR_LENGTH(BTRIM(description)) BETWEEN 10 AND 2000),
    CONSTRAINT chk_note_versions_teacher_name
        CHECK (CHAR_LENGTH(BTRIM(teacher_name)) BETWEEN 2 AND 120),
    CONSTRAINT chk_note_versions_semester
        CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT chk_note_versions_file_type CHECK (
        file_type IN (
            'PDF',
            'PPT',
            'PPTX',
            'DOC',
            'DOCX',
            'IMAGE',
            'OTHER'
        )
    ),
    CONSTRAINT chk_note_versions_change_summary CHECK (
        change_summary IS NULL
        OR CHAR_LENGTH(BTRIM(change_summary)) BETWEEN 1 AND 500
    )
);

CREATE INDEX idx_note_versions_creator
    ON note_versions (created_by);

CREATE TABLE note_tags (
    note_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_note_tags
        PRIMARY KEY (note_id, tag_id),
    CONSTRAINT fk_note_tags_note
        FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE RESTRICT
);

CREATE INDEX idx_note_tags_tag_note
    ON note_tags (tag_id, note_id);

CREATE TABLE note_ratings (
    note_id UUID NOT NULL,
    user_id UUID NOT NULL,
    rating SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_note_ratings
        PRIMARY KEY (note_id, user_id),
    CONSTRAINT fk_note_ratings_note
        FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_ratings_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_note_ratings_value
        CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_note_ratings_timestamps
        CHECK (updated_at >= created_at),
    CONSTRAINT chk_note_ratings_version
        CHECK (version >= 0)
);

CREATE INDEX idx_note_ratings_user
    ON note_ratings (user_id);

CREATE TABLE note_bookmarks (
    note_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_note_bookmarks
        PRIMARY KEY (note_id, user_id),
    CONSTRAINT fk_note_bookmarks_note
        FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_bookmarks_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_note_bookmarks_user_created_at
    ON note_bookmarks (user_id, created_at DESC, note_id);

CREATE TABLE note_download_events (
    id UUID PRIMARY KEY,
    note_id UUID NOT NULL,
    file_asset_id UUID NOT NULL,
    user_id UUID,
    request_fingerprint_hash CHAR(64),
    user_agent_hash CHAR(64),
    downloaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_download_events_note
        FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_download_events_file_asset
        FOREIGN KEY (file_asset_id)
        REFERENCES file_assets (id) ON DELETE RESTRICT,
    CONSTRAINT fk_note_download_events_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_note_download_events_fingerprint CHECK (
        request_fingerprint_hash IS NULL
        OR request_fingerprint_hash ~ '^[0-9a-f]{64}$'
    ),
    CONSTRAINT chk_note_download_events_user_agent CHECK (
        user_agent_hash IS NULL
        OR user_agent_hash ~ '^[0-9a-f]{64}$'
    )
);

CREATE INDEX idx_note_download_events_note_downloaded_at
    ON note_download_events (note_id, downloaded_at DESC);
CREATE INDEX idx_note_download_events_user_downloaded_at
    ON note_download_events (user_id, downloaded_at DESC)
    WHERE user_id IS NOT NULL;
CREATE INDEX idx_note_download_events_file_asset
    ON note_download_events (file_asset_id);

CREATE TABLE note_moderation_actions (
    id UUID PRIMARY KEY,
    note_id UUID NOT NULL,
    moderator_id UUID,
    action VARCHAR(20) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_moderation_actions_note
        FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_moderation_actions_moderator
        FOREIGN KEY (moderator_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_note_moderation_actions_action CHECK (
        action IN (
            'SUBMITTED',
            'APPROVED',
            'REJECTED',
            'HIDDEN',
            'RESTORED'
        )
    ),
    CONSTRAINT chk_note_moderation_actions_previous_status CHECK (
        previous_status IS NULL
        OR previous_status IN (
            'PENDING',
            'APPROVED',
            'REJECTED',
            'HIDDEN'
        )
    ),
    CONSTRAINT chk_note_moderation_actions_new_status CHECK (
        new_status IN (
            'PENDING',
            'APPROVED',
            'REJECTED',
            'HIDDEN'
        )
    ),
    CONSTRAINT chk_note_moderation_actions_reason CHECK (
        reason IS NULL
        OR CHAR_LENGTH(BTRIM(reason)) BETWEEN 1 AND 500
    )
);

CREATE INDEX idx_note_moderation_actions_note_created_at
    ON note_moderation_actions (note_id, created_at DESC);
CREATE INDEX idx_note_moderation_actions_moderator
    ON note_moderation_actions (moderator_id)
    WHERE moderator_id IS NOT NULL;
