# CampusOne

<div align="center">

**A modern campus community platform for notes, lost-and-found reports, timetable planning, discussions, events, internships, marketplace listings, notifications, gamification, and study support.**

Built as a full-stack production-style application with a React/Vite frontend,
a Spring Boot backend, PostgreSQL persistence, Cloudflare R2 object storage,
Resend password recovery email, and OpenAPI documentation.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white)](https://vite.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Cloudflare R2](https://img.shields.io/badge/Storage-Cloudflare%20R2-F38020?logo=cloudflare&logoColor=white)](https://developers.cloudflare.com/r2/)
[![License](https://img.shields.io/badge/License-Not%20specified-lightgrey)](#license)

</div>

---

## Overview

CampusOne brings core student workflows into one cohesive product:

- discover and submit study notes and past papers;
- report and recover lost or found campus items through moderated, privacy-safe flows;
- generate and review university timetables through AURA, CampusOne's automated scheduling workbench;
- ask questions, answer discussions, and follow campus activity;
- browse marketplace listings, internships, and events;
- receive notifications and track gamification progress;
- use a local AI study assistant for summaries, flashcards, quizzes, and plans.

The repository contains both applications:

| App | Location | Description |
|---|---|---|
| Frontend | [`src/`](src/) | React 19 + TypeScript + Vite single-page app |
| Backend | [`backend/`](backend/) | Spring Boot REST API with PostgreSQL, Flyway, JWT auth, R2 storage, and OpenAPI |

## Production Targets

| Service | Target |
|---|---|
| Frontend | `https://campusone.dev` |
| Backend | `https://campusone-backend-otc4.onrender.com` |
| Database | Neon PostgreSQL |
| File storage | Cloudflare R2 / S3-compatible storage |
| Email provider | Resend HTTPS API |

The frontend uses SPA routing. [`vercel.json`](vercel.json) rewrites deep links
such as `/reset-password?token=...` to the React app.

## Features

### Product experience

- Polished landing, login, signup, forgot-password, and reset-password flows
- Student dashboard with responsive navigation
- Profile, settings, preferences, skills, and activity surfaces
- Notes and past papers with PDF upload, search, bookmarks, ratings, and downloads
- Lost & Found reports with image upload, moderation approval, private claims, and suggested matches
- AURA timetable generation with setup data, Timefold-powered scheduling, clash detection, version publishing, and admin review
- Marketplace listings with image upload support
- Discussions and Q&A with voting, answers, and accepted answers
- Events with capacity and participation flows
- Internship discovery, posting, filters, and saves
- Notifications with read state and bulk actions
- Search across supported content modules
- Leaderboards, XP, badges, history, and progress
- AI study assistant with chat and generated study materials
- Admin and moderation workflows for reports and approval queues

### Backend capabilities

- Registration and login with BCrypt password hashing
- JWT access tokens and rotating hashed refresh-token sessions
- Secure password reset with hashed, one-time tokens and Resend email delivery
- Account lockout, replay protection, logout, and strict CORS/origin validation
- Flyway-managed PostgreSQL schema with Hibernate validation
- R2-backed uploads for note PDFs and marketplace images
- Approval workflow for user-submitted content
- Public visibility rules for approved content
- OpenAPI/Swagger documentation
- Consistent DTO-based API responses and error handling

## Modules

| Module | Implemented scope |
|---|---|
| Authentication | Register, login, refresh, logout, forgot/reset password, lockout |
| Academic core | Universities, departments, courses, reference data |
| Profiles | Current user, public profile, skills, preferences, visibility |
| Notes | PDF submissions, approval, search, filters, ratings, bookmarks, downloads |
| Lost & Found | Same-university reports, moderation approval, images, private claims, handover, deterministic matching |
| AURA Timetable Generator | Admin-only setup, readiness checks, Timefold solver runs, timetable versions, clash review, publish workflow |
| Marketplace | Listings, image metadata, filters, ownership, soft deletion |
| Discussions | Questions, answers, votes, accepted answers, pagination |
| Events | Creation, editing, participation, capacity, visibility |
| Internships | Posting, filters, saves, ownership |
| Notifications | User notifications, unread counts, read/unread, deletion |
| Search | Unified public search across supported content |
| Gamification | XP ledger, levels, badges, history, leaderboards |
| AI Study Assistant | Sessions, messages, generated content, usage records |
| Admin / Moderation | Reports, moderator authorization, actions, pending approvals |

## Tech Stack

| Layer | Technologies |
|---|---|
| Frontend | React 19, TypeScript 6, Vite 8, Tailwind CSS 4, React Router, Lucide React |
| Backend | Java 21, Spring Boot 3.5, Spring Web, Spring Security, Spring Data JPA, Spring JDBC |
| Database | PostgreSQL, Hibernate, Flyway |
| Optimization | Timefold Solver Community Edition for AURA timetable generation |
| Authentication | JWT access tokens, opaque refresh tokens, BCrypt |
| Storage | Cloudflare R2 through AWS SDK for Java v2 |
| Email | Resend HTTPS API, optional SMTP fallback |
| API docs | Springdoc OpenAPI, Swagger UI |
| Testing | JUnit 5, Mockito, MockMvc, Spring Security Test, Testcontainers |
| Tooling | Maven, npm, ESLint, TypeScript |

## Architecture

CampusOne is a modular monolith on the backend and a route-driven SPA on the
frontend.

```text
React / Vite SPA
      │
      ▼
Spring Boot REST API
      │
      ├── Controllers → request validation and DTO boundaries
      ├── Services    → transactions, permissions, business rules
      ├── Repositories→ Spring Data JPA / query logic
      └── Integrations→ R2 storage, Resend email, OpenAPI
      │
      ▼
PostgreSQL + Cloudflare R2
```

Backend source is organized by domain under `com.campusone`. Entities stay
behind service/repository boundaries; controllers return explicit response
DTOs. Database changes are versioned with Flyway.

## Repository Structure

```text
CampusOne/
├── backend/
│   ├── pom.xml
│   ├── README.md
│   └── src/
│       ├── main/
│       │   ├── java/com/campusone/
│       │   └── resources/db/migration/
│       └── test/java/com/campusone/
├── docs/
├── public/
├── src/
│   ├── api/
│   ├── auth/
│   ├── components/
│   ├── data/
│   ├── pages/
│   ├── routes/
│   ├── types/
│   └── utils/
├── .env.example
├── CONTRIBUTING.md
├── package.json
├── vercel.json
└── README.md
```

## Screenshots

No screenshots are currently committed to the repository. When product images
are added, place them under `docs/screenshots/` and reference them from this
section.

## Local Setup

### Prerequisites

- Java 21
- Maven 3.9 or newer
- PostgreSQL
- Node.js `20.19+` or `22.12+`
- npm

### 1. Create a local PostgreSQL database

Run from an administrator `psql` session, replacing the password:

```sql
CREATE ROLE campusone WITH LOGIN PASSWORD 'choose-a-local-password';
CREATE DATABASE campusone OWNER campusone;
```

### 2. Configure the backend

```bash
cd backend
cp .env.example .env
```

PowerShell:

```powershell
cd backend
Copy-Item .env.example .env
```

Edit `backend/.env`, set the database credentials, and provide a unique JWT
secret. Generate a suitable standard Base64 secret with:

```bash
openssl rand -base64 32
```

The secret must represent at least 32 random bytes. Do not reuse sample values
in production.

### 3. Configure the frontend

Copy the root frontend environment example:

```bash
cp .env.example .env
```

PowerShell:

```powershell
Copy-Item .env.example .env
```

Local frontend API target:

```text
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### 4. Install dependencies

```bash
npm ci
```

### 5. Run the applications

Terminal 1:

```bash
cd backend
mvn spring-boot:run
```

Terminal 2:

```bash
npm run dev
```

Default local URLs:

| App | URL |
|---|---|
| Frontend | `http://localhost:5173` |
| Backend API | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/api/v1/swagger-ui` |
| OpenAPI | `http://localhost:8080/api/v1/openapi` |

## Environment Variables

The backend loads `backend/.env` through Spring Boot's optional config import.
Operating-system variables take precedence. The frontend reads Vite variables
from the root `.env`.

### Frontend

| Variable | Required | Purpose |
|---|---:|---|
| `VITE_API_BASE_URL` | Yes | Backend API base URL, e.g. `http://localhost:8080/api/v1` locally or `https://campusone-backend-otc4.onrender.com/api/v1` on Vercel |

### Backend core

| Variable | Required | Purpose / default |
|---|---:|---|
| `SPRING_PROFILES_ACTIVE` | Local | Use `local` for development |
| `DB_URL` | Yes | PostgreSQL JDBC URL |
| `DB_USERNAME` | Yes | Runtime database user |
| `DB_PASSWORD` | Yes | Runtime database password |
| `SERVER_PORT` | Hosting | Optional Spring port override |
| `PORT` | Hosting | Platform port used when `SERVER_PORT` is absent |
| `JWT_SECRET` | Yes | Standard Base64 secret with at least 256 bits |
| `JWT_ISSUER` | No | Defaults to `campusone-backend` |
| `JWT_AUDIENCE` | No | Defaults to `campusone-api` |
| `JWT_ACCESS_TOKEN_TTL` | No | Defaults to `15m` |
| `REFRESH_TOKEN_TTL_DAYS` | No | Defaults to `7` |
| `AUTH_COOKIE_SECURE` | Production | Defaults to `true`; local profile uses `false` |
| `AUTH_COOKIE_SAME_SITE` | Production | Defaults to `None`; local profile uses `Strict` |
| `REFRESH_TOKEN_CLEANUP_INTERVAL` | No | Defaults to `24h` |
| `MAX_LOGIN_ATTEMPTS` | No | Defaults to `5` |
| `ACCOUNT_LOCK_MINUTES` | No | Defaults to `15` |

### Password reset and email

| Variable | Required | Purpose / default |
|---|---:|---|
| `PASSWORD_RESET_TOKEN_TTL_MINUTES` | No | Reset-link lifetime; defaults to `30` |
| `APP_FRONTEND_URL` | Production | Frontend base URL used in reset links, e.g. `https://campusone.dev` |
| `MAIL_PROVIDER` | Production | Set to `resend` for Render production email; defaults to `disabled` |
| `RESEND_API_KEY` | Production | Resend API key |
| `RESEND_FROM` | Production | Verified sender on `mail.campusone.dev`, e.g. `CampusOne <support@mail.campusone.dev>` |
| `RESEND_TIMEOUT` | No | Resend request timeout; defaults to `10s` |
| `MAIL_ENABLED` | SMTP fallback | Use only with `MAIL_PROVIDER=smtp` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP fallback | Optional SMTP fallback settings; not required for Resend mode |

Production password reset email uses Resend over HTTPS. Do not use
`onboarding@resend.dev` or `mail@campusone.dev` for production delivery.

### CORS, OpenAPI, and migrations

| Variable | Required | Purpose / default |
|---|---:|---|
| `APP_CORS_ALLOWED_ORIGINS` | Production | Comma-separated exact frontend origins |
| `CORS_ALLOWED_ORIGINS` | No | Legacy alias for exact-origin CORS |
| `OPENAPI_ENABLED` | No | Defaults to `true` |
| `AURA_ENABLED` | No | Enables the AURA timetable module; defaults to `true`. Set to `false` only for diagnostics or intentionally narrow tests |
| `FLYWAY_URL` | No | Migration JDBC URL; falls back to `DB_URL` |
| `FLYWAY_USERNAME` | No | Migration user; falls back to `DB_USERNAME` |
| `FLYWAY_PASSWORD` | No | Migration password; falls back to `DB_PASSWORD` |

### Storage and uploads

| Variable | Required | Purpose / default |
|---|---:|---|
| `STORAGE_PROVIDER` | Uploads | Set to `r2` to enable note PDF and marketplace image uploads |
| `R2_ENDPOINT` | R2 | Cloudflare R2 S3 API endpoint |
| `R2_ACCESS_KEY_ID` | R2 | R2 access key |
| `R2_SECRET_ACCESS_KEY` | R2 | R2 secret key |
| `R2_BUCKET` | R2 | Bucket for uploaded files |
| `R2_REGION` | No | Defaults to `auto` |
| `R2_PUBLIC_BASE_URL` | No | Optional public/custom-domain base URL; otherwise signed URLs are generated |
| `MAX_UPLOAD_SIZE_MB` | No | Maximum PDF upload size; defaults to `25` |
| `MARKETPLACE_MAX_IMAGES_PER_LISTING` | No | Defaults to `5` |
| `MARKETPLACE_MAX_IMAGE_SIZE_MB` | No | Defaults to `5` |
| `APP_LOST_FOUND_MAX_IMAGE_SIZE_MB` | No | Maximum Lost & Found image size; defaults to `5` |
| `ADMIN_MAX_UPLOADS_PER_DAY` | No | Defaults to `200` |
| `ADMIN_MAX_STORAGE_MB_PER_MONTH` | No | Defaults to `5000` |
| `GLOBAL_UPLOAD_STORAGE_CAP_MB` | No | Defaults to `8192` |
| `ADMIN_UPLOAD_EMAILS` | No | Optional comma-separated fallback admin emails |
| `STORAGE_DOWNLOAD_URL_TTL` | No | Defaults to `10m` |

Secrets and local `.env` files must never be committed.

## Deployment

### Vercel frontend

Set:

```text
VITE_API_BASE_URL=https://campusone-backend-otc4.onrender.com/api/v1
```

The repository includes [`vercel.json`](vercel.json) so browser refreshes and
email links work with React Router.

### Render backend

Minimum production values:

```text
APP_CORS_ALLOWED_ORIGINS=https://campusone.dev,https://www.campusone.dev,https://campus-one-ruby.vercel.app,http://localhost:5173,http://127.0.0.1:5173
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=None
APP_FRONTEND_URL=https://campusone.dev
MAIL_PROVIDER=resend
RESEND_API_KEY=<your-resend-api-key>
RESEND_FROM=CampusOne <support@mail.campusone.dev>
RESEND_TIMEOUT=10s
STORAGE_PROVIDER=r2
R2_ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=<secret>
R2_SECRET_ACCESS_KEY=<secret>
R2_BUCKET=<bucket-name>
R2_REGION=auto
```

Use Neon PostgreSQL for `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Use a
certificate-verified PostgreSQL connection string where required by the
environment.

## API Documentation

When the backend is running:

| Resource | URL |
|---|---|
| Swagger UI | `http://localhost:8080/api/v1/swagger-ui` |
| OpenAPI | `http://localhost:8080/api/v1/openapi` |
| Health | `http://localhost:8080/api/v1/health` |

For authenticated endpoints, log in through `POST /api/v1/auth/login`, then
use the returned bearer token in Swagger UI.

### AURA status

AURA currently ships as an admin timetable-generation workbench with normalized
setup records, readiness checks, Timefold-powered draft generation, timetable
versions, hard-clash detection, manual move preview/apply, metrics, and
publication controls. Import mapping for CSV/XLSX/PDF files, complete setup
CRUD screens, student-specific clash-resolution cases, and localized repair are
tracked as follow-up work and should not be treated as production-complete until
their APIs, UI, and fixtures are implemented and verified.

## Quality Checks

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

Some integration tests use Testcontainers and run when Docker is available.

## Contributing

Contributions are welcome once repository licensing and contribution terms are
finalized. Before opening a pull request:

1. Read [CONTRIBUTING.md](CONTRIBUTING.md).
2. Keep changes focused and aligned with the existing module structure.
3. Add or update tests for changed behavior.
4. Run backend and frontend quality checks.
5. Never commit credentials, `.env` files, build outputs, logs, or local data.

For larger changes, open an issue first so the design can be discussed.

## Additional Documentation

- [Backend README](backend/README.md)
- [Local setup notes](docs/LOCAL_SETUP.md)
- [Contributing guide](CONTRIBUTING.md)

## License

This repository does not currently include a software license. Until a license
is added, the source code is not granted for copying, modification,
distribution, or reuse.

## Author

Designed and developed by **Mustafa Nasir**.
