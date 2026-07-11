# Contributing to CampusOne

## Welcome

Thank you for taking the time to improve CampusOne. This repository is a full-stack campus community platform with a React/Vite frontend and a Spring Boot backend. Contributions should keep the product reliable, secure, polished, and easy to maintain.

This guide explains how to work in the repository, what quality bar changes must meet, and how to prepare a pull request that is straightforward to review.

## Code of Conduct

CampusOne does not currently include a separate `CODE_OF_CONDUCT.md`. Until one is added, contributors are expected to follow a simple standard:

- be respectful, patient, and constructive;
- assume good intent, but communicate clearly;
- keep discussions focused on the work and the product;
- avoid harassment, personal attacks, discriminatory language, or hostile behavior;
- give feedback that helps the project improve.

Maintainers may close issues or pull requests that do not meet this standard.

## Before You Start

Read the project documentation before opening an issue or changing code:

- [README.md](README.md) for product scope, architecture, setup, and deployment notes
- [backend/README.md](backend/README.md) for backend-specific details
- [docs/LOCAL_SETUP.md](docs/LOCAL_SETUP.md) for local setup notes

### Prerequisites

| Tool | Required for | Notes |
|---|---|---|
| Node.js `20.19+` or `22.12+` | Frontend | Required by the Vite toolchain |
| npm | Frontend | Use the committed `package-lock.json` |
| Java 21 | Backend | Matches `backend/pom.xml` |
| Maven 3.9+ | Backend | Used for build and verification |
| PostgreSQL | Backend runtime | Local database for Spring Boot |
| Docker | Optional tests | Needed for Testcontainers-backed integration tests |

### Project at a glance

CampusOne includes authentication, role/permission checks, notes and past papers, marketplace listings, events, discussions, internships, notifications, search, gamification, AI study tools, moderation, password reset, email delivery, and production deployment configuration.

Changes often cross the frontend/backend boundary. When they do, update both sides together and verify the API contract.

## Local Development

### Frontend setup

From the repository root:

```bash
npm ci
```

Create a local frontend environment file:

```bash
cp .env.example .env
```

PowerShell:

```powershell
Copy-Item .env.example .env
```

Local API target:

```text
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Start the frontend:

```bash
npm run dev
```

Vite serves the app at `http://localhost:5173` by default.

### Backend setup

Create a local PostgreSQL database:

```sql
CREATE ROLE campusone WITH LOGIN PASSWORD 'choose-a-local-password';
CREATE DATABASE campusone OWNER campusone;
```

Create the backend environment file:

```bash
cd backend
cp .env.example .env
```

PowerShell:

```powershell
cd backend
Copy-Item .env.example .env
```

Set at minimum:

```text
DB_URL=jdbc:postgresql://localhost:5432/campusone
DB_USERNAME=campusone
DB_PASSWORD=<your-local-password>
JWT_SECRET=<standard-base64-32-byte-secret>
```

Generate a suitable JWT secret with:

```bash
openssl rand -base64 32
```

Start the backend:

```bash
cd backend
mvn spring-boot:run
```

The Maven run goal activates the local profile. The backend starts on `http://localhost:8080` by default.

### Useful local URLs

| Resource | URL |
|---|---|
| Frontend | `http://localhost:5173` |
| Backend API | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/api/v1/swagger-ui` |
| OpenAPI | `http://localhost:8080/api/v1/openapi` |

## Project Structure

```text
CampusOne/
├── backend/                 Spring Boot backend
│   ├── pom.xml              Maven configuration
│   ├── README.md            Backend-specific documentation
│   └── src/
│       ├── main/java/com/campusone/
│       │   ├── auth/        Authentication and password reset
│       │   ├── note/        Notes, uploads, approval, search support
│       │   ├── marketplace/ Marketplace listings and images
│       │   ├── moderation/  Reports and moderation workflows
│       │   ├── security/    JWT, CORS, filters, permissions
│       │   └── ...          Other product modules
│       └── main/resources/db/migration/
├── src/                     React frontend
│   ├── api/                 API clients
│   ├── auth/                Auth context and route guards
│   ├── components/          Shared and feature components
│   ├── pages/               Route-level pages
│   ├── routes/              Router and path definitions
│   ├── types/               TypeScript domain types
│   └── utils/               Shared helpers
├── public/                  Static frontend assets
├── docs/                    Supporting documentation
├── .env.example             Frontend environment example
├── package.json             Frontend scripts and dependencies
└── vercel.json              SPA rewrite configuration
```

## Development Workflow

