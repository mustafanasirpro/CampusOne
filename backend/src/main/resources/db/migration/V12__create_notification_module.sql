CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    target_type VARCHAR(50),
    target_id UUID,
    action_url VARCHAR(1000),
    read_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_notifications_type CHECK (
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
            'ADMIN_MESSAGE'
        )
    ),
    CONSTRAINT chk_notifications_title
        CHECK (CHAR_LENGTH(BTRIM(title)) BETWEEN 3 AND 160),
    CONSTRAINT chk_notifications_message
        CHECK (CHAR_LENGTH(BTRIM(message)) BETWEEN 3 AND 1000),
    CONSTRAINT chk_notifications_target_type CHECK (
        target_type IS NULL
        OR target_type IN (
            'SYSTEM',
            'USER',
            'NOTE',
            'MARKETPLACE_LISTING',
            'DISCUSSION_QUESTION',
            'DISCUSSION_ANSWER',
            'EVENT',
            'INTERNSHIP'
        )
    ),
    CONSTRAINT chk_notifications_target_pair
        CHECK (target_id IS NULL OR target_type IS NOT NULL),
    CONSTRAINT chk_notifications_action_url
        CHECK (action_url IS NULL OR CHAR_LENGTH(action_url) <= 1000),
    CONSTRAINT chk_notifications_timestamps
        CHECK (updated_at >= created_at),
    CONSTRAINT chk_notifications_version
        CHECK (version >= 0)
);

CREATE INDEX idx_notifications_recipient_user_id
    ON notifications (recipient_user_id);
CREATE INDEX idx_notifications_recipient_read_at
    ON notifications (recipient_user_id, read_at);
CREATE INDEX idx_notifications_recipient_deleted
    ON notifications (recipient_user_id, deleted);
CREATE INDEX idx_notifications_type
    ON notifications (type);
CREATE INDEX idx_notifications_target_type
    ON notifications (target_type);
CREATE INDEX idx_notifications_created_at
    ON notifications (created_at DESC);
CREATE INDEX idx_notifications_deleted
    ON notifications (deleted);
