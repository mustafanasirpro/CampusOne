CREATE TABLE aura_academic_terms (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(140) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_terms_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_terms_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_terms_dates CHECK (starts_on <= ends_on),
    CONSTRAINT chk_aura_terms_status
        CHECK (status IN ('DRAFT', 'READY', 'GENERATING', 'PUBLISHED', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uk_aura_terms_university_code_lower
    ON aura_academic_terms (university_id, LOWER(code));
CREATE INDEX idx_aura_terms_university_status
    ON aura_academic_terms (university_id, status);

CREATE TABLE aura_programs (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    department_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_programs_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_programs_department
        FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uk_aura_programs_university_code_lower
    ON aura_programs (university_id, LOWER(code));

CREATE TABLE aura_batches (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL,
    admission_year INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_batches_program
        FOREIGN KEY (program_id) REFERENCES aura_programs (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_batches_year CHECK (admission_year BETWEEN 2000 AND 2100)
);

CREATE UNIQUE INDEX uk_aura_batches_program_code_lower
    ON aura_batches (program_id, LOWER(code));

CREATE TABLE aura_sections (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    student_count INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_sections_batch
        FOREIGN KEY (batch_id) REFERENCES aura_batches (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_sections_student_count CHECK (student_count > 0)
);

CREATE UNIQUE INDEX uk_aura_sections_batch_code_lower
    ON aura_sections (batch_id, LOWER(code));

CREATE TABLE aura_instructors (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    user_id UUID,
    display_name VARCHAR(140) NOT NULL,
    email VARCHAR(254),
    max_hours_per_week INTEGER NOT NULL DEFAULT 18,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_instructors_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_instructors_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_aura_instructors_hours CHECK (max_hours_per_week BETWEEN 1 AND 60)
);

CREATE INDEX idx_aura_instructors_university_active
    ON aura_instructors (university_id, active);

CREATE TABLE aura_rooms (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    building VARCHAR(120),
    name VARCHAR(120) NOT NULL,
    capacity INTEGER NOT NULL,
    room_type VARCHAR(32) NOT NULL DEFAULT 'CLASSROOM',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_rooms_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_rooms_capacity CHECK (capacity > 0),
    CONSTRAINT chk_aura_rooms_type
        CHECK (room_type IN ('CLASSROOM', 'LAB', 'LECTURE_HALL', 'SEMINAR_ROOM'))
);

CREATE UNIQUE INDEX uk_aura_rooms_university_name_lower
    ON aura_rooms (university_id, LOWER(name));
CREATE INDEX idx_aura_rooms_university_active
    ON aura_rooms (university_id, active);

CREATE TABLE aura_timeslots (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    day_of_week SMALLINT NOT NULL,
    starts_at TIME NOT NULL,
    ends_at TIME NOT NULL,
    label VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_timeslots_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_timeslots_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_aura_timeslots_time CHECK (starts_at < ends_at)
);

CREATE UNIQUE INDEX uk_aura_timeslots_university_time
    ON aura_timeslots (university_id, day_of_week, starts_at, ends_at);
CREATE INDEX idx_aura_timeslots_university_active
    ON aura_timeslots (university_id, active);

CREATE TABLE aura_instructor_availability (
    id UUID PRIMARY KEY,
    instructor_id UUID NOT NULL,
    timeslot_id UUID NOT NULL,
    availability VARCHAR(20) NOT NULL,
    reason VARCHAR(300),
    CONSTRAINT fk_aura_instructor_availability_instructor
        FOREIGN KEY (instructor_id) REFERENCES aura_instructors (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_instructor_availability_timeslot
        FOREIGN KEY (timeslot_id) REFERENCES aura_timeslots (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_instructor_availability
        CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE', 'PREFERRED'))
);

CREATE UNIQUE INDEX uk_aura_instructor_availability
    ON aura_instructor_availability (instructor_id, timeslot_id);

CREATE TABLE aura_room_availability (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    timeslot_id UUID NOT NULL,
    availability VARCHAR(20) NOT NULL,
    reason VARCHAR(300),
    CONSTRAINT fk_aura_room_availability_room
        FOREIGN KEY (room_id) REFERENCES aura_rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_room_availability_timeslot
        FOREIGN KEY (timeslot_id) REFERENCES aura_timeslots (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_room_availability
        CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE', 'PREFERRED'))
);

CREATE UNIQUE INDEX uk_aura_room_availability
    ON aura_room_availability (room_id, timeslot_id);

CREATE TABLE aura_course_offerings (
    id UUID PRIMARY KEY,
    term_id UUID NOT NULL,
    course_id UUID NOT NULL,
    section_id UUID NOT NULL,
    instructor_id UUID NOT NULL,
    expected_students INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_offerings_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_offerings_course
        FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_offerings_section
        FOREIGN KEY (section_id) REFERENCES aura_sections (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_offerings_instructor
        FOREIGN KEY (instructor_id) REFERENCES aura_instructors (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_offerings_students CHECK (expected_students > 0),
    CONSTRAINT chk_aura_offerings_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_aura_offerings_term_status
    ON aura_course_offerings (term_id, status);

CREATE TABLE aura_meeting_requirements (
    id UUID PRIMARY KEY,
    offering_id UUID NOT NULL,
    meeting_type VARCHAR(32) NOT NULL DEFAULT 'LECTURE',
    sessions_per_week INTEGER NOT NULL DEFAULT 1,
    duration_slots INTEGER NOT NULL DEFAULT 1,
    room_type VARCHAR(32) NOT NULL DEFAULT 'CLASSROOM',
    required_capacity INTEGER NOT NULL,
    notes VARCHAR(300),
    CONSTRAINT fk_aura_requirements_offering
        FOREIGN KEY (offering_id) REFERENCES aura_course_offerings (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_requirements_meeting_type
        CHECK (meeting_type IN ('LECTURE', 'LAB', 'TUTORIAL', 'SEMINAR')),
    CONSTRAINT chk_aura_requirements_sessions CHECK (sessions_per_week BETWEEN 1 AND 6),
    CONSTRAINT chk_aura_requirements_duration CHECK (duration_slots BETWEEN 1 AND 4),
    CONSTRAINT chk_aura_requirements_capacity CHECK (required_capacity > 0),
    CONSTRAINT chk_aura_requirements_room_type
        CHECK (room_type IN ('CLASSROOM', 'LAB', 'LECTURE_HALL', 'SEMINAR_ROOM'))
);

CREATE TABLE aura_cross_offering_conflicts (
    id UUID PRIMARY KEY,
    left_offering_id UUID NOT NULL,
    right_offering_id UUID NOT NULL,
    reason VARCHAR(300) NOT NULL,
    CONSTRAINT fk_aura_conflicts_left
        FOREIGN KEY (left_offering_id) REFERENCES aura_course_offerings (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_conflicts_right
        FOREIGN KEY (right_offering_id) REFERENCES aura_course_offerings (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_conflicts_distinct CHECK (left_offering_id <> right_offering_id)
);

CREATE UNIQUE INDEX uk_aura_cross_offering_conflicts_pair
    ON aura_cross_offering_conflicts (
        LEAST(left_offering_id, right_offering_id),
        GREATEST(left_offering_id, right_offering_id)
    );

CREATE TABLE aura_scheduling_data_revisions (
    id UUID PRIMARY KEY,
    term_id UUID NOT NULL,
    revision_number INTEGER NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    summary TEXT NOT NULL,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_revisions_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_revisions_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uk_aura_revisions_term_number
    ON aura_scheduling_data_revisions (term_id, revision_number);

CREATE TABLE aura_generation_runs (
    id UUID PRIMARY KEY,
    term_id UUID NOT NULL,
    revision_id UUID,
    requested_by_user_id UUID NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'QUEUED',
    score VARCHAR(80),
    termination_seconds INTEGER NOT NULL DEFAULT 30,
    message VARCHAR(500),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_runs_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_runs_revision
        FOREIGN KEY (revision_id) REFERENCES aura_scheduling_data_revisions (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_runs_user
        FOREIGN KEY (requested_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_runs_status
        CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_aura_runs_termination CHECK (termination_seconds BETWEEN 1 AND 300)
);

CREATE INDEX idx_aura_runs_term_created
    ON aura_generation_runs (term_id, created_at DESC);

CREATE TABLE aura_timetable_versions (
    id UUID PRIMARY KEY,
    term_id UUID NOT NULL,
    generation_run_id UUID,
    version_number INTEGER NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    score VARCHAR(80),
    notes VARCHAR(500),
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    CONSTRAINT fk_aura_versions_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_versions_run
        FOREIGN KEY (generation_run_id) REFERENCES aura_generation_runs (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_versions_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_versions_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uk_aura_versions_term_number
    ON aura_timetable_versions (term_id, version_number);
CREATE UNIQUE INDEX uk_aura_versions_single_published
    ON aura_timetable_versions (term_id)
    WHERE status = 'PUBLISHED';

CREATE TABLE aura_scheduled_sessions (
    id UUID PRIMARY KEY,
    version_id UUID NOT NULL,
    meeting_requirement_id UUID NOT NULL,
    offering_id UUID NOT NULL,
    section_id UUID NOT NULL,
    instructor_id UUID NOT NULL,
    room_id UUID NOT NULL,
    timeslot_id UUID NOT NULL,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    source VARCHAR(24) NOT NULL DEFAULT 'SOLVER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_sessions_version
        FOREIGN KEY (version_id) REFERENCES aura_timetable_versions (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_sessions_requirement
        FOREIGN KEY (meeting_requirement_id) REFERENCES aura_meeting_requirements (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_sessions_offering
        FOREIGN KEY (offering_id) REFERENCES aura_course_offerings (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_sessions_section
        FOREIGN KEY (section_id) REFERENCES aura_sections (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_sessions_instructor
        FOREIGN KEY (instructor_id) REFERENCES aura_instructors (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_sessions_room
        FOREIGN KEY (room_id) REFERENCES aura_rooms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_sessions_timeslot
        FOREIGN KEY (timeslot_id) REFERENCES aura_timeslots (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_sessions_source CHECK (source IN ('SOLVER', 'MANUAL', 'REPAIR'))
);

CREATE INDEX idx_aura_sessions_version
    ON aura_scheduled_sessions (version_id);
CREATE INDEX idx_aura_sessions_room_timeslot
    ON aura_scheduled_sessions (version_id, room_id, timeslot_id);
CREATE INDEX idx_aura_sessions_instructor_timeslot
    ON aura_scheduled_sessions (version_id, instructor_id, timeslot_id);
CREATE INDEX idx_aura_sessions_section_timeslot
    ON aura_scheduled_sessions (version_id, section_id, timeslot_id);

CREATE TABLE aura_clashes (
    id UUID PRIMARY KEY,
    version_id UUID NOT NULL,
    clash_type VARCHAR(40) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    message VARCHAR(500) NOT NULL,
    primary_session_id UUID,
    secondary_session_id UUID,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT fk_aura_clashes_version
        FOREIGN KEY (version_id) REFERENCES aura_timetable_versions (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_clashes_primary_session
        FOREIGN KEY (primary_session_id) REFERENCES aura_scheduled_sessions (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_clashes_secondary_session
        FOREIGN KEY (secondary_session_id) REFERENCES aura_scheduled_sessions (id) ON DELETE SET NULL,
    CONSTRAINT chk_aura_clashes_type
        CHECK (clash_type IN ('ROOM_DOUBLE_BOOKED', 'INSTRUCTOR_DOUBLE_BOOKED', 'SECTION_DOUBLE_BOOKED', 'ROOM_TOO_SMALL', 'ROOM_TYPE_MISMATCH')),
    CONSTRAINT chk_aura_clashes_severity CHECK (severity IN ('HARD', 'MEDIUM', 'SOFT'))
);

CREATE INDEX idx_aura_clashes_version_unresolved
    ON aura_clashes (version_id)
    WHERE resolved_at IS NULL;

CREATE TABLE aura_resolution_suggestions (
    id UUID PRIMARY KEY,
    clash_id UUID NOT NULL,
    target_session_id UUID NOT NULL,
    suggested_room_id UUID,
    suggested_timeslot_id UUID,
    score_delta INTEGER NOT NULL DEFAULT 0,
    explanation VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_suggestions_clash
        FOREIGN KEY (clash_id) REFERENCES aura_clashes (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_suggestions_session
        FOREIGN KEY (target_session_id) REFERENCES aura_scheduled_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_suggestions_room
        FOREIGN KEY (suggested_room_id) REFERENCES aura_rooms (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_suggestions_timeslot
        FOREIGN KEY (suggested_timeslot_id) REFERENCES aura_timeslots (id) ON DELETE SET NULL
);

CREATE TABLE aura_manual_moves (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    previous_room_id UUID NOT NULL,
    previous_timeslot_id UUID NOT NULL,
    new_room_id UUID NOT NULL,
    new_timeslot_id UUID NOT NULL,
    reason VARCHAR(500) NOT NULL,
    moved_by_user_id UUID NOT NULL,
    moved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_moves_session
        FOREIGN KEY (session_id) REFERENCES aura_scheduled_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_moves_previous_room
        FOREIGN KEY (previous_room_id) REFERENCES aura_rooms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_moves_previous_timeslot
        FOREIGN KEY (previous_timeslot_id) REFERENCES aura_timeslots (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_moves_new_room
        FOREIGN KEY (new_room_id) REFERENCES aura_rooms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_moves_new_timeslot
        FOREIGN KEY (new_timeslot_id) REFERENCES aura_timeslots (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_moves_user
        FOREIGN KEY (moved_by_user_id) REFERENCES users (id) ON DELETE RESTRICT
);
