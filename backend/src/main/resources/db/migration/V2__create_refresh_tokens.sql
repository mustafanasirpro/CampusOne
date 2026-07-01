CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    token_family UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT uk_refresh_tokens_replacement UNIQUE (replaced_by_token_id),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replacement
        FOREIGN KEY (replaced_by_token_id)
        REFERENCES refresh_tokens (id) ON DELETE SET NULL,
    CONSTRAINT chk_refresh_tokens_expiry CHECK (expires_at > created_at),
    CONSTRAINT chk_refresh_tokens_hash_length CHECK (CHAR_LENGTH(token_hash) = 64),
    CONSTRAINT chk_refresh_tokens_version CHECK (version >= 0)
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family
    ON refresh_tokens (token_family);
CREATE INDEX idx_refresh_tokens_active_expiry
    ON refresh_tokens (expires_at)
    WHERE revoked_at IS NULL;
