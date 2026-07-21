CREATE TABLE aura_buildings (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    minimum_transition_minutes INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_buildings_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_building_transition
        CHECK (minimum_transition_minutes BETWEEN 0 AND 240)
);

CREATE UNIQUE INDEX uk_aura_buildings_code_lower
    ON aura_buildings (university_id, LOWER(code));
CREATE UNIQUE INDEX uk_aura_buildings_name_lower
    ON aura_buildings (university_id, LOWER(name));

INSERT INTO aura_buildings (
    id, university_id, code, name
)
SELECT gen_random_uuid(), source.university_id,
       LEFT(
           COALESCE(NULLIF(REGEXP_REPLACE(UPPER(source.building), '[^A-Z0-9]+', '-', 'g'), ''), 'BUILDING')
           || '-' || LEFT(MD5(LOWER(source.building)), 8),
           40
       ),
       source.building
FROM (
    SELECT DISTINCT university_id, BTRIM(building) AS building
    FROM aura_rooms
    WHERE building IS NOT NULL AND BTRIM(building) <> ''
) source
ON CONFLICT DO NOTHING;

ALTER TABLE aura_rooms
    ADD COLUMN building_id UUID,
    ADD CONSTRAINT fk_aura_rooms_building
        FOREIGN KEY (building_id) REFERENCES aura_buildings (id) ON DELETE RESTRICT;
ALTER TABLE aura_instructors
    ADD COLUMN home_building_id UUID,
    ADD CONSTRAINT fk_aura_instructors_home_building
        FOREIGN KEY (home_building_id) REFERENCES aura_buildings (id) ON DELETE SET NULL;
ALTER TABLE aura_sections
    ADD COLUMN home_building_id UUID,
    ADD CONSTRAINT fk_aura_sections_home_building
        FOREIGN KEY (home_building_id) REFERENCES aura_buildings (id) ON DELETE SET NULL;

UPDATE aura_rooms room
SET building_id = building.id
FROM aura_buildings building
WHERE building.university_id = room.university_id
  AND LOWER(building.name) = LOWER(BTRIM(room.building));

UPDATE aura_instructors instructor
SET home_building_id = building.id
FROM aura_buildings building
WHERE building.university_id = instructor.university_id
  AND LOWER(building.name) = LOWER(BTRIM(instructor.home_building));

UPDATE aura_sections section_row
SET home_building_id = building.id
FROM aura_batches batch
JOIN aura_programs program ON program.id = batch.program_id
JOIN aura_buildings building ON building.university_id = program.university_id
WHERE batch.id = section_row.batch_id
  AND LOWER(building.name) = LOWER(BTRIM(section_row.home_building));

ALTER TABLE aura_meeting_requirements
    ADD COLUMN teaching_group_id UUID,
    ADD CONSTRAINT fk_aura_requirement_teaching_group
        FOREIGN KEY (teaching_group_id) REFERENCES aura_teaching_groups (id) ON DELETE SET NULL;

UPDATE aura_meeting_requirements requirement
SET teaching_group_id = teaching_group.id
FROM aura_teaching_groups teaching_group
WHERE teaching_group.offering_id = requirement.offering_id
  AND teaching_group.group_type = requirement.meeting_type
  AND LOWER(teaching_group.code) = LOWER(BTRIM(requirement.teaching_group));

