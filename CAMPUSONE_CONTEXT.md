# CampusOne Context

CampusOne is a production-oriented full-stack campus platform with:

- React, TypeScript, Vite, Tailwind CSS, and React Router in the repository root.
- Spring Boot, Java 21, Spring Security, JWT authentication, Spring Data JPA, PostgreSQL, and Flyway in `backend/`.
- Cloudflare R2/S3-compatible storage through the existing backend storage abstraction.
- In-app notifications, moderation approval queues, content reports, and Swagger/OpenAPI documentation.

## Current Lost & Found Context

The native Lost & Found module is implemented as a CampusOne domain module rather than a separate app.

Backend package:

```text
backend/src/main/java/com/campusone/lostfound
```

Frontend files:

```text
src/api/lostFoundApi.ts
src/types/lostFound.ts
src/components/lost-found/
src/pages/LostFound*.tsx
```

Flyway migration:

```text
backend/src/main/resources/db/migration/V21__create_lost_found_module.sql
```

## Privacy and Security Rules

- Every Lost & Found API route requires authentication.
- Reporter and university are derived from the authenticated user; clients cannot submit either value.
- Public browse returns same-university, published, non-expired, non-deleted items only.
- Owners can view their own reports in every lifecycle state.
- Claim proof and reviewer notes are private to involved users and authorized moderators/admins.
- Suggested matches are explainable hints only; they do not prove ownership or complete handover.
- Images are stored in R2/S3-compatible storage; PostgreSQL stores metadata only.

## Cost Boundary

Lost & Found adds no new vendor dependency. It uses the existing backend, PostgreSQL/Neon, Cloudflare R2, in-app notifications, moderation services, and deterministic Java matching.

## Current AURA Context

AURA is CampusOne's in-process university timetable module. Its admin API is under `/api/v1/admin/aura`, while authenticated student capability, registration, and personal-timetable endpoints are under `/api/v1/aura`. Scheduling data is university-scoped and stored in PostgreSQL through Flyway migrations V25–V36. Timefold Solver Community Edition runs inside the Spring Boot process; it does not require a paid solver service.

The current workbench connects setup, readiness, constraint profiles, generation, versions, sessions, clash review, imports, registrations, resolution cases, operational rules, localized and emergency repair, scoped timetable views, analytics, audit, exports, and publication. Student and linked-instructor timetable routes derive identity from authentication. The authoritative distinction between verified behavior and unfinished product scope is maintained in `AURA_COMPLETION_LEDGER.md` and `AURA_TEST_REPORT.md`.
