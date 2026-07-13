CREATE TABLE lost_found_items (
    id UUID PRIMARY KEY,
    reporter_user_id UUID NOT NULL,
    university_id UUID NOT NULL,
    item_type VARCHAR(16) NOT NULL,
    category VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    location_text VARCHAR(255) NOT NULL,
    item_date DATE NOT NULL,
    brand VARCHAR(80),
    color VARCHAR(60),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    moderation_reason VARCHAR(500),
    moderated_by_user_id UUID,
    moderated_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_lost_found_items_reporter
        FOREIGN KEY (reporter_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lost_found_items_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lost_found_items_moderator
        FOREIGN KEY (moderated_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_lost_found_items_type
        CHECK (item_type IN ('LOST', 'FOUND')),
    CONSTRAINT chk_lost_found_items_category CHECK (
        category IN (
            'ID_CARD',
            'WALLET_PURSE',
            'ELECTRONICS',
            'KEYS',
            'BAG',
            'BOOKS_STATIONERY',
            'CLOTHING_ACCESSORIES',
            'DOCUMENTS',
            'JEWELRY',
            'BOTTLE_UMBRELLA',
            'OTHER'
        )
    ),
    CONSTRAINT chk_lost_found_items_title
        CHECK (CHAR_LENGTH(BTRIM(title)) BETWEEN 5 AND 160),
    CONSTRAINT chk_lost_found_items_description
        CHECK (CHAR_LENGTH(BTRIM(description)) BETWEEN 10 AND 2000),
    CONSTRAINT chk_lost_found_items_location
        CHECK (CHAR_LENGTH(BTRIM(location_text)) BETWEEN 2 AND 255),
    CONSTRAINT chk_lost_found_items_brand
        CHECK (brand IS NULL OR CHAR_LENGTH(BTRIM(brand)) BETWEEN 1 AND 80),
    CONSTRAINT chk_lost_found_items_color
        CHECK (color IS NULL OR CHAR_LENGTH(BTRIM(color)) BETWEEN 1 AND 60),
    CONSTRAINT chk_lost_found_items_status CHECK (
        status IN (
            'PENDING_REVIEW',
            'PUBLISHED',
            'CLAIM_IN_PROGRESS',
            'RESOLVED',
            'CLOSED',
            'REJECTED',
            'ARCHIVED',
            'DELETED'
        )
    ),
    CONSTRAINT chk_lost_found_items_rejection_reason CHECK (
        status <> 'REJECTED'
        OR CHAR_LENGTH(BTRIM(moderation_reason)) BETWEEN 1 AND 500
    ),
    CONSTRAINT chk_lost_found_items_moderation_time CHECK (
        status = 'PENDING_REVIEW'
        OR moderated_at IS NOT NULL
        OR status IN ('DELETED', 'ARCHIVED', 'CLAIM_IN_PROGRESS', 'RESOLVED', 'CLOSED')
    ),
    CONSTRAINT chk_lost_found_items_publication_time CHECK (
        status NOT IN ('PUBLISHED', 'CLAIM_IN_PROGRESS', 'RESOLVED', 'CLOSED')
        OR published_at IS NOT NULL
    ),
    CONSTRAINT chk_lost_found_items_timestamps CHECK (
        updated_at >= created_at
        AND (moderated_at IS NULL OR moderated_at >= created_at)
        AND (published_at IS NULL OR published_at >= created_at)
        AND (expires_at IS NULL OR expires_at >= created_at)
        AND (resolved_at IS NULL OR resolved_at >= created_at)
        AND (deleted_at IS NULL OR deleted_at >= created_at)
    ),
    CONSTRAINT chk_lost_found_items_version
        CHECK (version >= 0)
);

CREATE TABLE lost_found_item_images (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL,
    storage_provider VARCHAR(20) NOT NULL,
    bucket_name VARCHAR(100) NOT NULL,
    object_key VARCHAR(1024) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(127) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    checksum_sha256 CHAR(64),
    display_order SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lost_found_item_images_item
        FOREIGN KEY (item_id) REFERENCES lost_found_items (id) ON DELETE CASCADE,
    CONSTRAINT uk_lost_found_item_images_order
        UNIQUE (item_id, display_order),
    CONSTRAINT uk_lost_found_item_images_object
        UNIQUE (storage_provider, bucket_name, object_key),
    CONSTRAINT chk_lost_found_item_images_storage_provider CHECK (
        storage_provider IN ('LOCAL', 'MINIO', 'S3', 'S3_COMPATIBLE')
    ),
    CONSTRAINT chk_lost_found_item_images_bucket
        CHECK (CHAR_LENGTH(BTRIM(bucket_name)) BETWEEN 3 AND 100),
    CONSTRAINT chk_lost_found_item_images_object_key
        CHECK (CHAR_LENGTH(BTRIM(object_key)) BETWEEN 1 AND 1024),
    CONSTRAINT chk_lost_found_item_images_filename
        CHECK (CHAR_LENGTH(BTRIM(original_filename)) BETWEEN 1 AND 255),
    CONSTRAINT chk_lost_found_item_images_mime CHECK (
        mime_type IN ('image/jpeg', 'image/png', 'image/webp')
    ),
    CONSTRAINT chk_lost_found_item_images_size
        CHECK (file_size_bytes > 0),
    CONSTRAINT chk_lost_found_item_images_checksum CHECK (
        checksum_sha256 IS NULL
        OR checksum_sha256 ~ '^[0-9a-f]{64}$'
    ),
    CONSTRAINT chk_lost_found_item_images_order
        CHECK (display_order BETWEEN 0 AND 2)
);

CREATE TABLE lost_found_claims (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL,
    claimant_user_id UUID NOT NULL,
    proof_text VARCHAR(2000) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id UUID,
    reviewer_note VARCHAR(1000),
    handover_note VARCHAR(1000),
    reporter_handover_confirmed_at TIMESTAMPTZ,
    claimant_handover_confirmed_at TIMESTAMPTZ,
    handover_completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_lost_found_claims_item
        FOREIGN KEY (item_id) REFERENCES lost_found_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_lost_found_claims_claimant
        FOREIGN KEY (claimant_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lost_found_claims_reviewer
        FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_lost_found_claims_proof
        CHECK (CHAR_LENGTH(BTRIM(proof_text)) BETWEEN 10 AND 2000),
    CONSTRAINT chk_lost_found_claims_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED')
    ),
    CONSTRAINT chk_lost_found_claims_reviewer_note
        CHECK (reviewer_note IS NULL OR CHAR_LENGTH(reviewer_note) <= 1000),
    CONSTRAINT chk_lost_found_claims_handover_note
        CHECK (handover_note IS NULL OR CHAR_LENGTH(handover_note) <= 1000),
    CONSTRAINT chk_lost_found_claims_timestamps CHECK (
        updated_at >= created_at
        AND (reviewed_at IS NULL OR reviewed_at >= created_at)
        AND (reporter_handover_confirmed_at IS NULL OR reporter_handover_confirmed_at >= created_at)
        AND (claimant_handover_confirmed_at IS NULL OR claimant_handover_confirmed_at >= created_at)
        AND (handover_completed_at IS NULL OR handover_completed_at >= created_at)
    ),
    CONSTRAINT chk_lost_found_claims_version
        CHECK (version >= 0)
);

CREATE TABLE lost_found_matches (
    id UUID PRIMARY KEY,
    lost_item_id UUID NOT NULL,
    found_item_id UUID NOT NULL,
    score SMALLINT NOT NULL,
    reasons JSONB NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'SUGGESTED',
    status_changed_by_user_id UUID,
    status_changed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_lost_found_matches_lost_item
        FOREIGN KEY (lost_item_id) REFERENCES lost_found_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_lost_found_matches_found_item
        FOREIGN KEY (found_item_id) REFERENCES lost_found_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_lost_found_matches_status_user
        FOREIGN KEY (status_changed_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT uk_lost_found_matches_pair
        UNIQUE (lost_item_id, found_item_id),
    CONSTRAINT chk_lost_found_matches_distinct_items
        CHECK (lost_item_id <> found_item_id),
    CONSTRAINT chk_lost_found_matches_score
        CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT chk_lost_found_matches_status
        CHECK (status IN ('SUGGESTED', 'CONFIRMED', 'REJECTED')),
    CONSTRAINT chk_lost_found_matches_timestamps CHECK (
        updated_at >= created_at
        AND (status_changed_at IS NULL OR status_changed_at >= created_at)
    ),
    CONSTRAINT chk_lost_found_matches_version
        CHECK (version >= 0)
);

CREATE INDEX idx_lost_found_items_public_university
    ON lost_found_items (university_id, created_at DESC, id)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;
CREATE INDEX idx_lost_found_items_pending_review
    ON lost_found_items (created_at DESC, id)
    WHERE status = 'PENDING_REVIEW' AND deleted_at IS NULL;
CREATE INDEX idx_lost_found_items_reporter_created
    ON lost_found_items (reporter_user_id, created_at DESC, id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_lost_found_items_matching
    ON lost_found_items (university_id, item_type, category, item_date DESC, id)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;
CREATE INDEX idx_lost_found_items_expiry
    ON lost_found_items (expires_at)
    WHERE expires_at IS NOT NULL
      AND status = 'PUBLISHED'
      AND deleted_at IS NULL;
CREATE INDEX idx_lost_found_claims_item_status
    ON lost_found_claims (item_id, status, created_at DESC);
CREATE INDEX idx_lost_found_claims_claimant_created
    ON lost_found_claims (claimant_user_id, created_at DESC);
CREATE UNIQUE INDEX uk_lost_found_claims_active_claimant
    ON lost_found_claims (item_id, claimant_user_id)
    WHERE status IN ('PENDING', 'APPROVED');
CREATE UNIQUE INDEX uk_lost_found_claims_approved_item
    ON lost_found_claims (item_id)
    WHERE status = 'APPROVED';
CREATE INDEX idx_lost_found_matches_lost
    ON lost_found_matches (lost_item_id, status, score DESC);
CREATE INDEX idx_lost_found_matches_found
    ON lost_found_matches (found_item_id, status, score DESC);
CREATE INDEX idx_search_lost_found_title_trgm
    ON lost_found_items USING gin (LOWER(title) gin_trgm_ops)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;
CREATE INDEX idx_search_lost_found_description_trgm
    ON lost_found_items USING gin (LOWER(description) gin_trgm_ops)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;
CREATE INDEX idx_search_lost_found_location_trgm
    ON lost_found_items USING gin (LOWER(location_text) gin_trgm_ops)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

ALTER TABLE content_reports
    DROP CONSTRAINT IF EXISTS chk_content_reports_target_type;
ALTER TABLE content_reports
    ADD CONSTRAINT chk_content_reports_target_type CHECK (
        target_type IN (
            'NOTE',
            'MARKETPLACE_LISTING',
            'DISCUSSION_QUESTION',
            'DISCUSSION_ANSWER',
            'EVENT',
            'INTERNSHIP',
            'LOST_FOUND_ITEM',
            'AI_GENERATED_ITEM',
            'USER_PROFILE',
            'SYSTEM'
        )
    );

ALTER TABLE moderation_actions
    DROP CONSTRAINT IF EXISTS chk_moderation_actions_target_type;
ALTER TABLE moderation_actions
    ADD CONSTRAINT chk_moderation_actions_target_type CHECK (
        target_type IN (
            'NOTE',
            'MARKETPLACE_LISTING',
            'DISCUSSION_QUESTION',
            'DISCUSSION_ANSWER',
            'EVENT',
            'INTERNSHIP',
            'LOST_FOUND_ITEM',
            'AI_GENERATED_ITEM',
            'USER_PROFILE',
            'SYSTEM'
        )
    );

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS chk_notifications_type;
ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_type CHECK (
        type IN (
            'SYSTEM',
            'USER_REMINDER',
            'DISCUSSION_REPLY',
            'DISCUSSION_ACCEPTED',
            'MARKETPLACE_UPDATE',
            'EVENT_UPDATE',
            'EVENT_REMINDER',
            'INTERNSHIP_POSTED',
            'NOTE_ACTIVITY',
            'LOST_FOUND_UPDATE',
            'ADMIN_MESSAGE'
        )
    );

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS chk_notifications_target_type;
ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_target_type CHECK (
        target_type IS NULL
        OR target_type IN (
            'SYSTEM',
            'USER',
            'NOTE',
            'MARKETPLACE_LISTING',
            'DISCUSSION_QUESTION',
            'DISCUSSION_ANSWER',
            'EVENT',
            'INTERNSHIP',
            'LOST_FOUND_ITEM',
            'LOST_FOUND_CLAIM',
            'LOST_FOUND_MATCH'
        )
    );
