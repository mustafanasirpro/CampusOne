CREATE OR REPLACE FUNCTION aura_recurring_calendar_exception_applies(
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
          AND exception_row.recurrence_pattern = 'WEEKLY'
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
          AND EXTRACT(ISODOW FROM exception_row.starts_on) = requested_day
    );
$$ LANGUAGE SQL STABLE;

CREATE OR REPLACE FUNCTION aura_calendar_exception_applies_on_date(
    requested_term_id UUID,
    requested_instructor_id UUID,
    requested_room_id UUID,
    requested_section_id UUID,
    requested_timeslot_id UUID,
    requested_date DATE,
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
          AND requested_date BETWEEN exception_row.starts_on AND exception_row.ends_on
          AND (exception_row.recurrence_pattern = 'NONE'
            OR (exception_row.recurrence_pattern = 'WEEKLY'
              AND EXTRACT(ISODOW FROM requested_date) =
                EXTRACT(ISODOW FROM exception_row.starts_on)))
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
            OR (exception_slot.day_of_week = EXTRACT(ISODOW FROM requested_date)
                AND exception_slot.starts_at < requested_end
                AND exception_slot.ends_at > requested_start))
          AND (exception_row.facility IS NULL OR EXISTS (
            SELECT 1 FROM aura_room_facilities room_facility
            WHERE room_facility.room_id = requested_room_id
              AND room_facility.facility = exception_row.facility))
          AND (exception_row.starts_at IS NULL
            OR (exception_row.starts_at < requested_end
                AND exception_row.ends_at > requested_start))
    );
$$ LANGUAGE SQL STABLE;
