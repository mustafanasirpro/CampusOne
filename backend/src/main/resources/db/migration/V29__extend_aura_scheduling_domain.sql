ALTER TABLE aura_academic_terms
    ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Karachi',
    ADD COLUMN data_revision BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN scheduling_policy JSONB NOT NULL DEFAULT '{}'::JSONB;

ALTER TABLE aura_programs
    ADD COLUMN number_of_semesters INTEGER NOT NULL DEFAULT 8,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_aura_program_semesters
        CHECK (number_of_semesters BETWEEN 1 AND 20);

ALTER TABLE aura_batches
    ADD COLUMN expected_graduation_year INTEGER,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_aura_batch_graduation_year
        CHECK (expected_graduation_year IS NULL
            OR expected_graduation_year BETWEEN admission_year AND 2120);

ALTER TABLE aura_sections
    ADD COLUMN term_id UUID,
    ADD COLUMN semester_number INTEGER,
    ADD COLUMN hard_daily_load INTEGER NOT NULL DEFAULT 8,
    ADD COLUMN preferred_daily_load INTEGER NOT NULL DEFAULT 6,
    ADD COLUMN home_building VARCHAR(120),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_aura_sections_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_aura_section_semester
        CHECK (semester_number IS NULL OR semester_number BETWEEN 1 AND 20),
    ADD CONSTRAINT chk_aura_section_loads
        CHECK (preferred_daily_load BETWEEN 1 AND hard_daily_load
            AND hard_daily_load BETWEEN 1 AND 24);

UPDATE aura_sections section_row
SET term_id = term_source.term_id
FROM (
    SELECT section_id, MIN(term_id::TEXT)::UUID AS term_id
    FROM aura_course_offerings
    GROUP BY section_id
    HAVING COUNT(DISTINCT term_id) = 1
) term_source
WHERE term_source.section_id = section_row.id;

CREATE INDEX idx_aura_sections_term_active
    ON aura_sections (term_id, active)
    WHERE term_id IS NOT NULL;

ALTER TABLE aura_instructors
    ADD COLUMN department_id UUID,
    ADD COLUMN employee_code VARCHAR(60),
    ADD COLUMN hard_daily_load INTEGER NOT NULL DEFAULT 8,
    ADD COLUMN preferred_weekly_load INTEGER NOT NULL DEFAULT 15,
    ADD COLUMN preferred_daily_load INTEGER NOT NULL DEFAULT 6,
    ADD COLUMN maximum_consecutive_slots INTEGER NOT NULL DEFAULT 4,
    ADD COLUMN home_building VARCHAR(120),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_aura_instructors_department
        FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_aura_instructor_daily_load
        CHECK (preferred_daily_load BETWEEN 1 AND hard_daily_load
            AND hard_daily_load BETWEEN 1 AND 24),
    ADD CONSTRAINT chk_aura_instructor_preferred_weekly_load
        CHECK (preferred_weekly_load BETWEEN 1 AND max_hours_per_week),
    ADD CONSTRAINT chk_aura_instructor_consecutive
        CHECK (maximum_consecutive_slots BETWEEN 1 AND 12);

CREATE UNIQUE INDEX uk_aura_instructors_employee_code_lower
    ON aura_instructors (university_id, LOWER(employee_code))
    WHERE employee_code IS NOT NULL;

ALTER TABLE aura_rooms
    ADD COLUMN room_code VARCHAR(60),
    ADD COLUMN display_name VARCHAR(120),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE aura_rooms SET display_name = name WHERE display_name IS NULL;
ALTER TABLE aura_rooms ALTER COLUMN display_name SET NOT NULL;

CREATE UNIQUE INDEX uk_aura_rooms_code_lower
    ON aura_rooms (university_id, LOWER(room_code))
    WHERE room_code IS NOT NULL;

ALTER TABLE aura_timeslots
    ADD COLUMN term_id UUID,
    ADD COLUMN slot_order INTEGER,
    ADD COLUMN slot_type VARCHAR(20) NOT NULL DEFAULT 'INSTRUCTIONAL',
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_aura_timeslots_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_aura_timeslot_order
        CHECK (slot_order IS NULL OR slot_order BETWEEN 1 AND 100),
    ADD CONSTRAINT chk_aura_timeslot_type
        CHECK (slot_type IN ('INSTRUCTIONAL', 'BREAK'));

