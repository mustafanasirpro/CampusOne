# AURA Verification Report

This report records reproduced local evidence for the current AURA implementation as of 2026-07-22. Product requirements that remain incomplete are kept separate in [AURA_COMPLETION_LEDGER.md](AURA_COMPLETION_LEDGER.md).

## Final gate snapshot

| Check | Result | Evidence |
|---|---|---|
| Backend verification | Passed | `mvn clean verify`: 607 tests, 0 failures, 0 errors, 13 skipped |
| AURA backend tests | Passed | 98 tests, 0 failures, 0 errors, 1 Docker-gated skip |
| Frontend lint | Passed | `npm run lint` |
| Frontend production build | Passed | `npm run build` (`tsc -b && vite build`) |
| Frontend component tests | Passed | `npm run test`: 8 files, 15 tests |
| Browser E2E | Passed for the implemented suite | `npm run test:e2e`: 31 passed, 0 failed, 1 intentional desktop skip for a mobile-only assertion |
| PostgreSQL/Flyway fresh path | Passed | PostgreSQL 17.10; packaged application applied V1–V36 and became healthy |
| PostgreSQL/Flyway upgrade path | Passed | `UPGRADE_FROM=35 UPGRADE_TO=36 HEALTH=PASS` |
| Render/Vercel | Not performed | Deployment and remote changes were not authorized |

## PostgreSQL-backed runtime coverage

The Playwright harness created a disposable PostgreSQL 17 database, packaged and started the real Spring Boot application, applied all migrations through V36, validated Hibernate/JPA, started Tomcat, created deterministic users and scheduling data, and ran the real Vite frontend. The suite exercised:

- unauthenticated redirects and student/admin authorization boundaries;
- readiness, selected constraint profiles, Timefold generation, score/session persistence, and publication;
- published immutability, draft clone, move preview/apply, pin/unpin, version comparison, and CSV download;
- student and linked-instructor personal timetables;
- administrator queries for instructor, section, room, course, offering, program, department, day, and week scopes, including guessed-ID rejection;
- building, teaching-group, offering-conflict, travel-rule, and active-state operations;
- persisted analytics and audit rendering;
- regeneration/publication after scheduling-input mutations;
- current-version emergency room reassignment and bounded localized-repair preview/apply;
- CSV import validation, readiness blockers, empty/error states, and mobile usability.

## Reliability defects reproduced and corrected

- A program-scoped timetable SQL predicate had a missing closing parenthesis. PostgreSQL runtime coverage reproduced it; the query is corrected and all scoped dimensions now pass.
- Dynamic emergency-resource SQL concatenation could produce `ANDroom_id`. Resource columns are allow-listed and formatted with explicit whitespace; repository tests protect every supported resource column.
- Instructor term loading and timetable loading shared one flag, causing a request race. Separate loading state and selection handling now prevent stale rendering.
- Published operational input changes correctly made the prior version stale, but the E2E workflow tried to repair it. The workflow now regenerates/publishes current data, and emergency repair now rejects stale published sources rather than cloning old sessions under a current revision.
- Scenario controls could retain a draft selected by an earlier mutation while an emergency request referenced the published source. The UI now requires and selects the published version explicitly.
- Move E2E data assumed stable cloned session IDs. It now resolves cloned sessions by stable course identity and verifies a deterministic safe room move.

## Skipped tests

The only AURA skip is `AuraDefaultProfileStartupIntegrationTest`, guarded by Testcontainers because Docker Desktop is unavailable. Its required behavior is independently covered by the packaged-application PostgreSQL 17 fresh and V35→V36 upgrade runs. Twelve existing tests in other modules are also Docker-gated.

## Solver evidence

The previously measured deterministic 300, 1,000, and 5,000 occurrence cases remain valid because this pass did not change solver construction or constraints. Each completed with `0hard/0medium/0soft`; exact timing and memory observations are in [AURA_BENCHMARK_REPORT.md](AURA_BENCHMARK_REPORT.md).

## Known verification limits

- The Playwright suite is materially expanded but does not cover every journey in the requested exhaustive matrix (notably all import formats, every setup update form, all exports, notifications, no-solution repairs, and all publication failure paths).
- The deterministic solver/repair scenario matrix and non-generation performance benchmarks remain incomplete.
- Production deployment and remote smoke testing were explicitly prohibited.
