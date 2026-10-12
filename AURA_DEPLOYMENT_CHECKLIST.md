# AURA Deployment Checklist

Production deployment is a separately authorized operation. Completing this
checklist locally does not mean Render or Vercel was changed.

## Before deployment

- [ ] Review the complete unstaged diff and migration order.
- [ ] Confirm no applied migration was edited.
- [ ] Run `mvn clean verify` from `backend`.
- [ ] Run `npm run lint`, `npm run test`, and `npm run build`.
- [ ] Run `npm run test:e2e` against isolated PostgreSQL.
- [ ] Verify the supported upgrade path and a fresh migration.
- [ ] Run `git diff --check`.
- [ ] Confirm AURA-specific skipped tests have a documented environment reason.
- [ ] Review [AURA_COMPLETION_LEDGER.md](AURA_COMPLETION_LEDGER.md) and known
  limitations.

## Render backend

- [ ] Use Java 21 and the repository's Maven build command.
- [ ] Preserve existing database, JWT, CORS, storage, and email secrets.
- [ ] Set `AURA_ENABLED=true` unless intentionally disabling the complete module.
- [ ] Verify the database user can run the next additive Flyway migration.
- [ ] Confirm startup applies/validates migrations and reaches healthy status.
- [ ] Check logs for migration, Hibernate/JDBC mapping, solver, or security errors
  without printing secrets.

## Vercel frontend

- [ ] Build with the production `VITE_API_BASE_URL`.
- [ ] Preserve SPA rewrites for direct navigation.
- [ ] Confirm `/admin/aura` and personal/instructor timetable routes load after
  refresh.

## Authorized production smoke test

- [ ] Admin sign-in and AURA authorization.
- [ ] Term/setup reads and readiness.
- [ ] Generation status and draft retrieval.
- [ ] Clash/session/version views.
- [ ] Student and instructor published timetable isolation.
- [ ] Import preview only with non-sensitive test data.
- [ ] One non-destructive export.
- [ ] Cross-university guessed identifier rejection.
- [ ] Health, latency, and logs after smoke testing.

## Rollback awareness

Do not run Flyway clean or edit applied migrations. Application rollback must
remain compatible with the migrated schema; database correction uses a new
forward migration. Preserve the prior published timetable while reviewing a new
draft.
