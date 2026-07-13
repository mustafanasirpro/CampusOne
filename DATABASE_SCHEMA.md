# CampusOne Database Schema Notes

Flyway manages CampusOne schema changes. Hibernate validation is expected to remain enabled.

## Lost & Found Migration

Migration:

```text
backend/src/main/resources/db/migration/V21__create_lost_found_module.sql
```

Tables:

| Table | Purpose |
|---|---|
| `lost_found_items` | Unified lost/found item reports. |
| `lost_found_item_images` | R2/S3-compatible image object metadata. |
| `lost_found_claims` | Private ownership claims for found items. |
| `lost_found_matches` | Deterministic suggested lost/found matches. |

## Key Constraints

- `lost_found_items.item_type` is constrained to `LOST` or `FOUND`.
- `lost_found_items.status` supports `PENDING_REVIEW`, `PUBLISHED`, `CLAIM_IN_PROGRESS`, `RESOLVED`, `CLOSED`, `REJECTED`, `ARCHIVED`, `DELETED`.
- `lost_found_items.university_id` is immutable at the entity layer and must be derived from the reporter profile.
- `lost_found_item_images` enforces allowed image MIME types and display order `0..2`.
- `lost_found_claims` enforces claim status values and handover timestamp consistency.
- `lost_found_matches` enforces score range `0..100`, distinct item IDs, and a unique lost/found pair.

## Indexes

Lost & Found adds indexes for:

- same-university public browse;
- pending moderation queue;
- owner submissions;
- candidate matching by university, type, status, category, and date;
- expiry filtering;
- claim lookup by item/claimant/status;
- match lookup by lost/found item and score;
- trigram-backed search over title, description, and location.

## Existing Check Constraint Updates

The migration extends existing check constraints for:

- `content_reports.target_type` with `LOST_FOUND_ITEM`;
- `moderation_actions.target_type` with `LOST_FOUND_ITEM`;
- `notifications.type` with `LOST_FOUND_UPDATE`;
- `notifications.target_type` with `LOST_FOUND_ITEM`, `LOST_FOUND_CLAIM`, and `LOST_FOUND_MATCH`.

## Operational Notes

- Images are never stored as binary data in PostgreSQL.
- Soft deletion is represented through `deleted_at` and status `DELETED`.
- Expired items are hidden by active browse queries even before a scheduler archives them.
- Do not edit applied Flyway migrations; create a new migration for future schema changes.
