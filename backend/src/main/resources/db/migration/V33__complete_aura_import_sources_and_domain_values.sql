CREATE TABLE aura_import_source_rows (
    import_job_id UUID NOT NULL,
    row_number INTEGER NOT NULL,
    row_data JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (import_job_id, row_number),
    CONSTRAINT fk_aura_import_source_row_job
        FOREIGN KEY (import_job_id) REFERENCES aura_import_jobs (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_import_source_row_number CHECK (row_number > 0)
);

CREATE INDEX idx_aura_import_source_rows_job
    ON aura_import_source_rows (import_job_id, row_number);

ALTER TABLE aura_rooms DROP CONSTRAINT chk_aura_rooms_type;
ALTER TABLE aura_rooms ADD CONSTRAINT chk_aura_rooms_type
    CHECK (room_type IN (
        'CLASSROOM', 'LAB', 'LECTURE_HALL',
        'LECTURE_ROOM', 'COMPUTER_LAB', 'SCIENCE_LAB',
        'SEMINAR_ROOM', 'AUDITORIUM', 'STUDIO', 'WORKSHOP',
        'ONLINE', 'OTHER'));

ALTER TABLE aura_meeting_requirements
    DROP CONSTRAINT chk_aura_requirements_meeting_type;
ALTER TABLE aura_meeting_requirements
    ADD CONSTRAINT chk_aura_requirements_meeting_type
    CHECK (meeting_type IN (
        'LECTURE', 'LAB', 'TUTORIAL', 'SEMINAR', 'PROJECT',
        'WORKSHOP', 'ONLINE', 'HYBRID', 'OTHER'));

ALTER TABLE aura_meeting_requirements
    DROP CONSTRAINT chk_aura_requirements_room_type;
ALTER TABLE aura_meeting_requirements
    ADD CONSTRAINT chk_aura_requirements_room_type
    CHECK (room_type IN (
        'CLASSROOM', 'LAB', 'LECTURE_HALL',
        'LECTURE_ROOM', 'COMPUTER_LAB', 'SCIENCE_LAB',
        'SEMINAR_ROOM', 'AUDITORIUM', 'STUDIO', 'WORKSHOP',
        'ONLINE', 'OTHER'));
