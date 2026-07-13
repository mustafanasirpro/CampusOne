# CampusOne Tasks

## Completed in the Lost & Found implementation pass

- [x] Add Lost & Found database migration.
- [x] Add backend entities, DTOs, repositories, mapper, service, image validation, and controller.
- [x] Extend R2 storage abstraction for Lost & Found images.
- [x] Add moderation target type `LOST_FOUND_ITEM`.
- [x] Add Lost & Found notification type and target types.
- [x] Integrate Lost & Found into the pending approval queue.
- [x] Add content-report validation for visible Lost & Found items.
- [x] Implement deterministic local matching with explainable scores.
- [x] Add frontend API client, types, routes, navigation, dashboard card, browse page, submit/edit forms, detail page, claims/matches page, moderation labels, and notification labels.
- [x] Add focused backend tests for Lost & Found service behavior, matching, and moderation authorization.
- [x] Run frontend lint and production build.

## Remaining verification tasks

- [ ] Browser-test `/lost-found`, `/lost-found/new`, `/lost-found/:itemId`, `/lost-found/:itemId/edit`, and `/lost-found/claims`.
- [ ] Browser-test moderator approval/rejection for `LOST_FOUND_ITEM`.
- [ ] Browser-test content report creation for a visible Lost & Found item.
- [ ] Browser-test R2 image upload in an environment with real R2 variables configured.
- [ ] Confirm production deployment after Render/Vercel environment variables are updated.
- [ ] Add optional admin statistics cards for Lost & Found if the admin dashboard layout should surface them directly.
- [ ] Add a scheduler job for batch archiving expired Lost & Found items; current public queries already hide expired items.

## Regression checklist before deployment

- [ ] `cd backend && mvn clean verify`
- [ ] `npm run lint`
- [ ] `npm run build`
- [ ] Confirm `.env`, `dist/`, `node_modules/`, `backend/target/`, logs, and generated junk are not staged.
- [ ] Confirm no secrets are printed, committed, or copied into documentation.
