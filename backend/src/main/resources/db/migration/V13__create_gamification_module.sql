CREATE TABLE gamification_profiles (
    user_id UUID PRIMARY KEY,
    total_xp INTEGER NOT NULL DEFAULT 0,
    level INTEGER NOT NULL DEFAULT 1,
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    last_activity_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gamification_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_gamification_profiles_total_xp
        CHECK (total_xp >= 0),
    CONSTRAINT chk_gamification_profiles_level
        CHECK (level >= 1),
    CONSTRAINT chk_gamification_profiles_current_streak
        CHECK (current_streak >= 0),
    CONSTRAINT chk_gamification_profiles_longest_streak
        CHECK (longest_streak >= 0),
    CONSTRAINT chk_gamification_profiles_version
        CHECK (version >= 0),
    CONSTRAINT chk_gamification_profiles_timestamps
        CHECK (updated_at >= created_at)
);

CREATE TABLE xp_transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    action_type VARCHAR(60) NOT NULL,
    points INTEGER NOT NULL,
    source_type VARCHAR(60),
    source_id UUID,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_xp_transactions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_xp_transactions_action_type CHECK (
        action_type IN (
            'PROFILE_COMPLETED',
            'NOTE_CREATED',
            'NOTE_DOWNLOADED',
            'NOTE_RATED',
            'MARKETPLACE_LISTING_CREATED',
            'DISCUSSION_QUESTION_CREATED',
            'DISCUSSION_ANSWER_CREATED',
            'DISCUSSION_ANSWER_ACCEPTED',
            'EVENT_CREATED',
            'EVENT_JOINED',
            'INTERNSHIP_CREATED',
            'DAILY_LOGIN',
            'SYSTEM_AWARD'
        )
    ),
    CONSTRAINT chk_xp_transactions_points
        CHECK (points > 0),
    CONSTRAINT chk_xp_transactions_source_type CHECK (
        source_type IS NULL
        OR source_type IN (
            'USER',
            'NOTE',
            'MARKETPLACE_LISTING',
            'DISCUSSION_QUESTION',
            'DISCUSSION_ANSWER',
            'EVENT',
            'INTERNSHIP',
            'SYSTEM'
        )
    ),
    CONSTRAINT chk_xp_transactions_source_pair
        CHECK (source_id IS NULL OR source_type IS NOT NULL),
    CONSTRAINT chk_xp_transactions_description
        CHECK (
            description IS NULL
            OR CHAR_LENGTH(description) <= 500
        )
);

CREATE UNIQUE INDEX uk_xp_transactions_award_source
    ON xp_transactions (
        user_id,
        action_type,
        source_type,
        source_id
    )
    WHERE source_type IS NOT NULL AND source_id IS NOT NULL;

CREATE TABLE badges (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category VARCHAR(60) NOT NULL,
    icon VARCHAR(120),
    xp_required INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_badges_code UNIQUE (code),
    CONSTRAINT chk_badges_code
        CHECK (CHAR_LENGTH(BTRIM(code)) BETWEEN 3 AND 80),
    CONSTRAINT chk_badges_name
        CHECK (CHAR_LENGTH(BTRIM(name)) BETWEEN 3 AND 120),
    CONSTRAINT chk_badges_description
        CHECK (CHAR_LENGTH(BTRIM(description)) BETWEEN 3 AND 500),
    CONSTRAINT chk_badges_category
        CHECK (CHAR_LENGTH(BTRIM(category)) BETWEEN 2 AND 60),
    CONSTRAINT chk_badges_icon
        CHECK (icon IS NULL OR CHAR_LENGTH(icon) <= 120),
    CONSTRAINT chk_badges_xp_required
        CHECK (xp_required >= 0),
    CONSTRAINT chk_badges_sort_order
        CHECK (sort_order >= 0),
    CONSTRAINT chk_badges_timestamps
        CHECK (updated_at >= created_at)
);

CREATE TABLE user_badges (
    badge_id UUID NOT NULL,
    user_id UUID NOT NULL,
    awarded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_type VARCHAR(60),
    source_id UUID,
    CONSTRAINT pk_user_badges PRIMARY KEY (badge_id, user_id),
    CONSTRAINT fk_user_badges_badge
        FOREIGN KEY (badge_id) REFERENCES badges (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_badges_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_badges_source_type CHECK (
        source_type IS NULL
        OR source_type IN (
            'USER',
            'NOTE',
            'MARKETPLACE_LISTING',
            'DISCUSSION_QUESTION',
            'DISCUSSION_ANSWER',
            'EVENT',
            'INTERNSHIP',
            'SYSTEM'
        )
    ),
    CONSTRAINT chk_user_badges_source_pair
        CHECK (source_id IS NULL OR source_type IS NOT NULL)
);

CREATE INDEX idx_gamification_profiles_total_xp
    ON gamification_profiles (
        total_xp DESC,
        created_at ASC,
        user_id ASC
    );
CREATE INDEX idx_gamification_profiles_level
    ON gamification_profiles (level);
CREATE INDEX idx_gamification_profiles_last_activity_at
    ON gamification_profiles (last_activity_at);

CREATE INDEX idx_xp_transactions_user_id
    ON xp_transactions (user_id, created_at DESC);
CREATE INDEX idx_xp_transactions_action_type
    ON xp_transactions (action_type);
CREATE INDEX idx_xp_transactions_source_type
    ON xp_transactions (source_type);
CREATE INDEX idx_xp_transactions_created_at
    ON xp_transactions (created_at DESC, user_id);

CREATE INDEX idx_badges_category
    ON badges (category);
CREATE INDEX idx_badges_active
    ON badges (active);
CREATE INDEX idx_badges_xp_required
    ON badges (xp_required);

CREATE INDEX idx_user_badges_user_id
    ON user_badges (user_id, awarded_at DESC);
CREATE INDEX idx_user_badges_badge_id
    ON user_badges (badge_id);
CREATE INDEX idx_user_badges_awarded_at
    ON user_badges (awarded_at DESC);

INSERT INTO badges (
    id,
    code,
    name,
    description,
    category,
    xp_required,
    sort_order
)
VALUES
    (
        '80000000-0000-4000-8000-000000000001',
        'FIRST_STEPS',
        'First Steps',
        'Earn your first XP on CampusOne',
        'XP',
        1,
        1
    ),
    (
        '80000000-0000-4000-8000-000000000002',
        'ACTIVE_STUDENT',
        'Active Student',
        'Earn 100 XP',
        'XP',
        100,
        2
    ),
    (
        '80000000-0000-4000-8000-000000000003',
        'CAMPUS_CONTRIBUTOR',
        'Campus Contributor',
        'Earn 500 XP',
        'XP',
        500,
        3
    ),
    (
        '80000000-0000-4000-8000-000000000004',
        'COMMUNITY_LEADER',
        'Community Leader',
        'Earn 1000 XP',
        'XP',
        1000,
        4
    ),
    (
        '80000000-0000-4000-8000-000000000005',
        'CAMPUS_LEGEND',
        'Campus Legend',
        'Earn 2500 XP',
        'XP',
        2500,
        5
    )
ON CONFLICT (code) DO NOTHING;
