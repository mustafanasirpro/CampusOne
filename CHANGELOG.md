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
- AURA runtime activation now uses the single `campusone.aura.enabled`
  property, enabled by default for production and disabled only in intentional
  test-slice contexts.
- AURA generation and publication are safer:
  - only one queued/running generation run may exist per term;
  - draft versions with unresolved hard clashes cannot be published;
  - independent clash detection now checks actual overlapping time ranges, not
    only identical timeslot IDs.
- AURA availability support now includes instructor, room, and section availability API
  endpoints, `AVOID` availability values, solver hard constraints for
  unavailable resource/timeslot pairs, and readiness checks
  for meeting requirements with no valid room-time candidate.
- AURA setup is now available directly in the admin workbench for the existing
  scheduling inputs, using university-scoped department/course references
  rather than raw UUID entry.
- AURA authorization now enforces the authenticated admin's university across
  setup, generation, versions, sessions, moves, and metrics.
- AURA room and meeting-requirement facilities are normalized and enforced by
  readiness and Timefold hard constraints. Term calendar exceptions now have
  scoped create/list/update/deactivate APIs and setup UI.
- Timefold overlap constraints compare real clock ranges, and room type is now
  enforced as a hard requirement.
- AURA solver facts now include participating sections, active student
  registrations, hard offering conflicts, student availability, week patterns,
  linked requirements, and university building-travel rules. ConstraintVerifier
  coverage verifies student clashes, offering clashes, lecture ordering, and
  travel safety independently of the optimizer.
- PostgreSQL-backed AURA startup verification fixed UUID projection mapping,
  room display-name persistence, and generation-run timestamp binding; an
  authenticated smoke test now covers setup, readiness, generation, version
  sessions, and move preflight.

### Notes

- Lost & Found is intentionally excluded from unauthenticated global search to preserve same-university privacy rules.
- Browser and production deployment verification remain separate manual steps.
