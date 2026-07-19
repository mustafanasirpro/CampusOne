# AURA Completion Ledger

This ledger is the source of truth for the completion state of AURA — Automated
University Resource Allocation. A requirement is marked complete only when its
database, backend, security, frontend, and automated verification obligations
are all satisfied and connected to the CampusOne application.

Status legend:

- `[ ] NOT STARTED`
- `[-] IN PROGRESS`
- `[x] IMPLEMENTED AND TESTED`
- `[!] EXTERNALLY BLOCKED`

Last locally verified: 2026-07-19. See [`AURA_TEST_REPORT.md`](AURA_TEST_REPORT.md)
for the commands, results, PostgreSQL startup evidence, and remaining test gaps.

## Repository and safety gates

- [x] IMPLEMENTED AND TESTED — AURA remains inside the existing CampusOne Spring Boot and React applications.
- [x] IMPLEMENTED AND TESTED — AURA uses PostgreSQL/Flyway and in-process Timefold Solver Community Edition.
- [x] IMPLEMENTED AND TESTED — AURA management is protected by backend admin authorization; moderator access is not implicitly granted.
- [x] IMPLEMENTED AND TESTED — Runtime activation uses the single `campusone.aura.enabled` property and production-default bean wiring has regression coverage.
- [x] IMPLEMENTED AND TESTED — Active generation runs are guarded per academic term.
- [x] IMPLEMENTED AND TESTED — Only draft timetable versions can be published and unresolved hard clashes block publication.
- [x] IMPLEMENTED AND TESTED — Scheduling-input tables increment the affected term revision; generation persistence and publication reject stale revisions, and one queued/running generation is enforced per term.
- [ ] NOT STARTED — Complete AURA audit-event coverage for setup, imports, generation, changes, repair, simulation, publication, and archive actions.
- [-] IN PROGRESS — Published-timetable notifications are sent to active students without duplicate recipients; complete admin and instructor notification coverage remains incomplete.

## Academic scheduling domain and setup

- [-] IN PROGRESS — Academic terms: full fields, lifecycle, validation, optimistic locking, CRUD, search, pagination, archive/deactivate, frontend.
- [-] IN PROGRESS — Academic programs: full fields, department/university isolation, optimistic locking, CRUD, search, pagination, frontend.
- [-] IN PROGRESS — Batches/cohorts: full fields, lifecycle, CRUD, search, pagination, frontend.
- [-] IN PROGRESS — Sections: full fields, load policies, building, optimistic locking, CRUD, search, pagination, frontend.
- [-] IN PROGRESS — Instructors: employee identity, load policies, building, optional user link, optimistic locking, CRUD, search, pagination, frontend.
- [-] IN PROGRESS — Rooms: room codes, types, capacity, building, optimistic locking, CRUD, search, pagination, frontend.
- [x] IMPLEMENTED AND TESTED — Normalized room facilities and requirement facility matching across migration, API, setup UI, readiness, and solver hard constraints.
- [-] IN PROGRESS — Timeslots and breaks: term scope, ordering, overlap validation, instructional/break types, CRUD, frontend.
- [-] IN PROGRESS — Calendar exceptions for holidays, non-teaching days, events, absences, closures, restrictions, cancellations, and outages have additive schema, scoped CRUD APIs, optimistic locking, and setup UI; generation/repair occurrence handling remains incomplete.
- [x] IMPLEMENTED AND TESTED — Instructor availability supports `AVAILABLE`, `UNAVAILABLE`, `AVOID`, and `PREFERRED` in API, readiness, and solver.
- [x] IMPLEMENTED AND TESTED — Room availability supports `AVAILABLE`, `UNAVAILABLE`, `AVOID`, and `PREFERRED` in API, readiness, and solver.
- [x] IMPLEMENTED AND TESTED — Section availability supports `AVAILABLE`, `UNAVAILABLE`, `AVOID`, and `PREFERRED` in API, readiness, and solver.
- [-] IN PROGRESS — Individual student availability is persisted, importable, and enforced by the solver for active registrations; dedicated CRUD UI/API remains incomplete.
- [-] IN PROGRESS — Course offerings: sections, enrollment, combined/parallel/elective groups, teaching groups, lifecycle, CRUD, frontend.
- [-] IN PROGRESS — Meeting requirements: occurrences, contiguous duration, instructor/room needs, day/time rules, groups, week patterns, fixed/pinned assignments, CRUD, frontend.
- [-] IN PROGRESS — Offering conflicts have normalized database pairs, source/severity validation, atomic imports, and hard-overlap detector support; dedicated CRUD UI remains incomplete.
- [-] IN PROGRESS — University-scoped building travel-time rules have constrained schema, atomic upsert imports, impossible-travel hard constraints, and difficult-travel medium constraints; dedicated CRUD UI remains incomplete.
- [ ] NOT STARTED — Constraint-weight configuration and solver profiles.
- [-] IN PROGRESS — The workbench now provides a connected creation workflow for programs, batches, sections, instructors, rooms/facilities, timeslots, offerings, requirements/facilities, availability, and calendar exceptions with loading, error, validation, and submitting states; update/archive flows and remaining domain inputs are incomplete.

