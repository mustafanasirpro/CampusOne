ALTER TABLE aura_generation_runs
    ADD COLUMN input_revision BIGINT NOT NULL DEFAULT 0;

UPDATE aura_generation_runs run_row
SET input_revision = term_row.data_revision
FROM aura_academic_terms term_row
WHERE term_row.id = run_row.term_id;

ALTER TABLE aura_timetable_versions
    ADD COLUMN input_revision BIGINT NOT NULL DEFAULT 0;

UPDATE aura_timetable_versions timetable_version
SET input_revision = COALESCE(
        (SELECT generation_run.input_revision
         FROM aura_generation_runs generation_run
         WHERE generation_run.id = timetable_version.generation_run_id),
        term_row.data_revision),
    revision_id = COALESCE(
        timetable_version.revision_id,
        (SELECT generation_run.revision_id
         FROM aura_generation_runs generation_run
         WHERE generation_run.id = timetable_version.generation_run_id))
FROM aura_academic_terms term_row
WHERE term_row.id = timetable_version.term_id;

ALTER TABLE aura_timetable_versions
    DROP CONSTRAINT chk_aura_versions_status;
ALTER TABLE aura_timetable_versions
    ADD CONSTRAINT chk_aura_versions_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'SUPERSEDED'));

ALTER TABLE aura_scheduled_sessions
    DROP CONSTRAINT chk_aura_sessions_source;
ALTER TABLE aura_scheduled_sessions
    ADD CONSTRAINT chk_aura_sessions_source
        CHECK (source IN ('SOLVER', 'IMPORTED', 'MANUAL', 'REPAIR',
            'EMERGENCY_REPAIR', 'WHAT_IF'));

ALTER TABLE aura_clashes
    DROP CONSTRAINT chk_aura_clashes_type;
ALTER TABLE aura_clashes
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN fingerprint VARCHAR(64),
    ADD COLUMN explanation VARCHAR(500),
    ADD COLUMN corrective_direction VARCHAR(500),
    ADD COLUMN affected_student_user_id UUID,
    ADD COLUMN resolved_by_user_id UUID,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT fk_aura_clash_student
        FOREIGN KEY (affected_student_user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_aura_clash_resolver
        FOREIGN KEY (resolved_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_aura_clash_status
        CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'DISMISSED'));

CREATE UNIQUE INDEX uk_aura_clash_fingerprint
    ON aura_clashes (version_id, fingerprint)
    WHERE fingerprint IS NOT NULL AND status IN ('OPEN', 'ACKNOWLEDGED');

CREATE OR REPLACE FUNCTION aura_increment_university_terms_revision()
RETURNS TRIGGER AS $$
DECLARE
    affected_university_id UUID;
BEGIN
    affected_university_id := COALESCE(NEW.university_id, OLD.university_id);
    UPDATE aura_academic_terms
    SET data_revision = data_revision + 1,
        updated_at = CURRENT_TIMESTAMP,
        version = version + 1
    WHERE university_id = affected_university_id
      AND status <> 'ARCHIVED';
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION aura_increment_section_revision()
RETURNS TRIGGER AS $$
DECLARE
    affected_term_id UUID;
    affected_university_id UUID;
    affected_batch_id UUID;
BEGIN
    affected_term_id := COALESCE(NEW.term_id, OLD.term_id);
    affected_batch_id := COALESCE(NEW.batch_id, OLD.batch_id);
    SELECT program.university_id INTO affected_university_id
    FROM aura_batches batch
    JOIN aura_programs program ON program.id = batch.program_id
    WHERE batch.id = affected_batch_id;
    IF affected_term_id IS NOT NULL THEN
        UPDATE aura_academic_terms
        SET data_revision = data_revision + 1,
            updated_at = CURRENT_TIMESTAMP,
            version = version + 1
        WHERE id = affected_term_id;
    ELSE
        UPDATE aura_academic_terms
        SET data_revision = data_revision + 1,
            updated_at = CURRENT_TIMESTAMP,
            version = version + 1
        WHERE university_id = affected_university_id
          AND status <> 'ARCHIVED';
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION aura_increment_offering_child_revision()
RETURNS TRIGGER AS $$
DECLARE
    affected_offering_id UUID;
    affected_term_id UUID;
BEGIN
    affected_offering_id := COALESCE(NEW.offering_id, OLD.offering_id);
    SELECT term_id INTO affected_term_id
    FROM aura_course_offerings WHERE id = affected_offering_id;
    UPDATE aura_academic_terms
    SET data_revision = data_revision + 1,
        updated_at = CURRENT_TIMESTAMP,
        version = version + 1
    WHERE id = affected_term_id;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION aura_increment_requirement_facility_revision()
