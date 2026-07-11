CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_search_notes_title_trgm
    ON notes USING gin (LOWER(title) gin_trgm_ops)
    WHERE deleted_at IS NULL
        AND moderation_status = 'APPROVED'
        AND visibility = 'PUBLIC';

CREATE INDEX IF NOT EXISTS idx_search_notes_description_trgm
    ON notes USING gin (LOWER(description) gin_trgm_ops)
    WHERE deleted_at IS NULL
        AND moderation_status = 'APPROVED'
        AND visibility = 'PUBLIC';

CREATE INDEX IF NOT EXISTS idx_search_notes_teacher_trgm
    ON notes USING gin (LOWER(teacher_name) gin_trgm_ops)
    WHERE deleted_at IS NULL
        AND moderation_status = 'APPROVED'
        AND visibility = 'PUBLIC';

CREATE INDEX IF NOT EXISTS idx_search_notes_public_created
    ON notes (created_at DESC, id)
    WHERE deleted_at IS NULL
        AND moderation_status = 'APPROVED'
        AND visibility = 'PUBLIC';

CREATE INDEX IF NOT EXISTS idx_search_notes_public_course_created
    ON notes (course_id, created_at DESC, id)
    WHERE deleted_at IS NULL
        AND moderation_status = 'APPROVED'
        AND visibility = 'PUBLIC';

CREATE INDEX IF NOT EXISTS idx_search_note_tags_note_id
    ON note_tags (note_id);

CREATE INDEX IF NOT EXISTS idx_search_note_tags_tag_id
    ON note_tags (tag_id);

CREATE INDEX IF NOT EXISTS idx_search_file_assets_filename_trgm
    ON file_assets USING gin (LOWER(original_filename) gin_trgm_ops)
    WHERE status = 'READY';

CREATE INDEX IF NOT EXISTS idx_search_courses_code_trgm
    ON courses USING gin (LOWER(course_code) gin_trgm_ops)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_search_courses_title_trgm
    ON courses USING gin (LOWER(title) gin_trgm_ops)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_search_departments_name_trgm
    ON departments USING gin (LOWER(name) gin_trgm_ops)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_search_tags_name_trgm
    ON tags USING gin (LOWER(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_search_profiles_full_name_trgm
    ON student_profiles USING gin (LOWER(full_name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_search_marketplace_title_trgm
    ON marketplace_listings USING gin (LOWER(title) gin_trgm_ops)
    WHERE deleted_at IS NULL
        AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_search_marketplace_description_trgm
    ON marketplace_listings USING gin (LOWER(description) gin_trgm_ops)
    WHERE deleted_at IS NULL
        AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_search_marketplace_category_trgm
    ON marketplace_listings USING gin (LOWER(category) gin_trgm_ops)
    WHERE deleted_at IS NULL
        AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_search_marketplace_image_filename_trgm
    ON marketplace_listing_images USING gin (LOWER(original_filename) gin_trgm_ops)
    WHERE original_filename IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_search_discussion_title_trgm
    ON discussion_questions USING gin (LOWER(title) gin_trgm_ops)
    WHERE deleted = FALSE
        AND status IN ('OPEN', 'RESOLVED', 'CLOSED');

CREATE INDEX IF NOT EXISTS idx_search_discussion_body_trgm
    ON discussion_questions USING gin (LOWER(body) gin_trgm_ops)
    WHERE deleted = FALSE
        AND status IN ('OPEN', 'RESOLVED', 'CLOSED');

CREATE INDEX IF NOT EXISTS idx_search_discussion_category_trgm
    ON discussion_questions USING gin (LOWER(category) gin_trgm_ops)
    WHERE deleted = FALSE
        AND status IN ('OPEN', 'RESOLVED', 'CLOSED');

CREATE INDEX IF NOT EXISTS idx_search_events_title_trgm
    ON events USING gin (LOWER(title) gin_trgm_ops)
    WHERE deleted = FALSE
        AND visibility = 'PUBLIC'
        AND status IN ('UPCOMING', 'CANCELLED', 'COMPLETED');

CREATE INDEX IF NOT EXISTS idx_search_events_description_trgm
    ON events USING gin (LOWER(description) gin_trgm_ops)
    WHERE deleted = FALSE
        AND visibility = 'PUBLIC'
        AND status IN ('UPCOMING', 'CANCELLED', 'COMPLETED');

CREATE INDEX IF NOT EXISTS idx_search_events_location_trgm
    ON events USING gin (LOWER(location) gin_trgm_ops)
    WHERE deleted = FALSE
        AND visibility = 'PUBLIC'
        AND status IN ('UPCOMING', 'CANCELLED', 'COMPLETED');

CREATE INDEX IF NOT EXISTS idx_search_internships_title_trgm
    ON internships USING gin (LOWER(title) gin_trgm_ops)
    WHERE deleted = FALSE
        AND status IN ('OPEN', 'CLOSED', 'EXPIRED');

CREATE INDEX IF NOT EXISTS idx_search_internships_company_trgm
    ON internships USING gin (LOWER(company_name) gin_trgm_ops)
    WHERE deleted = FALSE
        AND status IN ('OPEN', 'CLOSED', 'EXPIRED');

CREATE INDEX IF NOT EXISTS idx_search_internships_description_trgm
    ON internships USING gin (LOWER(description) gin_trgm_ops)
    WHERE deleted = FALSE
        AND status IN ('OPEN', 'CLOSED', 'EXPIRED');

CREATE INDEX IF NOT EXISTS idx_search_internships_location_trgm
    ON internships USING gin (LOWER(location) gin_trgm_ops)
    WHERE deleted = FALSE
        AND status IN ('OPEN', 'CLOSED', 'EXPIRED');