## Revision, readiness, and generation

- [-] IN PROGRESS — Scheduling data revisions are recorded and enforced; a complete deterministic content checksum remains incomplete.
- [x] IMPLEMENTED AND TESTED — Revision triggers cover scheduling-input mutations, while atomic imports participate through the same affected tables.
- [x] IMPLEMENTED AND TESTED — Generation records its input revision, persists a completed result atomically, and rejects cancelled or stale result persistence and stale publication.
- [-] IN PROGRESS — Readiness validates active rooms, instructional timeslots, active instructors, active offerings/requirements, weekly occurrence capacity, fixed resources, day rules, and resource/section/student availability candidates.
- [ ] NOT STARTED — Readiness validates facilities, calendar exceptions, registrations, teaching groups, travel, week patterns, loads, enrollment, fixed assignments, and cross-scope references.
- [x] IMPLEMENTED AND TESTED — Asynchronous generation has guarded queued/running/completed/failed/cancelled transitions, cancellation, abandoned-run cleanup, atomic score/draft persistence, and a per-term active-run database constraint.
- [ ] NOT STARTED — Deterministic seeded generation profiles for balanced, compact, room-efficient, instructor-friendly, and repair modes.
- [-] IN PROGRESS — Generation uses a 500-candidate solver limit, a 300-second termination limit, one active run per term, and abandoned-run cleanup; explicit memory budgeting and richer termination diagnostics remain incomplete.

## Solver hard constraints

- [x] IMPLEMENTED AND TESTED — Room overlap, including overlapping clock ranges.
- [x] IMPLEMENTED AND TESTED — Instructor overlap, including overlapping clock ranges.
- [x] IMPLEMENTED AND TESTED — Section overlap, including overlapping clock ranges.
- [x] IMPLEMENTED AND TESTED — Individual active-registration student overlap.
- [x] IMPLEMENTED AND TESTED — Hard offering conflict and combined-section overlap.
- [x] IMPLEMENTED AND TESTED — Room capacity.
- [x] IMPLEMENTED AND TESTED — Room type.
- [x] IMPLEMENTED AND TESTED — Required facilities.
- [x] IMPLEMENTED AND TESTED — Instructor unavailability.
- [x] IMPLEMENTED AND TESTED — Room unavailability.
- [x] IMPLEMENTED AND TESTED — Section unavailability.
- [x] IMPLEMENTED AND TESTED — Registered-student `UNAVAILABLE` timeslots, with `AVOID` and `PREFERRED` scoring.
- [x] IMPLEMENTED AND TESTED — Break prohibition and no crossing of breaks in solver and independent detector.
- [x] IMPLEMENTED AND TESTED — Contiguous, same-day multi-slot session assignment with effective end-time overlap detection.
- [x] IMPLEMENTED AND TESTED — The solver creates one session per required occurrence and publication independently rejects missing, duplicate, or unexpected occurrences.
- [x] IMPLEMENTED AND TESTED — Correct instructor assignment from the meeting requirement.
- [x] IMPLEMENTED AND TESTED — Instructor daily and weekly hard loads.
- [x] IMPLEMENTED AND TESTED — Section daily hard load.
- [-] IN PROGRESS — Fixed assignments are enforced by solver, detector, and move/swap preflight; localized-repair pin preservation remains incomplete.
- [ ] NOT STARTED — Inactive-resource, cross-university, and cross-term prohibition in assembled solver facts.
- [x] IMPLEMENTED AND TESTED — Allowed/prohibited day rules.
- [-] IN PROGRESS — Linked requirements and lecture-before-linked ordering are hard solver constraints; complete teaching-group policy remains incomplete.
- [ ] NOT STARTED — Teaching-group consistency and duplicate parallel-attendance prevention.
- [ ] NOT STARTED — Calendar exceptions, closures, cancellations, restrictions, and facility outages.
- [ ] NOT STARTED — Online/hybrid resource rules.
- [x] IMPLEMENTED AND TESTED — Odd/even/custom week patterns are persisted and respected by solver overlaps, personal timetables, ICS, and detector overlap logic.
- [ ] NOT STARTED — Shared specialized-resource collision.
- [x] IMPLEMENTED AND TESTED — Impossible or insufficient building travel for shared instructors, participating sections, and registered students.
- [ ] NOT STARTED — Enrollment capacity after student transfer.
- [ ] NOT STARTED — Academic registration validity, duplicate active registration prevention, and compatible lecture/lab groups.

