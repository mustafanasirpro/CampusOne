ALTER TABLE aura_instructor_availability
    DROP CONSTRAINT chk_aura_instructor_availability;

ALTER TABLE aura_instructor_availability
    ADD CONSTRAINT chk_aura_instructor_availability
        CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE', 'AVOID', 'PREFERRED'));

ALTER TABLE aura_room_availability
    DROP CONSTRAINT chk_aura_room_availability;

ALTER TABLE aura_room_availability
    ADD CONSTRAINT chk_aura_room_availability
        CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE', 'AVOID', 'PREFERRED'));
