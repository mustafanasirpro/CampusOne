CREATE OR REPLACE FUNCTION aura_increment_batch_revision()
RETURNS TRIGGER AS $$
DECLARE
    affected_program_id UUID;
    affected_university_id UUID;
BEGIN
    affected_program_id := COALESCE(NEW.program_id, OLD.program_id);
    SELECT university_id INTO affected_university_id
    FROM aura_programs
    WHERE id = affected_program_id;

    UPDATE aura_academic_terms
    SET data_revision = data_revision + 1,
        updated_at = CURRENT_TIMESTAMP,
        version = version + 1
    WHERE university_id = affected_university_id
      AND status <> 'ARCHIVED';
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_aura_programs_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_programs
FOR EACH ROW EXECUTE FUNCTION aura_increment_university_terms_revision();

CREATE TRIGGER trg_aura_batches_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_batches
FOR EACH ROW EXECUTE FUNCTION aura_increment_batch_revision();

CREATE TRIGGER trg_aura_cross_offering_conflicts_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_cross_offering_conflicts
FOR EACH ROW EXECUTE FUNCTION aura_increment_term_revision();

CREATE TRIGGER trg_aura_building_travel_times_revision
AFTER INSERT OR UPDATE OR DELETE ON aura_building_travel_times
FOR EACH ROW EXECUTE FUNCTION aura_increment_university_terms_revision();

WITH ranked_active_runs AS (
    SELECT id,
        ROW_NUMBER() OVER (
            PARTITION BY term_id
            ORDER BY created_at DESC, id DESC) AS active_rank
    FROM aura_generation_runs
    WHERE status IN ('QUEUED', 'RUNNING')
)
UPDATE aura_generation_runs generation_run
SET status = 'FAILED',
    completed_at = CURRENT_TIMESTAMP,
    message = 'Superseded while enforcing one active generation run per term.'
FROM ranked_active_runs ranked
WHERE generation_run.id = ranked.id
  AND ranked.active_rank > 1;

CREATE UNIQUE INDEX uk_aura_generation_runs_active_term
    ON aura_generation_runs (term_id)
    WHERE status IN ('QUEUED', 'RUNNING');