CREATE INDEX idx_aura_timeslots_term_day_order
    ON aura_timeslots (term_id, day_of_week, slot_order)
    WHERE term_id IS NOT NULL AND active = TRUE;

ALTER TABLE aura_course_offerings
    ADD COLUMN offering_code VARCHAR(80),
    ADD COLUMN maximum_enrollment INTEGER,
    ADD COLUMN combined_sections BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN parallel_group VARCHAR(80),
    ADD COLUMN elective_group VARCHAR(80),
    ADD COLUMN notes VARCHAR(500),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_aura_offering_max_enrollment
        CHECK (maximum_enrollment IS NULL
            OR maximum_enrollment >= expected_students);

CREATE UNIQUE INDEX uk_aura_offerings_term_code_lower
    ON aura_course_offerings (term_id, LOWER(offering_code))
    WHERE offering_code IS NOT NULL;

CREATE TABLE aura_offering_sections (
    offering_id UUID NOT NULL,
    section_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (offering_id, section_id),
    CONSTRAINT fk_aura_offering_sections_offering
        FOREIGN KEY (offering_id) REFERENCES aura_course_offerings (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_offering_sections_section
        FOREIGN KEY (section_id) REFERENCES aura_sections (id) ON DELETE RESTRICT
);

INSERT INTO aura_offering_sections (offering_id, section_id)
SELECT id, section_id FROM aura_course_offerings
ON CONFLICT DO NOTHING;

ALTER TABLE aura_meeting_requirements
    ADD COLUMN allowed_days SMALLINT[],
    ADD COLUMN prohibited_days SMALLINT[],
    ADD COLUMN preferred_days SMALLINT[],
    ADD COLUMN preferred_start_time TIME,
    ADD COLUMN preferred_end_time TIME,
    ADD COLUMN minimum_day_separation INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN maximum_occurrences_per_day INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN same_room_preferred BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN fixed_room_id UUID,
    ADD COLUMN fixed_timeslot_id UUID,
    ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN teaching_group VARCHAR(80),
    ADD COLUMN linked_requirement_id UUID,
    ADD COLUMN lecture_before_linked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN week_pattern VARCHAR(24) NOT NULL DEFAULT 'EVERY_WEEK',
    ADD COLUMN custom_weeks SMALLINT[],
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_aura_requirement_fixed_room
        FOREIGN KEY (fixed_room_id) REFERENCES aura_rooms (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_aura_requirement_fixed_timeslot
        FOREIGN KEY (fixed_timeslot_id) REFERENCES aura_timeslots (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_aura_requirement_link
        FOREIGN KEY (linked_requirement_id)
        REFERENCES aura_meeting_requirements (id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_aura_requirement_day_separation
        CHECK (minimum_day_separation BETWEEN 0 AND 6),
    ADD CONSTRAINT chk_aura_requirement_daily_occurrences
        CHECK (maximum_occurrences_per_day BETWEEN 1 AND 6),
    ADD CONSTRAINT chk_aura_requirement_week_pattern
        CHECK (week_pattern IN ('EVERY_WEEK', 'ODD_WEEK', 'EVEN_WEEK', 'CUSTOM_WEEK_SET')),
    ADD CONSTRAINT chk_aura_requirement_custom_weeks
        CHECK ((week_pattern = 'CUSTOM_WEEK_SET' AND custom_weeks IS NOT NULL)
            OR (week_pattern <> 'CUSTOM_WEEK_SET' AND custom_weeks IS NULL));

ALTER TABLE aura_cross_offering_conflicts
    ADD COLUMN term_id UUID,
    ADD COLUMN source VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN severity VARCHAR(16) NOT NULL DEFAULT 'HARD',
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN created_by_user_id UUID,
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_aura_conflict_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_aura_conflict_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_aura_conflict_source
        CHECK (source IN ('REPEATER_REGISTRATION', 'ELECTIVE_REGISTRATION',
            'SHARED_STUDENTS', 'PROGRAM_POLICY', 'MANUAL')),
    ADD CONSTRAINT chk_aura_conflict_severity
        CHECK (severity IN ('HARD', 'MEDIUM'));

UPDATE aura_cross_offering_conflicts conflict_row
SET term_id = offering.term_id
FROM aura_course_offerings offering
WHERE offering.id = conflict_row.left_offering_id
  AND conflict_row.term_id IS NULL;

CREATE INDEX idx_aura_conflicts_term_active
    ON aura_cross_offering_conflicts (term_id, severity)
    WHERE active = TRUE;

CREATE TABLE aura_teaching_groups (
    id UUID PRIMARY KEY,
    offering_id UUID NOT NULL,
    group_type VARCHAR(20) NOT NULL,
    code VARCHAR(80) NOT NULL,
    display_name VARCHAR(140) NOT NULL,
    capacity INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_teaching_group_offering
        FOREIGN KEY (offering_id) REFERENCES aura_course_offerings (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_teaching_group_type
        CHECK (group_type IN ('LECTURE', 'LAB', 'TUTORIAL')),
    CONSTRAINT chk_aura_teaching_group_capacity
        CHECK (capacity IS NULL OR capacity > 0)
);

CREATE UNIQUE INDEX uk_aura_teaching_group_code_lower
    ON aura_teaching_groups (offering_id, group_type, LOWER(code));

CREATE TABLE aura_student_course_registrations (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    term_id UUID NOT NULL,
    student_user_id UUID NOT NULL,
    offering_id UUID NOT NULL,
    registration_type VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    home_section_id UUID,
    teaching_section_id UUID,
    lecture_group_id UUID,
    lab_group_id UUID,
    tutorial_group_id UUID,
    equivalent_offering_id UUID,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_registration_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_registration_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_registration_student
        FOREIGN KEY (student_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_registration_offering
        FOREIGN KEY (offering_id) REFERENCES aura_course_offerings (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_registration_home_section
        FOREIGN KEY (home_section_id) REFERENCES aura_sections (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_registration_teaching_section
        FOREIGN KEY (teaching_section_id) REFERENCES aura_sections (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_registration_lecture_group
        FOREIGN KEY (lecture_group_id) REFERENCES aura_teaching_groups (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_registration_lab_group
        FOREIGN KEY (lab_group_id) REFERENCES aura_teaching_groups (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_registration_tutorial_group
        FOREIGN KEY (tutorial_group_id) REFERENCES aura_teaching_groups (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_registration_equivalent
        FOREIGN KEY (equivalent_offering_id) REFERENCES aura_course_offerings (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_registration_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_registration_type
        CHECK (registration_type IN ('PRIMARY_SECTION', 'REPEATER', 'ELECTIVE',
            'CROSS_SECTION', 'IMPROVEMENT', 'MAKEUP', 'MANUAL', 'TRANSFERRED')),
    CONSTRAINT chk_aura_registration_status
        CHECK (status IN ('ACTIVE', 'PENDING', 'DROPPED', 'COMPLETED', 'REJECTED'))
);

CREATE UNIQUE INDEX uk_aura_active_student_registration
    ON aura_student_course_registrations (term_id, student_user_id, offering_id)
    WHERE status IN ('ACTIVE', 'PENDING');
CREATE INDEX idx_aura_registrations_student_term
    ON aura_student_course_registrations (student_user_id, term_id, status);
CREATE INDEX idx_aura_registrations_offering_status
    ON aura_student_course_registrations (offering_id, status);

CREATE TABLE aura_student_availability (
    id UUID PRIMARY KEY,
    term_id UUID NOT NULL,
    student_user_id UUID NOT NULL,
    timeslot_id UUID NOT NULL,
    availability VARCHAR(20) NOT NULL,
    reason VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_student_availability_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_student_availability_user
        FOREIGN KEY (student_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_student_availability_timeslot
        FOREIGN KEY (timeslot_id) REFERENCES aura_timeslots (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_student_availability
        CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE', 'AVOID', 'PREFERRED'))
);

CREATE UNIQUE INDEX uk_aura_student_availability
    ON aura_student_availability (term_id, student_user_id, timeslot_id);

CREATE TABLE aura_building_travel_times (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    from_building VARCHAR(120) NOT NULL,
    to_building VARCHAR(120) NOT NULL,
    minutes INTEGER NOT NULL,
    difficulty VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_travel_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_travel_buildings
        CHECK (LOWER(BTRIM(from_building)) <> LOWER(BTRIM(to_building))),
    CONSTRAINT chk_aura_travel_minutes CHECK (minutes BETWEEN 0 AND 240),
    CONSTRAINT chk_aura_travel_difficulty
        CHECK (difficulty IN ('NORMAL', 'DIFFICULT', 'IMPOSSIBLE'))
);

CREATE UNIQUE INDEX uk_aura_travel_pair_lower
    ON aura_building_travel_times (
        university_id,
        LEAST(LOWER(BTRIM(from_building)), LOWER(BTRIM(to_building))),
        GREATEST(LOWER(BTRIM(from_building)), LOWER(BTRIM(to_building)))
    ) WHERE active = TRUE;

CREATE TABLE aura_constraint_configurations (
    id UUID PRIMARY KEY,
    term_id UUID NOT NULL,
    profile VARCHAR(24) NOT NULL,
    constraint_name VARCHAR(100) NOT NULL,
    constraint_level VARCHAR(16) NOT NULL,
    weight BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_constraint_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_constraint_profile
        CHECK (profile IN ('FAST_FEASIBLE', 'BALANCED', 'QUALITY', 'REPAIR', 'WHAT_IF')),
    CONSTRAINT chk_aura_constraint_level
        CHECK (constraint_level IN ('HARD', 'MEDIUM', 'SOFT')),
    CONSTRAINT chk_aura_constraint_weight
        CHECK (weight > 0)
);

CREATE UNIQUE INDEX uk_aura_constraint_profile_name
    ON aura_constraint_configurations (term_id, profile, LOWER(constraint_name));

ALTER TABLE aura_timetable_versions
    ADD COLUMN revision_id UUID,
    ADD COLUMN parent_version_id UUID,
    ADD COLUMN source VARCHAR(32) NOT NULL DEFAULT 'GENERATED',
    ADD COLUMN metrics JSONB NOT NULL DEFAULT '{}'::JSONB,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_aura_version_revision
        FOREIGN KEY (revision_id) REFERENCES aura_scheduling_data_revisions (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aura_version_parent
        FOREIGN KEY (parent_version_id) REFERENCES aura_timetable_versions (id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_aura_version_source
        CHECK (source IN ('GENERATED', 'IMPORTED', 'MANUAL', 'REPAIRED',
            'EMERGENCY_REPAIR', 'WHAT_IF'));

ALTER TABLE aura_scheduled_sessions
    ADD COLUMN occurrence_index INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN lock_reason VARCHAR(300),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_aura_session_occurrence CHECK (occurrence_index > 0);

-- Existing generated versions can contain several weekly occurrences for the
-- same meeting requirement. Number those rows deterministically before adding
-- the uniqueness guarantee.
WITH ranked_occurrences AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY version_id, meeting_requirement_id
               ORDER BY created_at, id
           )::INTEGER AS occurrence_index
    FROM aura_scheduled_sessions
)
UPDATE aura_scheduled_sessions session_row
SET occurrence_index = ranked_occurrences.occurrence_index
FROM ranked_occurrences
WHERE ranked_occurrences.id = session_row.id;

CREATE UNIQUE INDEX uk_aura_session_occurrence
    ON aura_scheduled_sessions (version_id, meeting_requirement_id, occurrence_index);

CREATE TABLE aura_audit_events (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    term_id UUID,
    actor_user_id UUID NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_id UUID,
    summary VARCHAR(500) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_audit_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_audit_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_audit_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_aura_audit_term_created
    ON aura_audit_events (term_id, created_at DESC);

CREATE OR REPLACE FUNCTION aura_increment_term_revision()
RETURNS TRIGGER AS $$
DECLARE
    affected_term_id UUID;
BEGIN
    affected_term_id := COALESCE(NEW.term_id, OLD.term_id);
    IF affected_term_id IS NOT NULL THEN
        UPDATE aura_academic_terms
        SET data_revision = data_revision + 1,
            updated_at = CURRENT_TIMESTAMP,
            version = version + 1
        WHERE id = affected_term_id;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_aura_calendar_exception_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_calendar_exceptions
FOR EACH ROW EXECUTE FUNCTION aura_increment_term_revision();

CREATE TRIGGER trg_aura_registration_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_student_course_registrations
FOR EACH ROW EXECUTE FUNCTION aura_increment_term_revision();

CREATE TRIGGER trg_aura_student_availability_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_student_availability
FOR EACH ROW EXECUTE FUNCTION aura_increment_term_revision();

CREATE TRIGGER trg_aura_constraint_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_constraint_configurations
FOR EACH ROW EXECUTE FUNCTION aura_increment_term_revision();
