ALTER TABLE aura_ranked_resolution_suggestions
    ADD COLUMN target_lecture_group_id UUID,
    ADD COLUMN target_lab_group_id UUID,
    ADD COLUMN target_tutorial_group_id UUID;

ALTER TABLE aura_ranked_resolution_suggestions
    ADD CONSTRAINT fk_aura_ranked_lecture_group
        FOREIGN KEY (target_lecture_group_id)
        REFERENCES aura_teaching_groups (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aura_ranked_lab_group
        FOREIGN KEY (target_lab_group_id)
        REFERENCES aura_teaching_groups (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aura_ranked_tutorial_group
        FOREIGN KEY (target_tutorial_group_id)
        REFERENCES aura_teaching_groups (id) ON DELETE SET NULL;

ALTER TABLE aura_timetable_versions
    DROP CONSTRAINT chk_aura_version_source;

ALTER TABLE aura_timetable_versions
    ADD CONSTRAINT chk_aura_version_source
        CHECK (source IN (
            'GENERATED', 'IMPORTED', 'MANUAL', 'REPAIRED',
            'EMERGENCY_REPAIR', 'WHAT_IF', 'ROLLBACK'));
