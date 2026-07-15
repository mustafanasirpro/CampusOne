CREATE TABLE aura_import_jobs (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    term_id UUID NOT NULL,
    import_type VARCHAR(40) NOT NULL,
    file_format VARCHAR(12) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PREVIEWED',
    source_name VARCHAR(160),
    headers JSONB NOT NULL DEFAULT '[]'::JSONB,
    raw_preview JSONB NOT NULL DEFAULT '[]'::JSONB,
    suggested_mapping JSONB NOT NULL DEFAULT '{}'::JSONB,
    applied_mapping JSONB,
    accepted_rows INTEGER NOT NULL DEFAULT 0,
    rejected_rows INTEGER NOT NULL DEFAULT 0,
    result_version_id UUID,
    message VARCHAR(500),
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_import_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_import_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_import_result_version
        FOREIGN KEY (result_version_id) REFERENCES aura_timetable_versions (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_import_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_import_type
        CHECK (import_type IN ('TIMETABLE', 'PROGRAMS', 'BATCHES', 'SECTIONS',
            'INSTRUCTORS', 'ROOMS', 'TIMESLOTS', 'AVAILABILITY', 'OFFERINGS',
            'REQUIREMENTS', 'CONFLICTS', 'REGISTRATIONS', 'EXCEPTIONS', 'TRAVEL_RULES')),
    CONSTRAINT chk_aura_import_format
        CHECK (file_format IN ('CSV', 'XLSX', 'XLS', 'PDF')),
    CONSTRAINT chk_aura_import_status
        CHECK (status IN ('PREVIEWED', 'VALIDATED', 'APPLYING', 'APPLIED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_aura_import_counts
        CHECK (accepted_rows >= 0 AND rejected_rows >= 0)
);

CREATE INDEX idx_aura_import_jobs_term_created
    ON aura_import_jobs (term_id, created_at DESC);

CREATE TABLE aura_import_row_errors (
    id UUID PRIMARY KEY,
    import_job_id UUID NOT NULL,
    row_number INTEGER NOT NULL,
    field_name VARCHAR(120),
    error_code VARCHAR(60) NOT NULL,
    message VARCHAR(500) NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'ERROR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_import_error_job
        FOREIGN KEY (import_job_id) REFERENCES aura_import_jobs (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_import_error_row CHECK (row_number >= 0),
    CONSTRAINT chk_aura_import_error_severity
        CHECK (severity IN ('ERROR', 'WARNING'))
);

CREATE INDEX idx_aura_import_errors_job_row
    ON aura_import_row_errors (import_job_id, row_number);

CREATE TABLE aura_import_mapping_profiles (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    import_type VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    mapping JSONB NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_mapping_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_mapping_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uk_aura_mapping_profile_name_lower
    ON aura_import_mapping_profiles (university_id, import_type, LOWER(name))
    WHERE active = TRUE;
