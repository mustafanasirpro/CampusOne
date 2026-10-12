# AURA User Guide

## Administrators

Open **Admin → AURA** to manage timetable setup and operations.

### Prepare a term

1. Create or select an academic term.
2. Add programs, batches, sections, instructors, buildings, rooms, and
   instructional timeslots.
3. Add offerings and their meeting requirements.
4. Configure facilities, availability, teaching groups, registrations,
   calendar exceptions, conflicts, travel rules, and a constraint profile.
5. Review readiness. Resolve every blocker before generation.

### Generate and publish

1. Start generation from a ready term.
2. Monitor the run until it completes, fails, or is cancelled.
3. Review the draft score, sessions, and clashes.
4. Preview moves, swaps, or repairs where needed.
5. Publish only when the version is current, complete, and free of hard clashes.

Published versions are immutable. Clone a version or use repair/emergency tools
to create a new review draft.

### Imports, resolutions, and exports

- Imports follow upload → preview → mapping → validation → apply.
- Resolution cases show suggestions and reasons before approval/application.
- Version tools provide comparison, archive, and authorized export formats.
- Analytics and audit views summarize university-scoped scheduling activity.

## Students

Open **My timetable** to view the current published sessions associated with
your active registrations. Week patterns and personal clashes appear with each
session. Calendar export contains only your authorized timetable.

Students can submit a resolution request from their own timetable. They cannot
select another student's identity or access AURA administration.

## Instructors

Linked instructors can open their protected teaching timetable. The view derives
the instructor link from the signed-in account and shows published sessions only.

## Troubleshooting

- A disabled generation button means readiness still has blockers.
- A stale-version message means setup changed after that version was created.
- An import apply button remains unavailable until validation succeeds.
- A prohibited edit on a published timetable requires a new draft.
- Empty personal timetables usually mean no published version or no active
  registration/instructor link for the selected term.