## Solver medium and soft constraints

- [x] IMPLEMENTED AND TESTED — `AVOID` availability is penalized at medium level for instructor, room, and section.
- [x] IMPLEMENTED AND TESTED — `PREFERRED` availability is rewarded at soft level for instructor, room, and section.
- [x] IMPLEMENTED AND TESTED — Minimum day separation and maximum occurrences per day.
- [x] IMPLEMENTED AND TESTED — Preferred days and time windows.
- [x] IMPLEMENTED AND TESTED — Same-room preference and room stability.
- [x] IMPLEMENTED AND TESTED — Preferred and hard load balancing for instructors and sections.
- [ ] NOT STARTED — Gap minimization for students, sections, and instructors.
- [ ] NOT STARTED — Early/late and undesirable-slot fairness.
- [-] IN PROGRESS — Difficult-travel penalties are implemented; general building-stability preferences remain incomplete.
- [ ] NOT STARTED — Parent assignment stability for localized repair.
- [ ] NOT STARTED — Configurable hard/medium/soft weights with validation.

## Imports

- [-] IN PROGRESS — Secure import job lifecycle, bounded file/row/cell validation, status transitions, persisted source rows, and atomic application are implemented; complete audit-event coverage remains incomplete.
- [x] IMPLEMENTED AND TESTED — CSV setup, registration, and timetable parsing with preview, mapping, validation, and apply.
- [x] IMPLEMENTED AND TESTED — XLSX multi-sheet setup, registration, and timetable parsing with preview, source selection, mapping, validation, and apply.
- [x] IMPLEMENTED AND TESTED — Legacy XLS parsing with preview, mapping, validation, and apply.
- [x] IMPLEMENTED AND TESTED — Text-based PDF timetable extraction with explicit OCR-required handling for scanned PDFs.
- [ ] NOT STARTED — Row, grid, multi-sheet, combined-cell, and registration layouts.
- [x] IMPLEMENTED AND TESTED — Mapping profiles, persisted raw rows, normalized preview, unknown-reference handling, actionable row errors, warnings, and transactional apply.
- [x] IMPLEMENTED AND TESTED — Imported timetable draft creation and independent clash detection.
- [x] IMPLEMENTED AND TESTED — Import frontend workflow covering upload, sheet selection, mapping, validation, apply, and results.

## Student registrations and personal timetables