RETURNS TRIGGER AS $$
DECLARE
    affected_requirement_id UUID;
    affected_term_id UUID;
BEGIN
    affected_requirement_id := COALESCE(
        NEW.meeting_requirement_id,
        OLD.meeting_requirement_id);
    SELECT offering.term_id INTO affected_term_id
    FROM aura_meeting_requirements requirement
    JOIN aura_course_offerings offering ON offering.id = requirement.offering_id
    WHERE requirement.id = affected_requirement_id;
    UPDATE aura_academic_terms
    SET data_revision = data_revision + 1,
        updated_at = CURRENT_TIMESTAMP,
        version = version + 1
    WHERE id = affected_term_id;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION aura_increment_room_facility_revision()
RETURNS TRIGGER AS $$
DECLARE
    affected_room_id UUID;
    affected_university_id UUID;
BEGIN
    affected_room_id := COALESCE(NEW.room_id, OLD.room_id);
    SELECT university_id INTO affected_university_id
    FROM aura_rooms WHERE id = affected_room_id;
    UPDATE aura_academic_terms
    SET data_revision = data_revision + 1,
        updated_at = CURRENT_TIMESTAMP,
        version = version + 1
    WHERE university_id = affected_university_id
      AND status <> 'ARCHIVED';
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_aura_rooms_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_rooms
FOR EACH ROW EXECUTE FUNCTION aura_increment_university_terms_revision();
CREATE TRIGGER trg_aura_instructors_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_instructors
FOR EACH ROW EXECUTE FUNCTION aura_increment_university_terms_revision();
CREATE TRIGGER trg_aura_timeslots_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_timeslots
FOR EACH ROW EXECUTE FUNCTION aura_increment_university_terms_revision();
CREATE TRIGGER trg_aura_sections_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_sections
FOR EACH ROW EXECUTE FUNCTION aura_increment_section_revision();
CREATE TRIGGER trg_aura_offerings_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_course_offerings
FOR EACH ROW EXECUTE FUNCTION aura_increment_term_revision();
CREATE TRIGGER trg_aura_requirements_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_meeting_requirements
FOR EACH ROW EXECUTE FUNCTION aura_increment_offering_child_revision();
CREATE TRIGGER trg_aura_offering_sections_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_offering_sections
FOR EACH ROW EXECUTE FUNCTION aura_increment_offering_child_revision();
CREATE TRIGGER trg_aura_teaching_groups_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_teaching_groups
FOR EACH ROW EXECUTE FUNCTION aura_increment_offering_child_revision();
CREATE TRIGGER trg_aura_requirement_facilities_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_meeting_requirement_facilities
FOR EACH ROW EXECUTE FUNCTION aura_increment_requirement_facility_revision();
CREATE TRIGGER trg_aura_room_facilities_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_room_facilities
FOR EACH ROW EXECUTE FUNCTION aura_increment_room_facility_revision();

CREATE OR REPLACE FUNCTION aura_increment_availability_revision()
RETURNS TRIGGER AS $$
DECLARE
    affected_university_id UUID;
BEGIN
    IF TG_TABLE_NAME = 'aura_instructor_availability' THEN
        SELECT university_id INTO affected_university_id
        FROM aura_instructors
        WHERE id = COALESCE(NEW.instructor_id, OLD.instructor_id);
    ELSIF TG_TABLE_NAME = 'aura_room_availability' THEN
        SELECT university_id INTO affected_university_id
        FROM aura_rooms
        WHERE id = COALESCE(NEW.room_id, OLD.room_id);
    ELSE
        SELECT program.university_id INTO affected_university_id
        FROM aura_sections section_row
        JOIN aura_batches batch ON batch.id = section_row.batch_id
        JOIN aura_programs program ON program.id = batch.program_id
        WHERE section_row.id = COALESCE(NEW.section_id, OLD.section_id);
    END IF;
    UPDATE aura_academic_terms
    SET data_revision = data_revision + 1,
        updated_at = CURRENT_TIMESTAMP,
        version = version + 1
    WHERE university_id = affected_university_id
      AND status <> 'ARCHIVED';
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_aura_instructor_availability_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_instructor_availability
FOR EACH ROW EXECUTE FUNCTION aura_increment_availability_revision();
CREATE TRIGGER trg_aura_room_availability_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_room_availability
FOR EACH ROW EXECUTE FUNCTION aura_increment_availability_revision();
CREATE TRIGGER trg_aura_section_availability_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_section_availability
FOR EACH ROW EXECUTE FUNCTION aura_increment_availability_revision();
