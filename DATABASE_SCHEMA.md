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

## AURA Migration

Migration:

```text
backend/src/main/resources/db/migration/V25__create_aura_module.sql
```

Tables:

| Table | Purpose |
|---|---|
| `aura_academic_terms` | University-scoped timetable planning terms. |
| `aura_programs`, `aura_batches`, `aura_sections` | Academic structure used for section-level scheduling. |
| `aura_instructors` | Instructor scheduling profiles linked optionally to users/departments. |
| `aura_rooms`, `aura_timeslots` | Physical room inventory and weekly scheduling slots. |
| `aura_instructor_availability`, `aura_room_availability` | Availability/unavailability rules for future scheduling refinements. |
| `aura_course_offerings`, `aura_meeting_requirements` | Term course offerings and required lecture/lab sessions. |
| `aura_generation_runs` | Asynchronous solver run history and status. |
| `aura_timetable_versions`, `aura_scheduled_sessions` | Generated timetable versions and assigned sessions. |
| `aura_clashes`, `aura_resolution_suggestions`, `aura_manual_moves` | Persisted clash detection, repair suggestions, and manual move audit records. |

AURA uses Spring JDBC DTO mapping rather than Hibernate entities, so these
tables are not part of JPA schema validation. Foreign keys point to existing
university, department, course, and user records where appropriate.

Additive AURA migrations:

- `V26` extends availability values with `AVOID`.
- `V27` adds section availability.
- `V28` adds normalized room and meeting-requirement facilities plus
  term-scoped calendar exceptions with typed targets and optimistic locking.
- `V29` extends scheduling resources, requirements, offering sections,
  registrations, student availability, travel rules, and solver configuration.
- `V30` adds durable import jobs, mappings, previews, validation, and source
  selection metadata.
- `V31` hardens scheduling revisions, generation inputs, timetable version
  sources, occurrence identity, and immutable version workflows.
- `V32` adds resolution, what-if, and emergency-repair workflow persistence.
- `V33` persists normalized import source rows and aligns expanded scheduling
  enum domains used by setup and timetable imports.
