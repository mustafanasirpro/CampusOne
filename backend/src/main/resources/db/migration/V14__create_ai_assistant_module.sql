CREATE TABLE ai_chat_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    mode VARCHAR(40) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_chat_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_ai_chat_sessions_title
        CHECK (CHAR_LENGTH(BTRIM(title)) BETWEEN 3 AND 160),
    CONSTRAINT chk_ai_chat_sessions_mode CHECK (
        mode IN (
            'GENERAL_CHAT',
            'EXPLAIN_CONCEPT',
            'SUMMARIZE',
            'FLASHCARDS',
            'QUIZ',
            'STUDY_PLAN'
        )
    ),
    CONSTRAINT chk_ai_chat_sessions_version
        CHECK (version >= 0),
    CONSTRAINT chk_ai_chat_sessions_timestamps
        CHECK (updated_at >= created_at)
);

CREATE TABLE ai_chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    token_estimate INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_chat_messages_session
        FOREIGN KEY (session_id)
        REFERENCES ai_chat_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_chat_messages_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_ai_chat_messages_role
        CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    CONSTRAINT chk_ai_chat_messages_content
        CHECK (CHAR_LENGTH(content) BETWEEN 1 AND 10000),
    CONSTRAINT chk_ai_chat_messages_token_estimate
        CHECK (token_estimate >= 0)
);

CREATE TABLE ai_generated_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    session_id UUID,
    item_type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    source_text TEXT,
    generated_content JSONB NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_generated_items_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_generated_items_session
        FOREIGN KEY (session_id)
        REFERENCES ai_chat_sessions (id) ON DELETE SET NULL,
    CONSTRAINT chk_ai_generated_items_type CHECK (
        item_type IN (
            'SUMMARY',
            'FLASHCARDS',
            'QUIZ',
            'STUDY_PLAN'
        )
    ),
    CONSTRAINT chk_ai_generated_items_title
        CHECK (CHAR_LENGTH(BTRIM(title)) BETWEEN 3 AND 160),
    CONSTRAINT chk_ai_generated_items_timestamps
        CHECK (updated_at >= created_at)
);

CREATE TABLE ai_usage_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    feature VARCHAR(40) NOT NULL,
    input_token_estimate INTEGER NOT NULL DEFAULT 0,
    output_token_estimate INTEGER NOT NULL DEFAULT 0,
    provider VARCHAR(60) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_usage_records_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_ai_usage_records_feature CHECK (
        feature IN (
            'CHAT',
            'EXPLAIN_CONCEPT',
            'SUMMARIZE',
            'FLASHCARDS',
            'QUIZ',
            'STUDY_PLAN'
        )
    ),
    CONSTRAINT chk_ai_usage_records_input_tokens
        CHECK (input_token_estimate >= 0),
    CONSTRAINT chk_ai_usage_records_output_tokens
        CHECK (output_token_estimate >= 0),
    CONSTRAINT chk_ai_usage_records_provider
        CHECK (CHAR_LENGTH(BTRIM(provider)) BETWEEN 2 AND 60)
);

CREATE INDEX idx_ai_chat_sessions_user_id
    ON ai_chat_sessions (user_id, updated_at DESC);
CREATE INDEX idx_ai_chat_sessions_mode
    ON ai_chat_sessions (mode);
CREATE INDEX idx_ai_chat_sessions_deleted
    ON ai_chat_sessions (deleted);
CREATE INDEX idx_ai_chat_sessions_created_at
    ON ai_chat_sessions (created_at DESC);

CREATE INDEX idx_ai_chat_messages_session_id
    ON ai_chat_messages (session_id, created_at ASC);
CREATE INDEX idx_ai_chat_messages_user_id
    ON ai_chat_messages (user_id);
CREATE INDEX idx_ai_chat_messages_role
    ON ai_chat_messages (role);
CREATE INDEX idx_ai_chat_messages_created_at
    ON ai_chat_messages (created_at DESC);

CREATE INDEX idx_ai_generated_items_user_id
    ON ai_generated_items (user_id, created_at DESC);
CREATE INDEX idx_ai_generated_items_session_id
    ON ai_generated_items (session_id);
CREATE INDEX idx_ai_generated_items_item_type
    ON ai_generated_items (item_type);
CREATE INDEX idx_ai_generated_items_deleted
    ON ai_generated_items (deleted);
CREATE INDEX idx_ai_generated_items_created_at
    ON ai_generated_items (created_at DESC);

CREATE INDEX idx_ai_usage_records_user_id
    ON ai_usage_records (user_id, created_at DESC);
CREATE INDEX idx_ai_usage_records_feature
    ON ai_usage_records (feature);
CREATE INDEX idx_ai_usage_records_provider
    ON ai_usage_records (provider);
CREATE INDEX idx_ai_usage_records_created_at
    ON ai_usage_records (created_at DESC);
