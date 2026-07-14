CREATE TABLE aura_section_availability (
    id UUID PRIMARY KEY,
    section_id UUID NOT NULL,
    timeslot_id UUID NOT NULL,
    availability VARCHAR(20) NOT NULL,
    reason VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_section_availability_section
        FOREIGN KEY (section_id) REFERENCES aura_sections (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_section_availability_timeslot
        FOREIGN KEY (timeslot_id) REFERENCES aura_timeslots (id) ON DELETE CASCADE,
    CONSTRAINT chk_aura_section_availability
        CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE', 'AVOID', 'PREFERRED'))
);

CREATE UNIQUE INDEX uk_aura_section_availability
    ON aura_section_availability (section_id, timeslot_id);

CREATE INDEX idx_aura_section_availability_section
    ON aura_section_availability (section_id);

CREATE INDEX idx_aura_section_availability_timeslot
    ON aura_section_availability (timeslot_id);