1. Sync with the latest main branch.
2. Create a focused branch for one feature, bug, or documentation change.
3. Keep the diff small enough to review thoughtfully.
4. Update tests and documentation alongside behavior changes.
5. Run the relevant quality gates before opening a pull request.
6. Include screenshots or screen recordings for visible UI changes.

Recommended branch names:

```text
feat/notes-search-ranking
fix/reset-password-email
chore/update-docs
```

Keep commits logical and reviewable. Avoid mixing unrelated frontend, backend, and documentation changes unless the feature requires it.

## Coding Standards

### Frontend: React and TypeScript

- Use TypeScript types from `src/types/` or colocated feature types when appropriate.
- Keep API calls in `src/api/`; do not call `fetch` directly from feature pages unless a clear abstraction already exists.
- Keep route-level orchestration in `src/pages/` and reusable UI in `src/components/`.
- Prefer small, readable components over deeply nested render blocks.
- Preserve loading, empty, error, and success states.
- Avoid duplicate API requests and accidental infinite `useEffect` loops.
- Do not introduce Redux, Zustand, or a UI library unless the project intentionally adopts one.
- Do not leave `console.log`, debug UI, placeholder data, or fake upload behavior.

### Backend: Java and Spring Boot

- Follow the existing package-by-domain structure under `com.campusone`.
- Keep controllers thin: validate requests, call services, return DTOs.
- Put transactions, permissions, ownership checks, and business rules in services.
- Keep JPA entities behind service/repository boundaries; return response DTOs from controllers.
- Prefer constructor injection.
- Do not use Lombok or MapStruct; the project currently uses explicit classes and manual mappers.
- Keep APIs backward-compatible when possible. If request/response fields change, update frontend clients and tests.
- Use clear validation errors and consistent exception handling.

### API consistency

- Keep frontend API paths aligned with backend controllers.
- Do not guess endpoint names or request fields.
- Keep pagination, sorting, enum values, and status fields consistent across backend DTOs and frontend types.
- Preserve public visibility rules: pending or rejected content must not appear in public lists/search.

### Accessibility and product copy

CampusOne should feel like a polished production application.

- Use semantic headings and accessible form labels.
- Keep buttons and links descriptive.
- Preserve visible focus states.
- Make layouts responsive on mobile and desktop.
- Avoid backend jargon in user-facing copy: do not show terms like JWT, CORS, stack trace, server exception, or raw tokens to users.
- Prefer calm, clear messages: “We couldn’t load this page. Please try again.”

## Commit Message Convention

Use Conventional Commits where practical:

```text
feat: add marketplace image preview validation
fix: prevent pending notes from appearing in public search
docs: improve environment variable guidance
refactor: simplify notes approval mapper
test: cover resend sender validation
chore: update dependencies
```

Common prefixes:

| Prefix | Use for |
|---|---|
| `feat` | User-facing features or new capabilities |
| `fix` | Bug fixes |
| `docs` | Documentation-only changes |
| `refactor` | Code restructuring without behavior changes |
| `test` | Test additions or updates |
| `chore` | Tooling, maintenance, dependency updates |

## Pull Requests

A good pull request is easy to understand, easy to test, and honest about risk.

### Before opening a PR

- [ ] Keep the change focused on one concern.
- [ ] Update or add tests for behavior changes.
- [ ] Update docs when setup, behavior, APIs, permissions, or env variables change.
- [ ] Run the relevant quality gates.
- [ ] Confirm no secrets, generated build outputs, or local files are included.
- [ ] Add screenshots for UI changes.

### PR description template

```markdown
## Summary

What changed and why?

## Testing

- [ ] npm run lint
- [ ] npm run build
- [ ] cd backend && mvn clean verify

## Screenshots

Add screenshots or screen recordings for UI changes.

## Notes for reviewers

Mention migrations, env vars, deployment concerns, or known tradeoffs.
```

## Testing

### Frontend

Run from the repository root:

```bash
npm run lint
npm run build
```

There is currently no `npm test` script. If you add frontend tests in the future, add the script and document it in this guide.

### Backend

Run from `backend/`:

```bash
mvn clean verify
```

This compiles the backend, runs the test suite, and builds the Spring Boot artifact. Some integration tests use Testcontainers and depend on Docker availability.

### Manual verification

For user-facing changes, manually verify the affected journey. Examples:

- login, signup, logout, forgot password, and reset password;
- notes submission, PDF upload, approval, search, bookmark/rating, download;
- marketplace listing creation, image upload, edit, detail, and deletion;
- events, discussions, internships, notifications, and admin/moderation flows;
- mobile layouts and keyboard accessibility.

