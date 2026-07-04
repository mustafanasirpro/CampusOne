CREATE TABLE moderators (
    user_id UUID PRIMARY KEY,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_by_user_id UUID,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_moderators_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_moderators_assigned_by
        FOREIGN KEY (assigned_by_user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_moderators_role
        CHECK (role IN ('ADMIN', 'MODERATOR')),
    CONSTRAINT chk_moderators_version
        CHECK (version >= 0),
    CONSTRAINT chk_moderators_timestamps
        CHECK (updated_at >= created_at)
);

CREATE TABLE content_reports (
    id UUID PRIMARY KEY,
    reporter_user_id UUID NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_id UUID NOT NULL,
    reason VARCHAR(60) NOT NULL,
    details VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id UUID,
    reviewed_at TIMESTAMPTZ,
    resolution_note VARCHAR(1000),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_content_reports_reporter
        FOREIGN KEY (reporter_user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_content_reports_reviewed_by
        FOREIGN KEY (reviewed_by_user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_content_reports_target_type CHECK (
        target_type IN (
            'NOTE',
            'MARKETPLACE_LISTING',
            'DISCUSSION_QUESTION',
            'DISCUSSION_ANSWER',
            'EVENT',
            'INTERNSHIP',
            'AI_GENERATED_ITEM',
            'USER_PROFILE',
            'SYSTEM'
        )
    ),
    CONSTRAINT chk_content_reports_reason CHECK (
        reason IN (
            'SPAM',
            'HARASSMENT',
            'HATE_SPEECH',
            'MISINFORMATION',
            'INAPPROPRIATE_CONTENT',
            'SCAM',
            'DUPLICATE',
            'PRIVACY_CONCERN',
            'OTHER'
        )
    ),
    CONSTRAINT chk_content_reports_status CHECK (
        status IN (
            'PENDING',
            'UNDER_REVIEW',
            'RESOLVED',
            'DISMISSED'
        )
    ),
    CONSTRAINT chk_content_reports_details
        CHECK (details IS NULL OR CHAR_LENGTH(details) <= 1000),
    CONSTRAINT chk_content_reports_resolution_note
        CHECK (
            resolution_note IS NULL
            OR CHAR_LENGTH(resolution_note) <= 1000
        ),
    CONSTRAINT chk_content_reports_version
        CHECK (version >= 0),
    CONSTRAINT chk_content_reports_timestamps
        CHECK (updated_at >= created_at)
);

CREATE UNIQUE INDEX uk_content_reports_active_target
    ON content_reports (
        reporter_user_id,
        target_type,
        target_id
    )
    WHERE status IN ('PENDING', 'UNDER_REVIEW')
      AND deleted = FALSE;

CREATE TABLE moderation_actions (
    id UUID PRIMARY KEY,
    moderator_user_id UUID NOT NULL,
    report_id UUID,
    action_type VARCHAR(60) NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_id UUID NOT NULL,
    reason VARCHAR(1000),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_moderation_actions_moderator
        FOREIGN KEY (moderator_user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_moderation_actions_report
        FOREIGN KEY (report_id)
        REFERENCES content_reports (id) ON DELETE SET NULL,
    CONSTRAINT chk_moderation_actions_action_type CHECK (
        action_type IN (
            'REPORT_REVIEWED',
            'REPORT_RESOLVED',
            'REPORT_DISMISSED',
            'CONTENT_WARNING',
            'CONTENT_HIDDEN',
            'CONTENT_RESTORED',
            'USER_WARNING',
            'USER_RESTRICTED',
            'USER_RESTORED',
            'SYSTEM_NOTE'
        )
    ),
    CONSTRAINT chk_moderation_actions_target_type CHECK (
        target_type IN (
            'NOTE',
            'MARKETPLACE_LISTING',
            'DISCUSSION_QUESTION',
            'DISCUSSION_ANSWER',
            'EVENT',
            'INTERNSHIP',
            'AI_GENERATED_ITEM',
            'USER_PROFILE',
            'SYSTEM'
        )
    ),
    CONSTRAINT chk_moderation_actions_reason
        CHECK (reason IS NULL OR CHAR_LENGTH(reason) <= 1000)
);

CREATE INDEX idx_moderators_user_id
    ON moderators (user_id);
CREATE INDEX idx_moderators_role
    ON moderators (role);
CREATE INDEX idx_moderators_active
    ON moderators (active);

CREATE INDEX idx_content_reports_reporter_user_id
    ON content_reports (reporter_user_id);
CREATE INDEX idx_content_reports_target
    ON content_reports (target_type, target_id);
CREATE INDEX idx_content_reports_status
    ON content_reports (status);
CREATE INDEX idx_content_reports_reason
    ON content_reports (reason);
CREATE INDEX idx_content_reports_created_at
    ON content_reports (created_at DESC);
CREATE INDEX idx_content_reports_reviewed_by_user_id
    ON content_reports (reviewed_by_user_id);
CREATE INDEX idx_content_reports_deleted
    ON content_reports (deleted);

CREATE INDEX idx_moderation_actions_moderator_user_id
    ON moderation_actions (moderator_user_id);
CREATE INDEX idx_moderation_actions_report_id
    ON moderation_actions (report_id);
CREATE INDEX idx_moderation_actions_action_type
    ON moderation_actions (action_type);
CREATE INDEX idx_moderation_actions_target
    ON moderation_actions (target_type, target_id);
CREATE INDEX idx_moderation_actions_created_at
    ON moderation_actions (created_at DESC);
