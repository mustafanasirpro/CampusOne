# AURA Verification Report

This report records reproduced local evidence for the current AURA
implementation as of 2026-07-27. Product requirements that remain incomplete
are tracked separately in
[AURA_COMPLETION_LEDGER.md](AURA_COMPLETION_LEDGER.md).

## Final gate snapshot

| Check | Result | Evidence |
|---|---|---|
| Backend verification | Passed | `mvn clean verify`: 633 tests, 0 failures, 0 errors, 13 skipped |
| AURA backend tests | Passed | 124 tests, 0 failures, 0 errors, 1 Docker-gated skip |
| Frontend lint | Passed | `npm run lint` |
| Frontend production build | Passed | `npm run build` (`tsc -b && vite build`) |
| Frontend component tests | Passed | `npm run test`: 12 files, 26 tests |
| Browser E2E | Passed for the implemented suite | `npm run test:e2e`: 31 passed, 0 failed, 1 intentional desktop skip for a mobile-only assertion |
| PostgreSQL/Flyway fresh path | Passed | PostgreSQL 17.10; packaged application applied V1–V37 and became healthy |
| PostgreSQL/Flyway upgrade path | Passed | `UPGRADE_FROM=36 UPGRADE_TO=37 HEALTH=PASS` |
| Solver benchmark | Passed | 300, 1,000, and 5,000 occurrences completed with zero hard score |
| Render/Vercel | Not performed | Deployment and remote changes were not authorized |

## PostgreSQL-backed runtime coverage

The Playwright harness created a disposable PostgreSQL 17 database, packaged
and started the real Spring Boot application, applied migrations through V37,
validated application persistence mappings, started Tomcat, created
deterministic users and scheduling data, and ran the real Vite frontend.

The suite exercised:

- unauthenticated redirects and student/admin authorization boundaries;
- readiness, constraint profiles, Timefold generation, score/session
  persistence, and publication;
- published immutability, draft clone, move preview/apply, pin/unpin, version
  comparison, and CSV download;
- student and linked-instructor personal timetables;
- administrator timetable queries by instructor, section, room, course,
  offering, program, department, day, and week, including guessed-ID rejection;
- building, teaching-group, offering-conflict, travel-rule, and active-state
  operations;
- persisted analytics and audit rendering;
- regeneration/publication after input mutations;
- emergency room reassignment and bounded localized-repair preview/apply;
- CSV import validation, readiness blockers, empty/error states, and mobile
  usability.

## Focused evidence added in this pass

- Master-data update and activation API/UI coverage, including full edit fields
  for sections, instructors, rooms, timeslots, offerings, and requirements.
- Dedicated student-availability create/list/delete behavior.
- Same-university/same-term active solver fact query contracts.
- Teaching-group duplicate attendance, calendar blocking, student/section/
  instructor gaps, undesirable-time fairness, building stability, and repair
  parent stability constraints.
- V37 date-aware calendar recurrence and cancellation-aware CSV/XLSX/JSON/
  HTML/PDF/ICS exports.
- Resolution candidate classification and apply-time safety for compatible,
  parallel, equivalent, makeup, and adjustment strategies.
- What-if baseline/projected clash deltas for resource scenarios.
- Compatible visible-sheet aggregation, merged workbook values, normalized
  cross-sheet headers, and formula rejection.
- Structural version comparison plus hard/medium/soft score deltas.

## Skipped tests

The only AURA skip is `AuraDefaultProfileStartupIntegrationTest`, guarded by
Testcontainers because Docker Desktop is unavailable. Its required default
profile/startup behavior is independently covered by the packaged-application
PostgreSQL 17 fresh and V36→V37 upgrade runs. Twelve existing tests in other
modules are also Docker-gated.

## Solver evidence

The current constraints were benchmarked again:

| Occurrences | Elapsed | Score |
|---:|---:|---|
| 300 | 2,805 ms | `0hard/0medium/-150soft` |
| 1,000 | 2,152 ms | `0hard/0medium/-200soft` |
| 5,000 | 17,237 ms | `0hard/0medium/-500soft` |

Exact memory measurements and interpretation are in
[AURA_BENCHMARK_REPORT.md](AURA_BENCHMARK_REPORT.md).

## Known verification limits

- The Playwright suite does not yet cover every setup form, import/export
  format, notification outcome, no-solution repair, or publication failure.
- Arbitrary visual timetable-grid imports, the named fixtures A–S matrix,
  ConstraintVerifier behavior tests for every catalog entry, multi-session
  dependency repair, and non-generation performance benchmarks remain open.
- Production deployment and remote smoke testing were explicitly prohibited.