ALTER TABLE aura_instructor_availability
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE aura_room_availability
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE aura_section_availability
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE aura_calendar_exceptions
    DROP CONSTRAINT chk_aura_calendar_exception_type,
    DROP CONSTRAINT chk_aura_calendar_exception_target,
    ADD COLUMN department_id UUID,
    ADD COLUMN building_id UUID,
    ADD COLUMN starts_at TIME,
    ADD COLUMN ends_at TIME,
    ADD COLUMN recurrence_pattern VARCHAR(20) NOT NULL DEFAULT 'NONE',
    ADD CONSTRAINT fk_aura_calendar_exception_department
        FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_aura_calendar_exception_building
        FOREIGN KEY (building_id) REFERENCES aura_buildings (id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_aura_calendar_exception_time
        CHECK ((starts_at IS NULL AND ends_at IS NULL)
            OR (starts_at IS NOT NULL AND ends_at IS NOT NULL AND starts_at < ends_at)),
    ADD CONSTRAINT chk_aura_calendar_exception_recurrence
        CHECK (recurrence_pattern IN ('NONE', 'WEEKLY')),
    ADD CONSTRAINT chk_aura_calendar_exception_target CHECK (
        NUM_NONNULLS(instructor_id, room_id, section_id, timeslot_id,
            department_id, building_id) <= 1
    ),
    ADD CONSTRAINT chk_aura_calendar_exception_type CHECK (exception_type IN (
        'HOLIDAY', 'NON_TEACHING_DAY', 'UNIVERSITY_EVENT',
        'UNIVERSITY_CLOSURE', 'CAMPUS_CLOSURE', 'DEPARTMENT_CLOSURE',
        'EXAMINATION_PERIOD', 'INSTRUCTOR_ABSENCE', 'ROOM_CLOSURE',
        'BUILDING_CLOSURE', 'SECTION_RESTRICTION', 'SECTION_SUSPENSION',
        'TIMESLOT_CANCELLATION', 'FACILITY_OUTAGE',
        'EXCEPTIONAL_AVAILABLE_DATE', 'EXCEPTIONAL_UNAVAILABLE_DATE'
    ));

CREATE INDEX idx_aura_calendar_exceptions_department
    ON aura_calendar_exceptions (department_id, starts_on, ends_on)
    WHERE active = TRUE AND department_id IS NOT NULL;
CREATE INDEX idx_aura_calendar_exceptions_building
    ON aura_calendar_exceptions (building_id, starts_on, ends_on)
    WHERE active = TRUE AND building_id IS NOT NULL;

CREATE OR REPLACE FUNCTION aura_calendar_exception_applies(
    requested_term_id UUID,
    requested_instructor_id UUID,
    requested_room_id UUID,
    requested_section_id UUID,
    requested_timeslot_id UUID,
    requested_day SMALLINT,
    requested_start TIME,
    requested_end TIME,
    available_only BOOLEAN DEFAULT FALSE,
    requested_available_scope VARCHAR DEFAULT NULL
) RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1
        FROM aura_calendar_exceptions exception_row
        LEFT JOIN aura_instructors instructor
          ON instructor.id = requested_instructor_id
        LEFT JOIN aura_rooms room ON room.id = requested_room_id
        LEFT JOIN aura_sections section_row ON section_row.id = requested_section_id
        LEFT JOIN aura_batches batch ON batch.id = section_row.batch_id
        LEFT JOIN aura_programs program ON program.id = batch.program_id
        LEFT JOIN aura_timeslots exception_slot
          ON exception_slot.id = exception_row.timeslot_id
        WHERE exception_row.term_id = requested_term_id
          AND exception_row.active = TRUE
          AND ((available_only
                AND exception_row.exception_type = 'EXCEPTIONAL_AVAILABLE_DATE')
            OR (NOT available_only
                AND exception_row.exception_type <> 'EXCEPTIONAL_AVAILABLE_DATE'))
          AND (NOT available_only OR requested_available_scope IS NULL OR
            CASE requested_available_scope
              WHEN 'INSTRUCTOR' THEN exception_row.instructor_id = requested_instructor_id
                OR NUM_NONNULLS(exception_row.instructor_id, exception_row.room_id,
                    exception_row.section_id, exception_row.timeslot_id,
                    exception_row.department_id, exception_row.building_id) = 0
              WHEN 'ROOM' THEN exception_row.room_id = requested_room_id
                OR NUM_NONNULLS(exception_row.instructor_id, exception_row.room_id,
                    exception_row.section_id, exception_row.timeslot_id,
                    exception_row.department_id, exception_row.building_id) = 0
              WHEN 'SECTION' THEN exception_row.section_id = requested_section_id
                OR NUM_NONNULLS(exception_row.instructor_id, exception_row.room_id,
                    exception_row.section_id, exception_row.timeslot_id,
                    exception_row.department_id, exception_row.building_id) = 0
              ELSE NUM_NONNULLS(exception_row.instructor_id, exception_row.room_id,
                    exception_row.section_id, exception_row.timeslot_id,
                    exception_row.department_id, exception_row.building_id) = 0
            END)
          AND (exception_row.instructor_id IS NULL
            OR exception_row.instructor_id = requested_instructor_id)
          AND (exception_row.room_id IS NULL
            OR exception_row.room_id = requested_room_id)
          AND (exception_row.section_id IS NULL
            OR exception_row.section_id = requested_section_id)
          AND (exception_row.department_id IS NULL
            OR exception_row.department_id = instructor.department_id
            OR exception_row.department_id = program.department_id)
          AND (exception_row.building_id IS NULL
            OR exception_row.building_id = room.building_id)
          AND (exception_row.timeslot_id IS NULL
            OR exception_row.timeslot_id = requested_timeslot_id
            OR (exception_slot.day_of_week = requested_day
                AND exception_slot.starts_at < requested_end
                AND exception_slot.ends_at > requested_start))
          AND (exception_row.facility IS NULL OR EXISTS (
            SELECT 1 FROM aura_room_facilities room_facility
            WHERE room_facility.room_id = requested_room_id
              AND room_facility.facility = exception_row.facility))
          AND (exception_row.starts_at IS NULL
            OR (exception_row.starts_at < requested_end
                AND exception_row.ends_at > requested_start))
          AND EXISTS (
            SELECT 1
            FROM GENERATE_SERIES(
                exception_row.starts_on,
                exception_row.ends_on,
                INTERVAL '1 day') AS affected_date
            WHERE EXTRACT(ISODOW FROM affected_date) = requested_day)
    );
