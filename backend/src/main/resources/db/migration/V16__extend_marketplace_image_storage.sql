ALTER TABLE marketplace_listing_images
    ADD COLUMN storage_provider VARCHAR(20),
    ADD COLUMN bucket_name VARCHAR(100),
    ADD COLUMN object_key VARCHAR(1024),
    ADD COLUMN original_filename VARCHAR(255),
    ADD COLUMN mime_type VARCHAR(127),
    ADD COLUMN size_bytes BIGINT;

ALTER TABLE marketplace_listing_images
    ALTER COLUMN image_url DROP NOT NULL;

ALTER TABLE marketplace_listing_images
    DROP CONSTRAINT chk_marketplace_listing_images_url;

ALTER TABLE marketplace_listing_images
    ADD CONSTRAINT chk_marketplace_listing_images_url CHECK (
        image_url IS NULL
        OR (
            char_length(btrim(image_url)) BETWEEN 8 AND 2048
            AND image_url ~* '^https?://'
        )
    );

ALTER TABLE marketplace_listing_images
    ADD CONSTRAINT chk_marketplace_listing_images_storage_provider CHECK (
        storage_provider IS NULL
        OR storage_provider IN ('LOCAL', 'MINIO', 'S3', 'S3_COMPATIBLE')
    );

ALTER TABLE marketplace_listing_images
    ADD CONSTRAINT chk_marketplace_listing_images_storage_metadata CHECK (
        (
            image_url IS NOT NULL
            AND storage_provider IS NULL
            AND bucket_name IS NULL
            AND object_key IS NULL
            AND original_filename IS NULL
            AND mime_type IS NULL
            AND size_bytes IS NULL
        )
        OR (
            image_url IS NOT NULL
            AND storage_provider IS NOT NULL
            AND bucket_name IS NOT NULL
            AND object_key IS NOT NULL
            AND original_filename IS NOT NULL
            AND mime_type IS NOT NULL
            AND size_bytes IS NOT NULL
            AND char_length(btrim(bucket_name)) BETWEEN 3 AND 100
            AND char_length(btrim(object_key)) BETWEEN 1 AND 1024
            AND char_length(btrim(original_filename)) BETWEEN 1 AND 255
            AND char_length(btrim(mime_type)) BETWEEN 3 AND 127
            AND position('/' IN mime_type) > 1
            AND size_bytes > 0
        )
    );

CREATE UNIQUE INDEX uk_marketplace_listing_images_storage_object
    ON marketplace_listing_images (storage_provider, bucket_name, object_key)
    WHERE object_key IS NOT NULL;

CREATE INDEX idx_marketplace_listing_images_object_key
    ON marketplace_listing_images (object_key)
    WHERE object_key IS NOT NULL;
