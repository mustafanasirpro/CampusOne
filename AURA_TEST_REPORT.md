# AURA Verification Report

This report records locally reproduced evidence for the current AURA implementation. It separates passing gates from product scope that remains incomplete in the [completion ledger](AURA_COMPLETION_LEDGER.md).

## Verification snapshot

| Check | Result | Evidence |
|---|---|---|
| Backend verification | Passed | `mvn clean verify`: 590 tests, 0 failures, 0 errors, 13 skipped |
| AURA backend tests | Passed | 81 tests, 0 failures, 0 errors, 1 Docker-gated skip |
| Frontend lint | Passed | `npm run lint` |
| Frontend production build | Passed | `npm run build` |
| Frontend component tests | Passed | `npm run test`: 5 files, 11 tests |
| Browser E2E | Passed for implemented suite | `npm run test:e2e`: 19 passed, 0 failed, 1 desktop skip for the mobile-only assertion |
| PostgreSQL/Flyway | Passed | PostgreSQL 17.10; fresh V1–V35 and V34→V35 upgrade verified |
| Packaged application startup | Passed | Flyway validation, JPA initialization, Tomcat startup, and health check succeeded |
| Render/Vercel | Not performed | Deployment and remote changes were not authorized |

## Behavior verified

- Admin authorization is derived from the authenticated role and university; student access to `/api/v1/admin/aura/**` is rejected.
- Readiness returns structured blockers and prevents generation for an incomplete term.
- Generation uses persisted PostgreSQL scheduling facts, a selected constraint profile, a deterministic seed, and a deterministic input checksum.
- A complete generated result is persisted atomically only for an active, current-revision run.
- Published versions remain immutable; CSV export produces a download.
- A published version can be cloned to a draft, selected immediately, previewed, safely moved, pinned, unpinned, and compared.
- Personal timetables expose only the authenticated student's published schedule.
- Empty imports fail with an actionable message; valid CSV imports reach preview and row validation.
- The browser harness starts the packaged Spring Boot application against an isolated PostgreSQL database and exercises desktop and mobile projects.

## PostgreSQL evidence

The local PostgreSQL 17.10 cluster applied all 35 successful Flyway migrations through `V35__add_aura_generation_profiles.sql`. The packaged application initialized Hibernate/JPA and embedded Tomcat. A separate pre-V35 database was migrated from V34 to V35 without modifying prior migrations.

The authenticated runtime fixture completed one generation run and persisted timetable versions and sessions. Browser tests then exercised readiness, version workflows, import validation, export, personal timetable access, and admin/student authorization boundaries.

## Skipped tests

The AURA-specific skip is `AuraDefaultProfileStartupIntegrationTest`, guarded by Testcontainers because Docker is unavailable. Equivalent startup and migration coverage ran directly against local PostgreSQL. Twelve additional existing integration tests from other CampusOne modules are also Docker-gated.

## Remaining verification gaps

- The completion ledger still identifies genuine product gaps, including exhaustive setup lifecycle screens, complete independent clash-detector parity, automatic localized repair, instructor timetable views, and the full deterministic backtest matrix.
- The current Playwright suite covers critical connected flows but not every requested browser journey.
- The 5,000-occurrence run completed with zero hard score but used substantial memory; see [AURA_BENCHMARK_REPORT.md](AURA_BENCHMARK_REPORT.md).
- Production deployment and Render/Vercel smoke verification were not performed.
