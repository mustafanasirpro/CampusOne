ALTER TABLE lost_found_item_images
    ALTER COLUMN checksum_sha256 TYPE VARCHAR(64)
    USING BTRIM(checksum_sha256)::VARCHAR(64);