- [-] IN PROGRESS — Student registration model, uniqueness, optimistic locking, admin CRUD, atomic bulk import, and student privacy are implemented; full delete/history UI remains incomplete.
- [x] IMPLEMENTED AND TESTED — Primary, repeater, elective, cross-section, improvement, makeup, manual, and transferred registration types.
- [-] IN PROGRESS — Home/teaching section and lecture/lab/tutorial group validation are implemented; complete equivalent-offering academic policy remains incomplete.
- [x] IMPLEMENTED AND TESTED — Personal timetable week view combines home and individual registrations while respecting selected teaching groups and week patterns.
- [x] IMPLEMENTED AND TESTED — Personal clash indicators, resolution requests, print view, and teaching-date-aware ICS export.
- [x] IMPLEMENTED AND TESTED — Student-only registration/timetable access derives identity from JWT and admin inspection is university-scoped.

## Independent clash detection

- [-] IN PROGRESS — Persisted detector covers clock-range room/instructor/section/hard-offering collisions, capacity, room type/facilities, availability, inactive resources, fixed assignments, multi-slot contiguity, breaks, calendar exceptions, week patterns, and occurrence integrity; remaining personal/travel/linkage checks are incomplete.
- [ ] NOT STARTED — Student, repeater, elective, cross-section, shared-student, availability, facility, load, occurrence, contiguity, break, registration, calendar, linkage, travel, week-pattern, and virtual-resource clashes.
- [ ] NOT STARTED — Stable clash fingerprints, affected-resource sets, authorized student references, explanations, corrective directions, resolver, and status lifecycle.
- [ ] NOT STARTED — Detector parity across generated, imported, manual, repaired, what-if, emergency, and personal schedules.
- [ ] NOT STARTED — Hard-clash publication gate based on complete detector coverage.

## Resolution, moves, and repair

- [x] IMPLEMENTED AND TESTED — Resolution case lifecycle and scoped student/admin APIs.
- [-] IN PROGRESS — Parallel-offering, alternate lab, and alternate tutorial suggestions are implemented with safe application; compatible section, makeup, equivalent, and registration-adjustment strategies remain incomplete.
- [ ] NOT STARTED — Capacity, policy, week-pattern, group, duplicate-registration, personal-clash, and institutional-clash validation for transfers.
- [-] IN PROGRESS — Student-only suggestions are ranked by safety and disruption with capacity/clash explanations; complete institutional impact metrics remain incomplete.
- [x] IMPLEMENTED AND TESTED — Manual move preview/apply is connected end to end for draft versions with stale-version protection, clash refresh, and an audit row.
- [-] IN PROGRESS — Room/time move, safe swap preview/apply, and pin/unpin with lock reason and audit are implemented; full score delta and version-based undo UI remain incomplete.
- [ ] NOT STARTED — Automatic localized minimum-disruption repair with cloned draft, dependency neighborhood, pinned unaffected sessions, comparison, and unresolved-clash report.
- [-] IN PROGRESS — Non-destructive what-if records and affected-session analysis are connected to the workbench; full feasibility solving and rich comparison remain incomplete.
- [-] IN PROGRESS — Emergency requests clone published schedules, identify impact, pin unaffected sessions, detect clashes, and expose review drafts; automatic reassignment remains incomplete.

## Versioning, views, exports, and analytics

- [x] IMPLEMENTED AND TESTED — Draft/published/superseded/archived lifecycle enforces one published version per term and atomically supersedes the previous publication.
- [x] IMPLEMENTED AND TESTED — `SUPERSEDED` status and generated/imported/manual/repaired/emergency/what-if source metadata are persisted.
- [x] IMPLEMENTED AND TESTED — Session move, swap, pin/unpin, archive, and publish mutations use draft-state and optimistic-version guards; published versions are immutable through these endpoints.
- [ ] NOT STARTED — Clone/revalidate rollback workflow.
- [-] IN PROGRESS — Version comparison reports stable occurrence-level additions, removals, and room/timeslot changes; student, workload, travel, and fairness deltas remain incomplete.
- [ ] NOT STARTED — Student, instructor, section, room, course, program, department, day, and week views.
- [x] IMPLEMENTED AND TESTED — Admin version exports support CSV, XLSX, JSON, printable HTML, PDF, and ICS, while students have scoped ICS.
- [-] IN PROGRESS — Basic counts for rooms, timeslots, offerings, versions, sessions, and unresolved clashes.
- [ ] NOT STARTED — Complete score, utilization, gaps, load variance, travel, undesirable-slot fairness, disruption, candidate, seed, duration, and bottleneck diagnostics.

