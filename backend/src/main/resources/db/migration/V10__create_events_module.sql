CREATE TABLE events (
    id UUID PRIMARY KEY,
    organizer_user_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(255) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    capacity INTEGER NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UPCOMING',
    participant_count INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_events_organizer
        FOREIGN KEY (organizer_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_events_title
        CHECK (CHAR_LENGTH(BTRIM(title)) BETWEEN 5 AND 160),
    CONSTRAINT chk_events_description
        CHECK (CHAR_LENGTH(BTRIM(description)) BETWEEN 10 AND 5000),
    CONSTRAINT chk_events_location
        CHECK (CHAR_LENGTH(BTRIM(location)) BETWEEN 2 AND 255),
    CONSTRAINT chk_events_capacity
        CHECK (capacity > 0),
    CONSTRAINT chk_events_participant_count
        CHECK (participant_count >= 0 AND participant_count <= capacity),
    CONSTRAINT chk_events_time_range
        CHECK (end_time > start_time),
    CONSTRAINT chk_events_visibility
        CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT chk_events_status
        CHECK (status IN ('UPCOMING', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_events_timestamps
        CHECK (updated_at >= created_at),
    CONSTRAINT chk_events_version
        CHECK (version >= 0)
);

CREATE TABLE event_participants (
    event_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, user_id),
    CONSTRAINT fk_event_participants_event
        FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_event_participants_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_events_organizer_user_id
    ON events (organizer_user_id);
CREATE INDEX idx_events_status
    ON events (status);
CREATE INDEX idx_events_visibility
    ON events (visibility);
CREATE INDEX idx_events_start_time
    ON events (start_time);
CREATE INDEX idx_events_created_at
    ON events (created_at DESC);
CREATE INDEX idx_events_deleted
    ON events (deleted);
CREATE INDEX idx_event_participants_user_id
    ON event_participants (user_id);
CREATE INDEX idx_event_participants_event_id
    ON event_participants (event_id);
