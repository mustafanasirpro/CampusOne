ALTER TABLE lost_found_item_images
    ALTER COLUMN display_order TYPE INTEGER
    USING display_order::INTEGER;

ALTER TABLE lost_found_matches
    ALTER COLUMN score TYPE INTEGER
    USING score::INTEGER;
