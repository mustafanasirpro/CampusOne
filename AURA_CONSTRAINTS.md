# AURA Constraint Reference

AURA scores timetable assignments with Timefold
`HardMediumSoftScore`. The executable catalog is
`AuraConstraintCatalog`; the implementation is `AuraConstraintProvider`.
Persisted constraint profiles may adjust weights but cannot change a
constraint's hard, medium, or soft level.

## Hard constraints

- room, instructor, section, combined-section, registered-student, and hard
  offering-conflict overlap;
- duplicate teaching-group attendance;
- instructor, room, section, and registered-student unavailability;
- calendar-exception assignment;
- room capacity, room type, and required facilities;
- contiguous instructional blocks and break avoidance;
- allowed/prohibited days, fixed room/timeslot, and pinned assignment;
- occurrence limits and minimum day separation;
- lecture-before-linked-meeting ordering;
- impossible building travel;
- instructor weekly/daily and section daily hard load limits.

Any hard-score penalty makes a generated result ineligible for publication.
Publication also runs independent occurrence and clash checks.

## Medium constraints

- instructor, room, section, and registered-student `AVOID` availability;
- difficult building travel;
- instructor preferred weekly/daily load and section preferred daily load.

## Soft constraints

- preferred instructor, room, section, and student timeslots;
- preferred days and time windows;
- same-room and section-building stability;
- spread meetings;
- instructor, section, and registered-student idle gaps;
- undesirable early/late time fairness;
- parent-assignment stability for repair.

## Profiles

| Profile | Intent |
|---|---|
| `FAST_FEASIBLE` | Prioritize hard feasibility |
| `BALANCED` | General scheduling |
| `COMPACT` | Reduce gaps and compact teaching days |
| `ROOM_EFFICIENT` | Favor room consistency and resource efficiency |
| `INSTRUCTOR_FRIENDLY` | Favor instructor preferences |
| `QUALITY` | Increase non-hard optimization |
| `REPAIR` | Preserve parent assignments during localized repair |
| `WHAT_IF` | Evaluate scenario impact |

## Week and travel semantics

Overlap applies only when week patterns intersect. `EVERY_WEEK`, `ODD_WEEK`,
`EVEN_WEEK`, and explicit custom weeks are supported. Travel checks compare
adjacent same-day assignments for shared instructors, participating sections,
and registered students using normalized building names and configured travel
minutes.

The current verified and incomplete constraint work is tracked in
[AURA_COMPLETION_LEDGER.md](AURA_COMPLETION_LEDGER.md); this guide must not be
used as evidence that every future policy is implemented.
