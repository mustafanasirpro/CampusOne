CREATE TABLE internships (
    id UUID PRIMARY KEY,
    poster_user_id UUID NOT NULL,
    title VARCHAR(180) NOT NULL,
    company_name VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(255) NOT NULL,
    internship_type VARCHAR(30) NOT NULL,
    work_mode VARCHAR(30) NOT NULL,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    stipend_amount NUMERIC(12, 2),
    currency VARCHAR(10),
    apply_url VARCHAR(1000) NOT NULL,
    deadline TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_internships_poster
        FOREIGN KEY (poster_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_internships_title
        CHECK (CHAR_LENGTH(BTRIM(title)) BETWEEN 5 AND 180),
    CONSTRAINT chk_internships_company_name
        CHECK (CHAR_LENGTH(BTRIM(company_name)) BETWEEN 2 AND 160),
    CONSTRAINT chk_internships_description
        CHECK (CHAR_LENGTH(BTRIM(description)) BETWEEN 20 AND 5000),
    CONSTRAINT chk_internships_location
        CHECK (CHAR_LENGTH(BTRIM(location)) BETWEEN 2 AND 255),
    CONSTRAINT chk_internships_type CHECK (
        internship_type IN (
            'FULL_TIME',
            'PART_TIME',
            'SUMMER',
            'WINTER',
            'REMOTE_INTERNSHIP'
        )
    ),
    CONSTRAINT chk_internships_work_mode
        CHECK (work_mode IN ('ONSITE', 'REMOTE', 'HYBRID')),
    CONSTRAINT chk_internships_stipend
        CHECK (stipend_amount IS NULL OR stipend_amount >= 0),
    CONSTRAINT chk_internships_currency CHECK (
        currency IS NULL
        OR CHAR_LENGTH(BTRIM(currency)) BETWEEN 3 AND 10
    ),
    CONSTRAINT chk_internships_apply_url CHECK (
        CHAR_LENGTH(BTRIM(apply_url)) BETWEEN 8 AND 1000
        AND apply_url ~* '^https?://'
    ),
    CONSTRAINT chk_internships_status
        CHECK (status IN ('OPEN', 'CLOSED', 'EXPIRED')),
    CONSTRAINT chk_internships_timestamps
        CHECK (updated_at >= created_at),
    CONSTRAINT chk_internships_version
        CHECK (version >= 0)
);

CREATE TABLE saved_internships (
    internship_id UUID NOT NULL,
    user_id UUID NOT NULL,
    saved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (internship_id, user_id),
    CONSTRAINT fk_saved_internships_internship
        FOREIGN KEY (internship_id)
        REFERENCES internships (id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_internships_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_internships_poster_user_id
    ON internships (poster_user_id);
CREATE INDEX idx_internships_status
    ON internships (status);
CREATE INDEX idx_internships_type
    ON internships (internship_type);
CREATE INDEX idx_internships_work_mode
    ON internships (work_mode);
CREATE INDEX idx_internships_paid
    ON internships (paid);
CREATE INDEX idx_internships_deadline
    ON internships (deadline);
CREATE INDEX idx_internships_created_at
    ON internships (created_at DESC);
CREATE INDEX idx_internships_deleted
    ON internships (deleted);
CREATE INDEX idx_saved_internships_user_id
    ON saved_internships (user_id);
CREATE INDEX idx_saved_internships_internship_id
    ON saved_internships (internship_id);
