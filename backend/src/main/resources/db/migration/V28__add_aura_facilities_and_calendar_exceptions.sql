CREATE TABLE aura_room_facilities (
    room_id UUID NOT NULL,
    facility VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id, facility),
    CONSTRAINT fk_aura_room_facilities_room
        FOREIGN KEY (room_id) REFERENCES aura_rooms (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_room_facility
        CHECK (facility IN (
            'PROJECTOR',
            'SMART_BOARD',
            'COMPUTERS',
            'INTERNET',
            'LAB_EQUIPMENT',
            'ACCESSIBLE',
            'AIR_CONDITIONING',
            'VIDEO_CONFERENCING',
            'SPECIALIZED_SOFTWARE',
            'OTHER'
        ))
);

CREATE INDEX idx_aura_room_facilities_facility
    ON aura_room_facilities (facility, room_id);

CREATE TABLE aura_meeting_requirement_facilities (
    meeting_requirement_id UUID NOT NULL,
    facility VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (meeting_requirement_id, facility),
    CONSTRAINT fk_aura_requirement_facilities_requirement
        FOREIGN KEY (meeting_requirement_id)
        REFERENCES aura_meeting_requirements (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_requirement_facility
        CHECK (facility IN (
            'PROJECTOR',
            'SMART_BOARD',
            'COMPUTERS',
            'INTERNET',
            'LAB_EQUIPMENT',
            'ACCESSIBLE',
            'AIR_CONDITIONING',
            'VIDEO_CONFERENCING',
            'SPECIALIZED_SOFTWARE',
            'OTHER'
        ))
);

CREATE INDEX idx_aura_requirement_facilities_facility
    ON aura_meeting_requirement_facilities (facility, meeting_requirement_id);

CREATE TABLE aura_calendar_exceptions (
    id UUID PRIMARY KEY,
    term_id UUID NOT NULL,
    exception_type VARCHAR(32) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    instructor_id UUID,
    room_id UUID,
    section_id UUID,
    timeslot_id UUID,
    facility VARCHAR(40),
    reason VARCHAR(300) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_calendar_exception_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_calendar_exception_instructor
        FOREIGN KEY (instructor_id) REFERENCES aura_instructors (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_calendar_exception_room
        FOREIGN KEY (room_id) REFERENCES aura_rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_calendar_exception_section
        FOREIGN KEY (section_id) REFERENCES aura_sections (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_calendar_exception_timeslot
        FOREIGN KEY (timeslot_id) REFERENCES aura_timeslots (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_calendar_exception_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_calendar_exception_dates
        CHECK (starts_on <= ends_on),
    CONSTRAINT chk_aura_calendar_exception_type
        CHECK (exception_type IN (
            'HOLIDAY',
            'NON_TEACHING_DAY',
            'UNIVERSITY_EVENT',
            'INSTRUCTOR_ABSENCE',
            'ROOM_CLOSURE',
            'SECTION_RESTRICTION',
            'TIMESLOT_CANCELLATION',
            'FACILITY_OUTAGE'
        )),
    CONSTRAINT chk_aura_calendar_exception_facility
        CHECK (facility IS NULL OR facility IN (
            'PROJECTOR',
            'SMART_BOARD',
            'COMPUTERS',
            'INTERNET',
            'LAB_EQUIPMENT',
            'ACCESSIBLE',
            'AIR_CONDITIONING',
            'VIDEO_CONFERENCING',
            'SPECIALIZED_SOFTWARE',
            'OTHER'
        )),
    CONSTRAINT chk_aura_calendar_exception_target
        CHECK (
            (exception_type IN ('HOLIDAY', 'NON_TEACHING_DAY', 'UNIVERSITY_EVENT')
                AND instructor_id IS NULL AND room_id IS NULL
                AND section_id IS NULL AND facility IS NULL)
            OR (exception_type = 'INSTRUCTOR_ABSENCE'
                AND instructor_id IS NOT NULL AND room_id IS NULL
                AND section_id IS NULL AND facility IS NULL)
            OR (exception_type = 'ROOM_CLOSURE'
                AND room_id IS NOT NULL AND instructor_id IS NULL
                AND section_id IS NULL AND facility IS NULL)
            OR (exception_type = 'SECTION_RESTRICTION'
                AND section_id IS NOT NULL AND instructor_id IS NULL
                AND room_id IS NULL AND facility IS NULL)
            OR (exception_type = 'TIMESLOT_CANCELLATION'
                AND timeslot_id IS NOT NULL AND instructor_id IS NULL
                AND room_id IS NULL AND section_id IS NULL AND facility IS NULL)
            OR (exception_type = 'FACILITY_OUTAGE'
                AND room_id IS NOT NULL AND facility IS NOT NULL
                AND instructor_id IS NULL AND section_id IS NULL)
        )
);

CREATE INDEX idx_aura_calendar_exceptions_term_dates
    ON aura_calendar_exceptions (term_id, starts_on, ends_on)
    WHERE active = TRUE;

CREATE INDEX idx_aura_calendar_exceptions_instructor
    ON aura_calendar_exceptions (instructor_id, starts_on, ends_on)
    WHERE active = TRUE AND instructor_id IS NOT NULL;

CREATE INDEX idx_aura_calendar_exceptions_room
    ON aura_calendar_exceptions (room_id, starts_on, ends_on)
    WHERE active = TRUE AND room_id IS NOT NULL;

CREATE INDEX idx_aura_calendar_exceptions_section
    ON aura_calendar_exceptions (section_id, starts_on, ends_on)
    WHERE active = TRUE AND section_id IS NOT NULL;