## Security

Never commit secrets or sensitive local files.

Do not commit:

- `.env` or `.env.*` files, except committed examples;
- API keys;
- passwords;
- JWT secrets;
- refresh tokens or access tokens;
- database URLs containing credentials;
- R2 or Resend credentials;
- logs containing user data or secrets.

Do not log sensitive values. Password reset tokens must only appear in the email link and must never be returned by the API or written to production logs.

If a change touches authentication, authorization, uploads, email, or moderation, treat it as security-sensitive and add tests.

## Database and Migrations

CampusOne uses Flyway migrations under:

```text
backend/src/main/resources/db/migration/
```

Rules for schema changes:

- Create a new migration for every schema change.
- Do not edit, rename, reorder, or delete migrations that may already have run.
- Keep migrations additive and safe where possible.
- Backfill data deliberately and document assumptions.
- Ensure Hibernate validation still passes.
- Run `mvn clean verify` after migration changes.

Never disable Flyway validation to work around a schema problem. Fix the migration history safely.

## Authentication and Permissions

Do not break these flows:

- registration;
- login;
- logout;
- refresh-token rotation;
- forgot password;
- reset password;
- JWT authentication;
- admin/moderator permissions;
- content approval rules;
- protected routes.

Backend enforcement is required. Frontend checks are useful for UX, but they are not security boundaries.

When adding a new protected action:

1. enforce it in the backend service/controller layer;
2. expose clear permission state if the UI needs it;
3. hide or disable unavailable actions in the frontend;
4. test both allowed and forbidden paths.

## UI and UX Standards

CampusOne should feel calm, modern, and trustworthy.

- Keep spacing, typography, and card layouts consistent with existing pages.
- Use clear empty, loading, and error states.
- Avoid raw backend errors and internal implementation details in the UI.
- Do not show raw UUIDs or storage metadata to normal users.
- Use friendly labels for form fields.
- Validate forms before submission when possible, then display backend validation cleanly.
- Keep mobile layouts usable and avoid horizontal overflow.
- Use accessible color contrast and meaningful alt text for images.

## Performance

- Avoid unnecessary rerenders and expensive work inside render paths.
- Avoid duplicate API requests.
- Use pagination where the backend supports it.
- Avoid N+1 backend queries.
- Keep bundle growth intentional; do not add heavy dependencies casually.
- Add database indexes for new high-traffic filters/search paths when needed.

## Documentation

Update documentation when a change affects:

- local setup;
- environment variables;
- deployment steps;
- API behavior;
- authentication or permissions;
- upload/storage behavior;
- migrations;
- user-facing workflows.

Relevant docs:

- [README.md](README.md)
- [backend/README.md](backend/README.md)
- [docs/LOCAL_SETUP.md](docs/LOCAL_SETUP.md)
- Swagger/OpenAPI annotations for backend endpoints

## Reporting Bugs

A useful bug report includes:

```markdown
## What happened?

Describe the issue clearly.

## Expected behavior

What should have happened?

## Steps to reproduce

1.
2.
3.

## Environment

- Browser / OS:
- Frontend URL:
- Backend profile or environment:

## Evidence

Screenshots, logs, request IDs, or failing test output.
```

Please remove secrets, tokens, passwords, and private user data from logs before sharing them.

## Requesting Features

A strong feature request explains the product need, not just the implementation idea.

```markdown
## Problem

What user problem should CampusOne solve?

## Proposed solution

What should change?

## Alternatives considered

What other approaches did you consider?

## Impact

Who benefits? What modules are affected?

## Risks

Security, performance, migration, or UX concerns.
```

## Final Contributor Checklist

Before submitting, confirm:

- [ ] Frontend lint passes: `npm run lint`
- [ ] Frontend build passes: `npm run build`
- [ ] Backend verification passes: `cd backend && mvn clean verify`
- [ ] Tests were added or updated for behavior changes
- [ ] Manual verification was completed for affected user flows
- [ ] Screenshots or recordings are included for UI changes
- [ ] Documentation is updated
- [ ] No secrets, tokens, passwords, or `.env` files are committed
- [ ] No debug code, `console.log`, temporary files, or generated artifacts are committed
- [ ] No unfinished TODOs or placeholder behavior remain
- [ ] UI copy is polished and user-friendly
- [ ] Accessibility and responsive behavior were checked
- [ ] Backend permissions and public visibility rules were verified
- [ ] Frontend routes and API clients still match backend contracts

Thank you for helping make CampusOne better. Aim for changes that are small, tested, secure, and kind to the next person who has to maintain them.