$$ LANGUAGE SQL STABLE;

ALTER TABLE aura_clashes
    ADD COLUMN reason_code VARCHAR(80),
    ADD COLUMN affected_sessions JSONB NOT NULL DEFAULT '[]'::JSONB,
    ADD COLUMN affected_students JSONB NOT NULL DEFAULT '[]'::JSONB,
    ADD COLUMN affected_instructors JSONB NOT NULL DEFAULT '[]'::JSONB,
    ADD COLUMN affected_sections JSONB NOT NULL DEFAULT '[]'::JSONB,
    ADD COLUMN affected_rooms JSONB NOT NULL DEFAULT '[]'::JSONB,
    ADD COLUMN occurrence_context JSONB NOT NULL DEFAULT '{}'::JSONB,
    ADD COLUMN suggested_action VARCHAR(500);

CREATE TABLE aura_repair_plans (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    term_id UUID NOT NULL,
    source_version_id UUID NOT NULL,
    draft_version_id UUID NOT NULL,
    requested_by_user_id UUID NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    trigger_id UUID,
    status VARCHAR(24) NOT NULL DEFAULT 'PREVIEWED',
    input_revision BIGINT NOT NULL,
    source_version_revision BIGINT NOT NULL,
    proposed_moves JSONB NOT NULL DEFAULT '[]'::JSONB,
    impact JSONB NOT NULL DEFAULT '{}'::JSONB,
    preview_hash VARCHAR(64) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    applied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_repair_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_repair_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_repair_source
        FOREIGN KEY (source_version_id) REFERENCES aura_timetable_versions (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_repair_draft
        FOREIGN KEY (draft_version_id) REFERENCES aura_timetable_versions (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_repair_requester
        FOREIGN KEY (requested_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_repair_distinct_versions
        CHECK (source_version_id <> draft_version_id),
    CONSTRAINT chk_aura_repair_trigger
        CHECK (trigger_type IN ('CLASH', 'SESSION', 'EMERGENCY')),
    CONSTRAINT chk_aura_repair_status
        CHECK (status IN ('PREVIEWED', 'APPLIED', 'EXPIRED', 'REJECTED', 'FAILED')),
    CONSTRAINT chk_aura_repair_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_aura_repair_term_created
    ON aura_repair_plans (term_id, created_at DESC);
CREATE UNIQUE INDEX uk_aura_active_repair_preview
    ON aura_repair_plans (
        draft_version_id,
        trigger_type,
        COALESCE(trigger_id, '00000000-0000-0000-0000-000000000000'::UUID)
    )
    WHERE status = 'PREVIEWED';

ALTER TABLE aura_audit_events
    ADD COLUMN correlation_id UUID,
    ADD COLUMN result VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
    ADD CONSTRAINT chk_aura_audit_result
        CHECK (result IN ('SUCCESS', 'FAILURE', 'REJECTED'));

CREATE INDEX idx_aura_audit_university_created
    ON aura_audit_events (university_id, created_at DESC);
CREATE INDEX idx_aura_audit_action_created
    ON aura_audit_events (university_id, action, created_at DESC);

CREATE TRIGGER trg_aura_buildings_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_buildings
FOR EACH ROW EXECUTE FUNCTION aura_increment_university_terms_revision();