## Frontend workbench

- [-] IN PROGRESS — Admin route and dashboard shell at `/admin/aura`.
- [ ] NOT STARTED — Terms management screen with full lifecycle controls.
- [ ] NOT STARTED — Setup wizard and CRUD screens for every scheduling input.
- [ ] NOT STARTED — Offerings, requirements, groups, conflicts, and registrations screens.
- [x] IMPLEMENTED AND TESTED — Import upload, sheet selection, preview, mapping, whole-file validation, apply, and result screen.
- [-] IN PROGRESS — Readiness, generation run, version, session, clash, metrics, and publication views.
- [-] IN PROGRESS — The version workbench connects move preview/apply, swap, pin/unpin, comparison, archive, and exports; score-delta detail and version-based undo remain incomplete.
- [x] IMPLEMENTED AND TESTED — Resolution-case workbench with analyze, approve, reject, and apply actions.
- [-] IN PROGRESS — What-if and emergency-repair workbench is connected; richer feasibility and repair controls remain incomplete.
- [x] IMPLEMENTED AND TESTED — Student personal timetable, print/calendar export, clash display, registration list, and resolution request screen.
- [ ] NOT STARTED — Instructor timetable screen.
- [ ] NOT STARTED — Complete responsive, accessible loading/empty/error/validation/submitting/confirmation states across AURA.

## Security, auditing, and privacy

- [x] IMPLEMENTED AND TESTED — AURA admin APIs require authenticated `ADMIN` capability through backend authorization.
- [x] IMPLEMENTED AND TESTED — `MODERATOR` does not automatically manage AURA.
- [x] IMPLEMENTED AND TESTED — Existing setup, availability, generation, version, session, move, and metrics endpoints derive and enforce the authenticated admin university; cross-university identifiers return not found.
- [ ] NOT STARTED — Cross-term, guessed-UUID, other-student, malicious-import, and optimistic-lock security coverage for the complete module.
- [ ] NOT STARTED — Student sees only own timetable, registrations, clashes, and resolution cases.
- [ ] NOT STARTED — Instructor sees only authorized personal timetable data.
- [ ] NOT STARTED — Complete personal-data minimization in imports, diagnostics, auditing, exports, and notifications.

## Automated fixtures, testing, and backtesting

- [ ] NOT STARTED — Deterministic fixtures A–S defined in the completion brief.
- [ ] NOT STARTED — At least 20 deterministic generator/resolver backtest scenarios.
- [ ] NOT STARTED — `AURA_BACKTEST_REPORT.md` with expected-versus-actual outcomes.
- [-] IN PROGRESS — Backend domain/service/readiness/solver/clash/authorization/application-context tests include atomic generation persistence, stale-state guards, publication occurrence integrity, notifications, and resolution concurrency; the exhaustive matrix below remains incomplete.
- [ ] NOT STARTED — Repository, controller, complete migration, ConstraintVerifier-per-constraint, import, registration, personal timetable, resolver, move/swap, repair, simulation, emergency, comparison, export, concurrency, locking, packaged-JAR, and end-to-end API coverage.
- [ ] NOT STARTED — Frontend component test framework and AURA component tests.
- [ ] NOT STARTED — Playwright browser E2E framework and the 29 required AURA journeys.
- [ ] NOT STARTED — Tiny, ~300, ~1,000, and offline ~5,000 occurrence benchmarks.
- [ ] NOT STARTED — `AURA_BENCHMARK_REPORT.md` with readiness, solver, memory, utilization, gap, and repair metrics.

## Verification, deployment, and regression

