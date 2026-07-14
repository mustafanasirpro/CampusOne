# CampusOne Test Plan

## Standard Quality Gates

Backend:

```bash
cd backend
mvn clean verify
```

Frontend:

```bash
npm run lint
npm run build
```

## Lost & Found Automated Coverage

Focused backend tests added:

- `LostFoundServiceTest`
  - item creation derives reporter/university and starts as pending review;
  - item type cannot be changed after creation;
  - pending claim creation keeps a found item published;
  - claim approval moves a found item to claim-in-progress.
- `LostFoundMatchingServiceTest`
  - strong matches create explainable suggested matches;
  - weak matches do not create rows;
  - rejected matches are not overwritten by recalculation.
- `ContentApprovalServiceTest`
  - Lost & Found approval uses moderator authorization;
  - existing note approval remains admin-gated.

## Manual Lost & Found Regression Checklist

Run these in a local or staging environment with authentication and R2 configured:

1. Student submits a lost item with no image.
2. Student submits a found item with up to three JPG/PNG/WebP images.
3. Submitted items appear in "My submissions" as pending.
4. Pending items do not appear in public browse.
5. Moderator/admin approves an item from the existing moderation queue.
6. Approved item appears in same-university browse.
7. Other-university users cannot view or claim the item.
8. User submits a private claim against a found item.
9. Reporter approves/rejects the claim.
10. Claimant and reporter handover confirmations resolve the found item.
11. Suggested matches appear for involved owners after approval.
12. Involved users can confirm or reject a suggested match.
13. Owner can close, archive, reopen, renew, resolve, or delete only where allowed.
14. Content report flow accepts visible Lost & Found items and rejects inaccessible UUIDs.
15. Notification links open the correct Lost & Found routes.

## Verification Boundaries

- Do not claim production verification unless the deployed Render/Vercel services were actually tested.
- Do not claim R2 upload verification unless a real configured R2 environment was used.
- Docker-dependent Testcontainers tests may be skipped locally when Docker is unavailable.

## AURA Automated Coverage

Focused backend tests added:

- `AuraReadinessValidatorTest`
  - reports missing setup data clearly;
  - marks a term ready when rooms, timeslots, instructors, offerings, and requirements exist;
  - warns when required sessions exceed the simple room/timeslot capacity envelope.
- `AuraClashDetectorTest`
  - detects room, instructor, and section clashes from persisted session DTOs;
  - detects overlaps even when sessions use different timeslot IDs but
    overlapping clock ranges;
  - previews manual moves without mutating the original session list.
- `AuraServiceTest`
  - blocks concurrent generation runs for the same term;
  - prevents non-draft versions from being published;
  - prevents publishing while unresolved hard clashes remain.
- `AuraModuleBeanWiringTest`
  - verifies AURA controller, service, repository, readiness, solver, and clash
    beans are registered together under the production-default enabled flag.
- `AuraDefaultProfileStartupIntegrationTest`
  - Docker/Testcontainers-gated default-profile startup regression test for
    Flyway, HTTP startup, health response, and AURA bean loading.
- `AuraAuthorizationServiceTest`
  - requires admin permissions for AURA management;
  - accepts existing admin-upload authorization fallback.
- `AuraSolverServiceTest`
  - verifies the Timefold solver assigns sessions without hard clashes for a small fixture.

## Manual AURA Regression Checklist

Run these in a local or staging environment as an admin:

1. Open `/admin/aura`.
2. Create an academic term.
3. Add or seed rooms, timeslots, instructors, sections, offerings, and meeting requirements through the admin API.
4. Confirm readiness changes from blocked to ready.
5. Start a generation run and watch it move from queued/running to completed.
6. Open the generated timetable version and inspect scheduled sessions.
7. Confirm hard clashes are absent or listed with clear messages.
8. Publish a generated version and verify only one version is published.
9. Preview and apply a manual room/timeslot move.
10. Confirm sessions and clash counts refresh after the move.
