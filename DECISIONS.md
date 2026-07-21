# CampusOne Decisions

## Lost & Found Implementation Decisions

| Area | Decision |
|---|---|
| Architecture | Implement Lost & Found as a native modular-monolith backend domain and React SPA module. |
| Authentication | Require CampusOne authentication for every Lost & Found endpoint and page. |
| University scope | Derive immutable `university_id` from the reporter's current `StudentProfile`; never accept it from the client. |
| Item model | Use one `lost_found_items` table for both `LOST` and `FOUND` reports. |
| Initial status | New items always begin as `PENDING_REVIEW`, including admin submissions. |
| Moderation | Reuse the existing generic approval endpoints with target type `LOST_FOUND_ITEM`. |
| Moderator permission | Lost & Found approval uses active moderator/admin authorization; unrelated approval targets keep their existing admin-only behavior. |
| Images | Allow up to three JPG, PNG, or WebP images per report; store image files in existing R2/S3-compatible storage and metadata in PostgreSQL. |
| Claims | Claims apply to `FOUND` items, keep ownership proof private, and move items to `CLAIM_IN_PROGRESS` only after claim approval. |
| Handover | Both claimant-side and reporter-side confirmation endpoints are available; completion resolves the item when both sides confirm. |
| Matching | Use deterministic local Java scoring; no paid AI, OCR, geocoding, vector database, or external search service. |
| Match threshold | Create or refresh suggestions at score `>= 60`. |
| Candidate cap | Recalculate against at most 500 same-university, opposite-type, active candidates within a 30-day date window. |
| Global search | Lost & Found is not added to unauthenticated global search because the current global search endpoint is public and Lost & Found requires authenticated same-university isolation. Module-local search is implemented. |
| Expiry visibility | Browse queries exclude expired items even if a scheduler has not archived them yet. |

## Operational Decisions

- Keep R2 optional at startup. Upload attempts fail cleanly if storage is not configured.
- Do not expose reporter contact details, storage credentials, claim proof, reviewer notes, or internal moderation state in public DTOs.
- Do not add any new paid provider or external service.
- Do not commit `.env`, build output, generated logs, storage keys, database passwords, JWT secrets, or email provider secrets.

## AURA Decisions

| Area | Decision |
|---|---|
| Solver | Run Timefold Community Edition in-process and deterministically pre-construct eligible assignments before local optimization. |
| Reproducibility | Persist a generation seed, selected profile, scheduling revision, and deterministic input checksum. |
| Profiles | Keep validated university-scoped weights in PostgreSQL; provide balanced, compact, room-efficient, instructor-friendly, and repair defaults. |
| Authorization | Derive admin capability and university scope from the authenticated backend identity; never trust client-supplied scope. |
| Version safety | Persist only complete current-revision runs, keep published schedules immutable, and edit through cloned drafts. |
| Verification | Use local PostgreSQL 17 for migration/runtime checks when Docker is unavailable and Playwright for browser-visible critical workflows. |
