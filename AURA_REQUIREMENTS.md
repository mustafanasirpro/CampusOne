# AURA Requirements

This document defines the product invariants for CampusOne AURA. Verification
status belongs in [AURA_COMPLETION_LEDGER.md](AURA_COMPLETION_LEDGER.md); a
requirement listed here is not automatically complete.

## Roles and isolation

- Administrators manage scheduling data only for their authenticated university.
- Students see only their own registrations and published personal timetable.
- Linked instructors see only their own published teaching timetable.
- Moderation capability alone does not grant AURA administration.
- Guessed identifiers must never cross university, term, student, or instructor
  boundaries.

## Scheduling setup

A term may contain programs, batches, sections, instructors, buildings, rooms,
timeslots, offerings, meeting requirements, facilities, availability,
registrations, teaching groups, calendar exceptions, offering conflicts, travel
rules, and a constraint profile.

Updates must preserve optimistic locking and referenced records. Deactivation is
preferred to destructive deletion where history depends on a record.

## Readiness and generation

- Readiness returns actionable blockers and warnings before generation.
- Generation uses active, same-university, same-term facts only.
- One active run is allowed per term.
- Cancellation, failure, stale input, or incomplete solutions must not persist a
  partial timetable version.
- Generated versions begin as drafts and record score, seed/profile, input
  revision, and source metadata.

## Timetable integrity

Hard constraints protect rooms, instructors, sections, registered students,
capacity, facilities, availability, breaks, fixed/pinned assignments, calendar
exceptions, week patterns, linked teaching, and travel feasibility.

Publication requires a current input revision, complete occurrences, no
unresolved hard clashes, and one active published version per term.

## Changes and recovery

- Moves and swaps are previewed before application.
- Published versions cannot be edited.
- Repair preserves pinned and unaffected sessions and applies from a
  non-expired, untampered preview.
- Emergency operations create a new review draft.
- Resolution suggestions must be scoped, capacity-safe, and revalidated at
  application time.

## Imports and exports

Imports require authenticated administration, file/content validation, preview,
field mapping, row validation, and atomic application. Supported readers include
CSV, XLS, XLSX, and text PDFs; scanned PDFs return `OCR_REQUIRED`.

Exports enforce university/version access and use safe filenames and content
types. Admin formats include CSV, XLSX, JSON, HTML, PDF, and ICS; student ICS is
scoped to the signed-in student.

## Acceptance

Local acceptance requires backend verification, frontend lint/build/component
tests, PostgreSQL migrations and startup, authenticated runtime workflows,
Playwright desktop/mobile coverage, and an accurate documentation ledger.
Production acceptance additionally requires an authorized Render/Vercel smoke
test and is never inferred from local evidence.
