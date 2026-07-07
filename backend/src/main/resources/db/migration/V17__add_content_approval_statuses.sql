ALTER TABLE marketplace_listings
    DROP CONSTRAINT IF EXISTS chk_marketplace_listings_status;

ALTER TABLE marketplace_listings
    ADD CONSTRAINT chk_marketplace_listings_status
    CHECK (
        status IN (
            'PENDING_REVIEW',
            'ACTIVE',
            'SOLD',
            'REJECTED',
            'DELETED'
        )
    );

CREATE INDEX IF NOT EXISTS idx_marketplace_listings_pending_review
    ON marketplace_listings (created_at DESC, id)
    WHERE status = 'PENDING_REVIEW' AND deleted_at IS NULL;

ALTER TABLE events
    DROP CONSTRAINT IF EXISTS chk_events_status;

ALTER TABLE events
    ADD CONSTRAINT chk_events_status
    CHECK (
        status IN (
            'PENDING_REVIEW',
            'UPCOMING',
            'CANCELLED',
            'COMPLETED',
            'REJECTED'
        )
    );

CREATE INDEX IF NOT EXISTS idx_events_pending_review
    ON events (created_at DESC, id)
    WHERE status = 'PENDING_REVIEW' AND deleted = FALSE;

ALTER TABLE discussion_questions
    DROP CONSTRAINT IF EXISTS chk_discussion_questions_status;

ALTER TABLE discussion_questions
    ADD CONSTRAINT chk_discussion_questions_status
    CHECK (
        status IN (
            'PENDING_REVIEW',
            'OPEN',
            'RESOLVED',
            'CLOSED',
            'HIDDEN',
            'REJECTED'
        )
    );

CREATE INDEX IF NOT EXISTS idx_discussion_questions_pending_review
    ON discussion_questions (created_at DESC, id)
    WHERE status = 'PENDING_REVIEW' AND deleted = FALSE;

ALTER TABLE internships
    DROP CONSTRAINT IF EXISTS chk_internships_status;

ALTER TABLE internships
    ADD CONSTRAINT chk_internships_status
    CHECK (
        status IN (
            'PENDING_REVIEW',
            'OPEN',
            'CLOSED',
            'EXPIRED',
            'REJECTED'
        )
    );

CREATE INDEX IF NOT EXISTS idx_internships_pending_review
    ON internships (created_at DESC, id)
    WHERE status = 'PENDING_REVIEW' AND deleted = FALSE;

ALTER TABLE moderation_actions
    DROP CONSTRAINT IF EXISTS chk_moderation_actions_action_type;

ALTER TABLE moderation_actions
    ADD CONSTRAINT chk_moderation_actions_action_type
    CHECK (
        action_type IN (
            'REPORT_REVIEWED',
            'REPORT_RESOLVED',
            'REPORT_DISMISSED',
            'CONTENT_APPROVED',
            'CONTENT_REJECTED',
            'CONTENT_WARNING',
            'CONTENT_HIDDEN',
            'CONTENT_RESTORED',
            'USER_WARNING',
            'USER_RESTRICTED',
            'USER_RESTORED',
            'SYSTEM_NOTE'
        )
    );
