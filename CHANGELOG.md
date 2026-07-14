# Changelog

## Unreleased

### Added

- Native Lost & Found module:
  - authenticated browse, submit, detail, edit, owner lifecycle actions, claims, matches, and admin statistics;
  - PostgreSQL/Flyway schema for items, images, claims, and deterministic matches;
  - R2/S3-compatible image storage metadata through the existing storage abstraction;
  - moderation approval integration using `LOST_FOUND_ITEM`;
  - content-report support for visible Lost & Found items;
  - Lost & Found notification type and target types;
  - frontend routes, navigation entry, dashboard card, item forms, browse/detail pages, and claims/matches page.
- Deterministic Lost & Found matching with a 60-point threshold and explainable JSON reasons.
- Focused unit tests for Lost & Found service behavior, matching behavior, and moderation authorization boundaries.
- Native AURA timetable generator:
  - normalized Flyway schema for academic terms, programs, batches, sections, instructors, rooms, timeslots, offerings, requirements, generation runs, timetable versions, sessions, clashes, suggestions, and manual moves;
  - admin-only Spring Boot API under `/api/v1/admin/aura`;
  - Timefold Solver Community Edition integration for in-process timetable generation;
  - readiness validation, asynchronous generation runs, clash detection, timetable version publishing, and manual move preview/apply support;
  - frontend admin workbench at `/admin/aura`;
  - focused tests for readiness, authorization, clash detection, and solver assignment.

### Changed

- Extended existing moderation, notification, and storage code to recognize Lost & Found targets.
- Frontend moderation and notification labels now include Lost & Found.
- Admin navigation now includes the AURA workbench.

### Notes

- Lost & Found is intentionally excluded from unauthenticated global search to preserve same-university privacy rules.
- Browser and production deployment verification remain separate manual steps.
