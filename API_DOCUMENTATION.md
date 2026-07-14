# CampusOne API Documentation

This file summarizes project-specific API notes that complement Swagger/OpenAPI.
The generated OpenAPI document remains the source of truth when the backend is running.

## Lost & Found API

Base path:

```text
/api/v1/lost-found
```

All Lost & Found endpoints require authentication.

### Items

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/items` | Browse same-university, published, non-expired items with filters and pagination. |
| `POST` | `/items` | Submit a lost/found item as multipart form data with JSON `item` and optional image parts. |
| `GET` | `/items/my` | List the current user's own submissions across statuses. |
| `GET` | `/items/{itemId}` | Get contextual item detail with owner/public privacy rules. |
| `PATCH` | `/items/{itemId}` | Edit an owned item; material edits resubmit for review. |
| `DELETE` | `/items/{itemId}` | Soft-delete an owned item when no approved claim is active. |
| `PATCH` | `/items/{itemId}/close` | Close an owned published item. |
| `PATCH` | `/items/{itemId}/archive` | Archive an owned published or closed item. |
| `PATCH` | `/items/{itemId}/renew` | Renew/resubmit an owned eligible item for review. |
| `PATCH` | `/items/{itemId}/reopen` | Reopen a closed item or resubmit it for review when needed. |
| `PATCH` | `/items/{itemId}/resolve` | Mark an owned published lost report as recovered. |

### Claims

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/items/{itemId}/claims` | Submit private proof for a claim against a found item. |
| `GET` | `/items/{itemId}/claims` | List claims for an owned item or authorized admin/moderator. |
| `GET` | `/claims/my` | List claims related to the current user. |
| `PATCH` | `/claims/{claimId}/approve` | Reporter/admin approves a pending claim. |
| `PATCH` | `/claims/{claimId}/reject` | Reporter/admin rejects a pending claim. |
| `PATCH` | `/claims/{claimId}/cancel` | Claimant cancels a pending claim, or involved users cancel eligible approved claims. |
| `PATCH` | `/claims/{claimId}/handover/claimant` | Claimant confirms handover. |
| `PATCH` | `/claims/{claimId}/handover/reporter` | Reporter/admin confirms handover. |
| `PATCH` | `/claims/{claimId}/complete` | Reporter/admin completes an approved handover directly when appropriate. |

### Matches

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/matches/my` | List suggested matches involving the current user. |
| `GET` | `/items/{itemId}/matches` | List suggested matches for an owned item. |
| `PATCH` | `/matches/{matchId}/confirm` | Involved user confirms a suggested match. |
| `PATCH` | `/matches/{matchId}/reject` | Involved user rejects a suggested match. |

### Admin/statistics

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/admin/stats` | Return aggregate Lost & Found counts for authorized admins. |

Lost & Found item approval/rejection uses the existing admin moderation endpoints:

```text
GET   /api/v1/admin/moderation/pending?targetType=LOST_FOUND_ITEM
PATCH /api/v1/admin/moderation/LOST_FOUND_ITEM/{targetId}/approve
PATCH /api/v1/admin/moderation/LOST_FOUND_ITEM/{targetId}/reject
```

## Privacy Notes

- Public item DTOs do not include reporter email, phone, credentials, claim proof, or reviewer notes.
- Owner and claim DTOs expose private proof only to involved users.
- Same-university isolation is enforced server-side.
- Lost & Found is intentionally not exposed through public unauthenticated global search.

## AURA Timetable API

Base path:

```text
/api/v1/admin/aura
```

AURA endpoints are admin-only and use the authenticated admin's university as
the trusted boundary. Clients do not provide a trusted university ID except
when creating an academic term, where the backend still verifies admin access.

### Setup data

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/terms` | Create an academic term for timetable planning. |
| `GET` | `/terms` | List AURA academic terms. |
| `POST` | `/programs` | Create a program under a university. |
| `GET` | `/programs` | List programs. |
| `POST` | `/batches` | Create a batch/cohort. |
| `GET` | `/batches` | List batches. |
| `POST` | `/sections` | Create a section. |
| `GET` | `/sections` | List sections. |
| `POST` | `/instructors` | Create an instructor profile for scheduling. |
| `GET` | `/instructors` | List instructors. |
| `POST` | `/rooms` | Create a room/lab/hall. |
| `GET` | `/rooms` | List rooms. |
| `POST` | `/timeslots` | Create a weekly timeslot. |
| `GET` | `/timeslots` | List timeslots. |
| `POST` | `/offerings` | Create a course offering for a term. |
| `GET` | `/terms/{termId}/offerings` | List term offerings. |
| `POST` | `/meeting-requirements` | Add required weekly sessions for an offering. |
| `GET` | `/offerings/{offeringId}/meeting-requirements` | List offering requirements. |

### Generation and review

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/terms/{termId}/readiness` | Validate whether a term has enough setup data to generate a timetable. |
| `POST` | `/terms/{termId}/runs` | Start an asynchronous Timefold timetable generation run. |
| `GET` | `/runs/{runId}` | Read generation status, score, and result metadata. |
| `POST` | `/runs/{runId}/cancel` | Cancel a queued or running generation attempt. |
| `GET` | `/terms/{termId}/versions` | List generated timetable versions for a term. |
| `GET` | `/versions/{versionId}` | Read one timetable version summary. |
| `POST` | `/versions/{versionId}/publish` | Publish a generated version and archive any previous published version. |
| `GET` | `/versions/{versionId}/sessions` | List scheduled sessions for a timetable version. |
| `GET` | `/versions/{versionId}/clashes` | List detected hard clashes for a timetable version. |
| `POST` | `/sessions/{sessionId}/move-preview` | Preview a manual room/timeslot move and its resulting clashes. |
| `PATCH` | `/sessions/{sessionId}/move` | Apply a manual move and refresh clash records. |
| `GET` | `/terms/{termId}/metrics` | Return aggregate generation/version/clash metrics for admin dashboards. |

AURA stores timetable data in normalized PostgreSQL tables managed by Flyway
and keeps generated versions immutable apart from publish state and explicit
manual moves. The solver uses Timefold Solver Community Edition in-process; no
external scheduling service, queue, or database is required.
