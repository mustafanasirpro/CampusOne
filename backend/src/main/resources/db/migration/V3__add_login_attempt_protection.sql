ALTER TABLE users
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMPTZ,
    ADD CONSTRAINT chk_users_failed_login_attempts
        CHECK (failed_login_attempts >= 0);

CREATE INDEX idx_users_locked_until
    ON users (locked_until)
    WHERE locked_until IS NOT NULL;
