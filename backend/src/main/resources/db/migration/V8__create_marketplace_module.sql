CREATE TABLE marketplace_listings (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(32) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'PKR',
    item_condition VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_marketplace_listings_seller
        FOREIGN KEY (seller_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_marketplace_listings_title
        CHECK (CHAR_LENGTH(BTRIM(title)) BETWEEN 5 AND 160),
    CONSTRAINT chk_marketplace_listings_description
        CHECK (CHAR_LENGTH(BTRIM(description)) BETWEEN 10 AND 5000),
    CONSTRAINT chk_marketplace_listings_category CHECK (
        category IN (
            'BOOKS',
            'ELECTRONICS',
            'CALCULATORS',
            'HOSTEL_ITEMS',
            'FURNITURE',
            'BIKES',
            'ACCESSORIES',
            'OTHER'
        )
    ),
    CONSTRAINT chk_marketplace_listings_price
        CHECK (price > 0 AND price <= 10000000.00),
    CONSTRAINT chk_marketplace_listings_currency
        CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_marketplace_listings_condition CHECK (
        item_condition IN ('NEW', 'LIKE_NEW', 'USED', 'FAIR')
    ),
    CONSTRAINT chk_marketplace_listings_status
        CHECK (status IN ('ACTIVE', 'SOLD', 'DELETED')),
    CONSTRAINT chk_marketplace_listings_deletion CHECK (
        (status = 'DELETED' AND deleted_at IS NOT NULL)
        OR (status <> 'DELETED' AND deleted_at IS NULL)
    ),
    CONSTRAINT chk_marketplace_listings_timestamps CHECK (
        updated_at >= created_at
        AND (deleted_at IS NULL OR deleted_at >= created_at)
    ),
    CONSTRAINT chk_marketplace_listings_version
        CHECK (version >= 0)
);

CREATE INDEX idx_marketplace_listings_active_newest
    ON marketplace_listings (created_at DESC, id)
    WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX idx_marketplace_listings_active_category
    ON marketplace_listings (category, created_at DESC, id)
    WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX idx_marketplace_listings_seller_created_at
    ON marketplace_listings (seller_id, created_at DESC, id)
    WHERE deleted_at IS NULL;

CREATE TABLE marketplace_listing_images (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    alt_text VARCHAR(160),
    display_order SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_marketplace_listing_images_listing
        FOREIGN KEY (listing_id)
        REFERENCES marketplace_listings (id) ON DELETE CASCADE,
    CONSTRAINT uk_marketplace_listing_images_order
        UNIQUE (listing_id, display_order),
    CONSTRAINT uk_marketplace_listing_images_url
        UNIQUE (listing_id, image_url),
    CONSTRAINT chk_marketplace_listing_images_url
        CHECK (
            CHAR_LENGTH(BTRIM(image_url)) BETWEEN 8 AND 2048
            AND image_url ~* '^https?://'
        ),
    CONSTRAINT chk_marketplace_listing_images_alt_text
        CHECK (
            alt_text IS NULL
            OR CHAR_LENGTH(BTRIM(alt_text)) BETWEEN 1 AND 160
        ),
    CONSTRAINT chk_marketplace_listing_images_order
        CHECK (display_order BETWEEN 0 AND 5)
);
