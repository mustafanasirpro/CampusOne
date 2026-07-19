# AURA Verification Report

This report records the evidence collected for the current local AURA
implementation. It deliberately separates automated and local PostgreSQL
verification from browser and production checks that have not been performed.

## Verification snapshot

| Check | Result | Evidence |
|---|---|---|
| Backend verification | Passed | `mvn clean verify`: 581 tests, 0 failures, 0 errors, 13 skipped |
| Frontend lint | Passed | `npm run lint` |
| Frontend production build | Passed | `npm run build`; TypeScript project build and Vite production bundle completed |
| Flyway migration validation | Passed | Disposable PostgreSQL 17 applied V1–V34 from empty and separately upgraded a V33 schema to V34 |
| Packaged application startup | Passed | JPA initialized, embedded Tomcat started, and `Started CampusOneApplication` was logged |
| Docker/Testcontainers | Not run | Docker Desktop is unavailable in the local environment; PostgreSQL was verified directly instead |
| Browser E2E | Not run | The repository does not currently configure Playwright or another frontend E2E runner |
| Render production smoke | Not run | Deployment and remote changes were explicitly out of scope for this local pass |

## Focused coverage added in this pass

- Generation results are persisted in a single transaction only while the run
  is active and its captured term data revision is current.
- Queued, running, completed, failed, and cancelled generation transitions are
  conditional; abandoned active runs are expired and the database permits only
  one active generation per term.
- Publication verifies exact occurrence integrity, current input revision,
  draft state, and unresolved hard clashes before atomically superseding the
  prior published version.
- Readiness capacity uses required weekly occurrences and excludes inactive or
  non-instructional inputs.
- Resolution application uses case and registration versions to reject stale
  concurrent writes, and rejection auditing no longer trusts a client-provided
  suggestion identifier.
- Timetable publication notifications deduplicate recipients and exclude the
  publishing actor.
- The version workbench uses the existing move preview/apply API and supports
  pin and unpin actions without duplicating backend logic.

## PostgreSQL startup evidence

The packaged backend was launched against an empty disposable PostgreSQL 17
database. Flyway reported 34 successfully validated and applied migrations,
including `V34__complete_aura_input_revision_triggers.sql`. A separate database
was migrated to V33 first and then started again without a target override;
Flyway detected version 33 and applied V34 successfully. Hibernate schema
validation and `EntityManagerFactory` initialization succeeded on both paths,
and Tomcat started on the configured local smoke-test port.

Database inspection also confirmed the AURA scheduling-input revision triggers
and the partial unique index `uk_aura_generation_runs_active_term`.

## Remaining verification gaps

- Docker-specific Testcontainers execution is still unavailable locally.
- Frontend component and Playwright E2E frameworks are not configured.
- Browser workflows, large deterministic backtests, memory/load benchmarks,
  and Render production verification remain open gates in
  [`AURA_COMPLETION_LEDGER.md`](AURA_COMPLETION_LEDGER.md).
- This report does not claim that the incomplete automatic repair, complete
  analytics, instructor view, or all remaining setup CRUD requirements are
  implemented.
