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
