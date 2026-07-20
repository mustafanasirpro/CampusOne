ALTER TABLE aura_constraint_configurations
    DROP CONSTRAINT chk_aura_constraint_weight,
    DROP CONSTRAINT chk_aura_constraint_profile;

ALTER TABLE aura_constraint_configurations
    ADD CONSTRAINT chk_aura_constraint_weight
        CHECK (weight BETWEEN 0 AND 1000000),
    ADD CONSTRAINT chk_aura_constraint_profile
        CHECK (profile IN (
            'FAST_FEASIBLE', 'BALANCED', 'COMPACT', 'ROOM_EFFICIENT',
            'INSTRUCTOR_FRIENDLY', 'QUALITY', 'REPAIR', 'WHAT_IF'));

ALTER TABLE aura_generation_runs
    ADD COLUMN profile VARCHAR(24) NOT NULL DEFAULT 'BALANCED',
    ADD COLUMN random_seed BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN candidate_count INTEGER,
    ADD COLUMN termination_reason VARCHAR(80),
    ADD CONSTRAINT chk_aura_run_profile
        CHECK (profile IN (
            'FAST_FEASIBLE', 'BALANCED', 'COMPACT', 'ROOM_EFFICIENT',
            'INSTRUCTOR_FRIENDLY', 'QUALITY', 'REPAIR', 'WHAT_IF')),
    ADD CONSTRAINT chk_aura_run_candidate_count
        CHECK (candidate_count IS NULL OR candidate_count >= 0);

CREATE INDEX idx_aura_constraint_term_profile_active
    ON aura_constraint_configurations (term_id, profile)
    WHERE active = TRUE;