- [x] IMPLEMENTED AND TESTED — Current backend suite passed after generation-persistence, publication, resolution-concurrency, and workbench integration: 581 tests, 0 failures, 0 errors, 13 skipped.
- [x] IMPLEMENTED AND TESTED — Current frontend lint and TypeScript/Vite production build passed with the connected version move workflow.
- [!] EXTERNALLY BLOCKED — Docker/Testcontainers execution remains unavailable because the Docker Desktop named pipe is absent; equivalent migration and startup coverage was completed with a disposable local PostgreSQL 17 cluster.
- [x] IMPLEMENTED AND TESTED — Packaged JAR started against disposable PostgreSQL 17, applied and validated all 34 migrations, initialized JPA, and started embedded Tomcat; an earlier authenticated smoke covered setup/readiness/generation/version/session/move-preview and a one-session Timefold run.
- [ ] NOT STARTED — Full browser E2E execution.
- [x] IMPLEMENTED AND TESTED — The full Maven verification suite passed across AURA and existing CampusOne modules after the current changes; browser-level cross-module regression remains part of the separate E2E gate.
- [ ] NOT STARTED — Commit and push only after every local implementable gate passes.
- [ ] NOT STARTED — Render deployment, migrations, health, authenticated AURA API, frontend integration, and safe production smoke verification.

## Documentation

- [-] IN PROGRESS — Existing CampusOne context, status, decisions, tasks, README, backend README, API, schema, test plan, and changelog accurately describe the current partial AURA surface.
- [ ] NOT STARTED — Update all required existing documents for the completed AURA system.
- [ ] NOT STARTED — `AURA_ARCHITECTURE.md`.
- [ ] NOT STARTED — `AURA_REQUIREMENTS.md`.
- [ ] NOT STARTED — `AURA_CONSTRAINTS.md`.
- [ ] NOT STARTED — `AURA_IMPORT_GUIDE.md`.
- [ ] NOT STARTED — `AURA_RESOLUTION_GUIDE.md`.
- [ ] NOT STARTED — `AURA_USER_GUIDE.md`.
- [x] IMPLEMENTED AND TESTED — `AURA_TEST_REPORT.md` records the current automated and PostgreSQL verification evidence without overstating browser or production coverage.
- [ ] NOT STARTED — `AURA_BACKTEST_REPORT.md`.
- [ ] NOT STARTED — `AURA_BENCHMARK_REPORT.md`.
- [ ] NOT STARTED — `AURA_DEPLOYMENT_CHECKLIST.md`.

## Final completion gates

- [ ] NOT STARTED — A. Complete setup backend and frontend.
- [ ] NOT STARTED — B. CSV import.
- [ ] NOT STARTED — C. XLSX import.
- [ ] NOT STARTED — D. Text-based PDF import.
- [ ] NOT STARTED — E. Student registrations.
- [ ] NOT STARTED — F. Personal timetable.
- [ ] NOT STARTED — G. Repeater/elective/cross-section resolver.
- [ ] NOT STARTED — H. Parallel-section and alternate lab/tutorial transfer.
- [ ] NOT STARTED — I. Complete independent detector.
- [ ] NOT STARTED — J. Ranked suggestions.
- [ ] NOT STARTED — K. Manual move and swap.
- [ ] NOT STARTED — L. Automatic localized repair.
- [ ] NOT STARTED — M. What-if simulation.
- [ ] NOT STARTED — N. Emergency repair.
- [ ] NOT STARTED — O. Version comparison.
- [ ] NOT STARTED — P. Views and exports.
- [x] IMPLEMENTED AND TESTED — Q. Backend full verification passed for the current implementation (581 tests, 0 failures, 0 errors, 13 skipped).
- [-] IN PROGRESS — R. Frontend lint and production build pass; no frontend component-test framework is configured, so automated component tests remain incomplete.
- [ ] NOT STARTED — S. Browser E2E.
- [x] IMPLEMENTED AND TESTED — T. Isolated PostgreSQL migration/startup verification completed with a disposable local PostgreSQL 17 instance; Docker-specific Testcontainers execution remains externally unavailable.
- [ ] NOT STARTED — U. Backtesting and benchmarks.
- [ ] NOT STARTED — V. Render production verification.
- [ ] NOT STARTED — W. Full CampusOne regression verification.
- [ ] NOT STARTED — X. Complete documentation.
