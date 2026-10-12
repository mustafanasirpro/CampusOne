# AURA Resolution and Repair Guide

AURA separates student registration resolution, manual timetable changes,
localized repair, scenarios, and emergency drafts. None of these flows mutates a
published version directly.

## Student resolution

Students create a scoped resolution case from their own timetable. An
administrator analyzes the case, reviews ranked suggestions, approves or rejects
a suggestion, and applies it only after the backend revalidates authorization,
capacity, duplicate registration, and timetable-clash safety.

Current suggestion labels include compatible section transfer,
parallel-offering transfer, equivalent offering, makeup session, registration
adjustment, and alternate teaching-group options. A suggestion that cannot be
applied safely remains visible with its reason instead of silently changing the
registration.

## Manual changes

- **Move** previews room/timeslot impact and applies only to a current draft.
- **Swap** previews both sessions and revalidates before applying.
- **Pin/unpin** protects intentional assignments from generation and repair.
- Every accepted mutation refreshes clashes and writes audit evidence.

## Localized repair

Repair creates a bounded preview for a clash or session, searches allowed
room/timeslot alternatives, preserves pinned and unaffected assignments, and
returns an expiring token. Apply rejects stale, expired, or altered previews and
persists changes atomically to an isolated draft.

## What-if and emergency operations

Resource what-if scenarios compare baseline and projected clashes without
mutating timetable data. Emergency room, facility, and timeslot closures clone a
current published version into a review draft and attempt safe reassignment.

## Operational safeguards

- Work only from current, university-scoped versions.
- Review impacts before apply.
- Never treat a preview as a committed timetable.
- Publish only after readiness, occurrence integrity, and hard-clash checks pass.
- Use version comparison and audit history to understand changes.

Known strategy and multi-session repair limitations remain in
[AURA_COMPLETION_LEDGER.md](AURA_COMPLETION_LEDGER.md).
