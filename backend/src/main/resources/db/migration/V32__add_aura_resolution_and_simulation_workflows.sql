CREATE TABLE aura_resolution_cases (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    term_id UUID NOT NULL,
    student_user_id UUID NOT NULL,
    registration_id UUID,
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    case_type VARCHAR(32) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    requested_by_user_id UUID NOT NULL,
    reviewed_by_user_id UUID,
    review_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_resolution_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_resolution_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_resolution_student
        FOREIGN KEY (student_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_resolution_registration
        FOREIGN KEY (registration_id) REFERENCES aura_student_course_registrations (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_resolution_requester
        FOREIGN KEY (requested_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_resolution_reviewer
        FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_aura_resolution_status CHECK (status IN (
        'OPEN', 'ANALYZING', 'SUGGESTED', 'PENDING_APPROVAL', 'APPROVED',
        'APPLIED', 'REJECTED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_aura_resolution_type CHECK (case_type IN (
        'REPEATER_CLASH', 'ELECTIVE_CLASH', 'CROSS_SECTION_CLASH',
        'SHARED_STUDENT_CLASH', 'MANUAL_REQUEST'))
);

CREATE INDEX idx_aura_resolution_term_status
    ON aura_resolution_cases (term_id, status, created_at DESC);
CREATE INDEX idx_aura_resolution_student
    ON aura_resolution_cases (student_user_id, term_id, created_at DESC);
CREATE UNIQUE INDEX uk_aura_open_resolution_registration
    ON aura_resolution_cases (registration_id)
    WHERE registration_id IS NOT NULL
      AND status IN ('OPEN', 'ANALYZING', 'SUGGESTED', 'PENDING_APPROVAL', 'APPROVED');

CREATE TABLE aura_ranked_resolution_suggestions (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL,
    suggestion_type VARCHAR(40) NOT NULL,
    target_offering_id UUID,
    target_section_id UUID,
    target_group_id UUID,
    target_session_id UUID,
    target_room_id UUID,
    target_timeslot_id UUID,
    rank_order INTEGER NOT NULL,
    safe BOOLEAN NOT NULL,
    hard_clashes_removed INTEGER NOT NULL DEFAULT 0,
    hard_clashes_added INTEGER NOT NULL DEFAULT 0,
    affected_students INTEGER NOT NULL DEFAULT 1,
    changed_sessions INTEGER NOT NULL DEFAULT 0,
    impact JSONB NOT NULL DEFAULT '{}'::JSONB,
    explanation VARCHAR(500) NOT NULL,
    applied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_ranked_case
        FOREIGN KEY (case_id) REFERENCES aura_resolution_cases (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_ranked_offering
        FOREIGN KEY (target_offering_id) REFERENCES aura_course_offerings (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_ranked_section
        FOREIGN KEY (target_section_id) REFERENCES aura_sections (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_ranked_group
        FOREIGN KEY (target_group_id) REFERENCES aura_teaching_groups (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_ranked_session
        FOREIGN KEY (target_session_id) REFERENCES aura_scheduled_sessions (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_ranked_room
        FOREIGN KEY (target_room_id) REFERENCES aura_rooms (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_ranked_timeslot
        FOREIGN KEY (target_timeslot_id) REFERENCES aura_timeslots (id) ON DELETE SET NULL,
    CONSTRAINT chk_aura_ranked_type CHECK (suggestion_type IN (
        'PARALLEL_OFFERING_TRANSFER', 'ALTERNATE_LAB', 'ALTERNATE_TUTORIAL',
        'SECTION_TRANSFER', 'MAKEUP_SESSION', 'EQUIVALENT_OFFERING',
        'REGISTRATION_ADJUSTMENT', 'ROOM_REPLACEMENT', 'TIME_MOVE',
        'ROOM_TIME_MOVE', 'SAFE_SWAP', 'LOCALIZED_REPAIR', 'EMERGENCY_REPAIR')),
    CONSTRAINT chk_aura_ranked_order CHECK (rank_order > 0),
    CONSTRAINT chk_aura_ranked_clashes CHECK (
        hard_clashes_removed >= 0 AND hard_clashes_added >= 0
        AND affected_students >= 0 AND changed_sessions >= 0)
);

CREATE UNIQUE INDEX uk_aura_ranked_case_order
    ON aura_ranked_resolution_suggestions (case_id, rank_order);

CREATE TABLE aura_resolution_actions (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL,
    suggestion_id UUID,
    actor_user_id UUID NOT NULL,
    action VARCHAR(32) NOT NULL,
    reason VARCHAR(500),
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_resolution_action_case
        FOREIGN KEY (case_id) REFERENCES aura_resolution_cases (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_resolution_action_suggestion
        FOREIGN KEY (suggestion_id) REFERENCES aura_ranked_resolution_suggestions (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_resolution_action_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_resolution_action CHECK (action IN (
        'REQUESTED', 'ANALYZED', 'SUBMITTED', 'APPROVED', 'REJECTED',
        'APPLIED', 'FAILED', 'CANCELLED'))
);

CREATE TABLE aura_what_if_runs (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    term_id UUID NOT NULL,
    source_version_id UUID,
    requested_by_user_id UUID NOT NULL,
    scenario_type VARCHAR(40) NOT NULL,
    scenario_input JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    readiness_impact JSONB NOT NULL DEFAULT '{}'::JSONB,
    result_metrics JSONB NOT NULL DEFAULT '{}'::JSONB,
    recommendation VARCHAR(500),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_what_if_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_what_if_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_what_if_source
        FOREIGN KEY (source_version_id) REFERENCES aura_timetable_versions (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_what_if_requester
        FOREIGN KEY (requested_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_what_if_status
        CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_aura_what_if_type CHECK (scenario_type IN (
        'ROOM_UNAVAILABLE', 'INSTRUCTOR_UNAVAILABLE', 'SECTION_ADDED',
        'OFFERING_ADDED', 'ENROLLMENT_CHANGED', 'TIMESLOT_REMOVED',
        'LAB_DURATION_CHANGED', 'INSTRUCTOR_REPLACED', 'REGISTRATION_ADDED',
        'TRAVEL_RULE_CHANGED', 'EXCEPTION_ADDED', 'FACILITY_REMOVED'))
);

CREATE INDEX idx_aura_what_if_term_created
    ON aura_what_if_runs (term_id, created_at DESC);

CREATE TABLE aura_emergency_repair_requests (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    term_id UUID NOT NULL,
    source_version_id UUID NOT NULL,
    draft_version_id UUID,
    requested_by_user_id UUID NOT NULL,
    emergency_type VARCHAR(32) NOT NULL,
    affected_resource_id UUID,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING_REVIEW',
    impact JSONB NOT NULL DEFAULT '{}'::JSONB,
    reviewed_by_user_id UUID,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aura_emergency_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_emergency_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_emergency_source
        FOREIGN KEY (source_version_id) REFERENCES aura_timetable_versions (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_emergency_draft
        FOREIGN KEY (draft_version_id) REFERENCES aura_timetable_versions (id) ON DELETE SET NULL,
    CONSTRAINT fk_aura_emergency_requester
        FOREIGN KEY (requested_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_aura_emergency_reviewer
        FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_aura_emergency_type CHECK (emergency_type IN (
        'ROOM_CLOSURE', 'INSTRUCTOR_ABSENCE', 'UNIVERSITY_EVENT',
        'TIMESLOT_CANCELLATION', 'FACILITY_OUTAGE', 'SECTION_RESTRICTION')),
    CONSTRAINT chk_aura_emergency_status CHECK (status IN (
        'PENDING_REVIEW', 'ANALYZING', 'DRAFT_READY', 'APPROVED',
        'REJECTED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_aura_emergency_dates CHECK (
        starts_at IS NULL OR ends_at IS NULL OR starts_at < ends_at)
);

CREATE INDEX idx_aura_emergency_term_status
    ON aura_emergency_repair_requests (term_id, status, created_at DESC);

CREATE TABLE aura_version_comparisons (
    id UUID PRIMARY KEY,
    term_id UUID NOT NULL,
    base_version_id UUID NOT NULL,
    compared_version_id UUID NOT NULL,
    requested_by_user_id UUID NOT NULL,
    summary JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aura_comparison_term
        FOREIGN KEY (term_id) REFERENCES aura_academic_terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_comparison_base
        FOREIGN KEY (base_version_id) REFERENCES aura_timetable_versions (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_comparison_compared
        FOREIGN KEY (compared_version_id) REFERENCES aura_timetable_versions (id) ON DELETE CASCADE,
    CONSTRAINT fk_aura_comparison_requester
        FOREIGN KEY (requested_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_aura_comparison_distinct CHECK (base_version_id <> compared_version_id)
);

CREATE INDEX idx_aura_comparison_term_created
    ON aura_version_comparisons (term_id, created_at DESC);

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS chk_notifications_type;
ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_type CHECK (type IN (
        'SYSTEM', 'USER_REMINDER', 'DISCUSSION_REPLY', 'DISCUSSION_ACCEPTED',
        'MARKETPLACE_UPDATE', 'EVENT_UPDATE', 'EVENT_REMINDER',
        'INTERNSHIP_POSTED', 'NOTE_ACTIVITY', 'LOST_FOUND_UPDATE',
        'AURA_UPDATE', 'ADMIN_MESSAGE'));

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS chk_notifications_target_type;
ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_target_type CHECK (
        target_type IS NULL OR target_type IN (
            'SYSTEM', 'USER', 'NOTE', 'MARKETPLACE_LISTING',
            'DISCUSSION_QUESTION', 'DISCUSSION_ANSWER', 'EVENT', 'INTERNSHIP',
            'LOST_FOUND_ITEM', 'LOST_FOUND_CLAIM', 'LOST_FOUND_MATCH',
            'AURA_TIMETABLE', 'AURA_RESOLUTION'));
