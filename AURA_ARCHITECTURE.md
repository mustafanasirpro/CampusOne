# AURA Architecture

AURA is CampusOne's university-scoped timetable planning module. It lives inside
the existing Spring Boot API and React application; it is not a separate service.

## System boundary

```text
React workbench and personal views
        |
        | JWT-authenticated REST
        v
Spring controllers -> authorization -> application services
        |                                  |
        |                                  +-> Timefold planning model
        v
JDBC repositories -> PostgreSQL/Flyway -> audit and notifications
```

Administrators operate under `/api/v1/admin/aura`. Student and linked-instructor
views use `/api/v1/aura`. The backend derives user and university scope from the
authenticated principal; clients do not establish trusted scope.

## Backend responsibilities

| Layer | Primary responsibility |
|---|---|
| Controllers | Request validation, authenticated identity, HTTP contracts |
| Authorization | Admin capability and university boundary enforcement |
| Services | Readiness, generation, publication, import, resolution, repair, scenarios, export |
| JDBC repositories | Scoped SQL, optimistic state guards, atomic persistence |
| Solver domain | Planning lessons, rooms, timeslots, availability, travel facts |
| Constraint provider | Hard, medium, and soft timetable scoring |
| Clash detector | Independent persisted-schedule validation |
| Flyway | Additive PostgreSQL schema and scheduling-revision functions |

Generation reads normalized PostgreSQL scheduling facts, builds an
`AuraTimetableSolution`, runs Timefold, and persists a new draft only after the
result is complete and still matches the term input revision.

## Frontend responsibilities

- `/admin/aura` hosts setup, import, registration, readiness, generation,
  version, resolution, scenario, analytics, and audit workflows.
- Personal and instructor timetable routes expose published data only.
- `src/api/auraApi.ts` is the single frontend API boundary.
- `src/types/aura.ts` contains the matching response and request contracts.
- Components under `src/components/aura` reuse the CampusOne design system.

## Reliability model

- One queued or running generation is permitted per term.
- Scheduling input mutations increment the term revision.
- Draft mutations use version/state guards; published versions are immutable.
- Publication revalidates revision, occurrence integrity, and hard clashes.
- Imports apply transactionally after preview and validation.
- Repairs and emergency changes create reviewable drafts rather than mutating a
  published version.
- Audit and notification failures must not expose another university's data.

## Data and deployment

AURA uses PostgreSQL tables created by Flyway migrations `V25` onward. The module
uses Spring JDBC DTO mapping rather than JPA entities. `AURA_ENABLED` controls the
module as one unit and defaults to enabled. Production deployment remains part
of the existing Render backend and Vercel frontend.

See [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md), [API_DOCUMENTATION.md](API_DOCUMENTATION.md),
and [AURA_COMPLETION_LEDGER.md](AURA_COMPLETION_LEDGER.md) for the schema, route
catalog, and verified completion state.
