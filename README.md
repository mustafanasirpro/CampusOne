# CampusOne

<div align="center">

**Everything your campus needs, in one place.**

A full-stack university community platform for learning, collaboration, campus
life, and student opportunities.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![JWT](https://img.shields.io/badge/Auth-JWT-000000?logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-Not%20specified-lightgrey)](#-license)

</div>

## 🎓 Project Overview

CampusOne is a student-focused platform designed to bring university resources
and communities into one cohesive product. Students can discover notes, ask and
answer questions, browse campus marketplace listings, find internships, join
events, receive notifications, track achievements, and use local AI-assisted
study tools.

The repository currently contains:

- A responsive React frontend with complete product screens and local demo data.
- A modular Spring Boot REST backend with PostgreSQL persistence.
- Secure JWT authentication with rotating, hashed refresh-token sessions.
- Versioned Flyway migrations covering the implemented backend modules.
- Automated controller, service, validation, security, and persistence tests.

> [!IMPORTANT]
> The frontend experience is implemented, but most screens still use local
> dummy data. Connecting those screens to the REST backend is the next major
> integration milestone.

## ✨ Features

### Frontend

- Premium landing, login, and signup experiences
- Responsive student dashboard and navigation
- Student profiles, preferences, activity, skills, and achievements
- Notes library with search, filters, bookmarks, previews, and upload flows
- Discussion and Q&A experiences with voting and replies
- Student marketplace listings, wishlists, and seller previews
- Internship discovery and saved opportunities
- Campus events, calendar views, and RSVP interactions
- Notifications, settings, empty states, loading states, and toast feedback
- Leaderboards, badges, XP progress, streaks, and challenges
- AI study assistant chat and study-tool interfaces

### Backend

- Registration and login with BCrypt password hashing
- Short-lived JWT access tokens
- Rotating opaque refresh tokens stored as SHA-256 hashes
- Session logout, replay protection, account lockout, and strict CORS handling
- Student profiles, skills, preferences, and visibility controls
- Notes metadata, tags, ratings, bookmarks, downloads, and moderation states
- Marketplace listing CRUD, filters, images metadata, and soft deletion
- Discussion questions, answers, votes, accepted answers, and pagination
- Event creation, participation, ownership, capacity, and visibility rules
- Internship posting, filtering, saving, and ownership controls
- In-app notification storage, read state, filtering, and bulk actions
- Unified public search across supported content modules
- XP profiles, badges, history, and weekly/monthly/all-time leaderboards
- AI chat history, generated study content, and usage tracking
- Deterministic local AI provider requiring no external credentials
- OpenAPI documentation and consistent API error responses

## 🧰 Tech Stack

| Layer | Technologies |
|---|---|
| Frontend | React 19, TypeScript 6, Vite 8, Tailwind CSS 4, React Router, Lucide React |
| Backend | Java 21, Spring Boot 3.5, Spring Web, Spring Security, Spring Data JPA |
| Database | PostgreSQL, Hibernate, Flyway |
| Authentication | JWT access tokens, opaque refresh tokens, BCrypt |
| API documentation | Springdoc OpenAPI and Swagger UI |
| Build tools | Maven, npm |
| Testing | JUnit 5, Mockito, MockMvc, Spring Security Test, Testcontainers |

## 🏗️ Backend Architecture

The backend is a modular monolith organized by domain under
`com.campusone`. Each feature follows a clear layered flow:

```text
HTTP request
    │
    ▼
Controller → Request DTO validation
    │
    ▼
Service → Transactions, ownership, and business rules
    │
    ▼
Repository → Spring Data JPA
    │
    ▼
PostgreSQL
```

Responses are produced through explicit response DTOs and manual mappers; JPA
entities never cross controller boundaries. Schema changes are owned by Flyway,
Hibernate validates the schema, and primary domain identifiers use UUIDs.

Security is stateless for access-token authentication. Refresh sessions are
persisted so they can be rotated and revoked safely. Protected operations also
enforce ownership in the service layer.

The AI module uses an `AiProvider` abstraction. Its current
`LocalStudyAiProvider` is deterministic, performs no network calls, and keeps
the project runnable without an AI API key.

## ✅ Implemented Modules

| Module | Current backend capabilities |
|---|---|
| Authentication | Registration, login, JWT access tokens, refresh rotation, logout, lockout |
| Academic core | Universities, departments, courses, and reference data |
| User profiles | Profile editing, skills, preferences, visibility |
| Notes | Metadata CRUD, tags, ratings, bookmarks, download events |
| Marketplace | Listing CRUD, filters, image metadata, ownership, soft deletion |
| Discussions | Questions, answers, voting, accepted answers |
| Events | Event CRUD, participation, capacity, visibility |
| Internships | Posting, search/filtering, saves, ownership |
| Notifications | User notifications, filtering, read state, soft deletion |
| Global search | Unified search across notes, listings, discussions, events, and internships |
| Gamification | XP ledger, levels, badges, history, leaderboards |
| AI Study Assistant | Sessions, messages, summaries, flashcards, quizzes, study plans, usage records |

Admin and moderation management APIs are not implemented yet. Notification and
XP infrastructure exists, but automatic triggers have not yet been connected
to every content module.

## 📁 Project Structure

```text
CampusOne/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/campusone/
│       │   │   ├── ai/
│       │   │   ├── academic/
│       │   │   ├── auth/
│       │   │   ├── discussion/
│       │   │   ├── event/
│       │   │   ├── gamification/
│       │   │   ├── internship/
│       │   │   ├── marketplace/
│       │   │   ├── note/
│       │   │   ├── notification/
│       │   │   ├── search/
│       │   │   ├── security/
│       │   │   ├── user/
│       │   │   └── common/
│       │   └── resources/
│       │       └── db/migration/
│       └── test/java/com/campusone/
├── public/
├── src/
│   ├── components/
│   ├── data/
│   ├── pages/
│   ├── routes/
│   ├── styles/
│   ├── types/
│   └── utils/
├── CONTRIBUTING.md
├── package.json
└── README.md
```

## 🚀 Installation

### Prerequisites

- Java 21
- Maven 3.9 or newer
- PostgreSQL
- Node.js `20.19+` or `22.12+`
- npm

### 1. Prepare PostgreSQL

Create a local database and login. Run the following from an administrator
`psql` session, replacing the example password:

```sql
CREATE ROLE campusone WITH LOGIN PASSWORD 'choose-a-local-password';
CREATE DATABASE campusone OWNER campusone;
```

### 2. Configure the backend

From the repository root:

```bash
cd backend
cp .env.example .env
```

On PowerShell, use:

```powershell
cd backend
Copy-Item .env.example .env
```

Edit `backend/.env` and set the database password and a newly generated JWT
secret. Generate a suitable secret with:

```bash
openssl rand -base64 32
```

The secret must be standard Base64 representing at least 32 random bytes.
Never reuse the public development value from `.env.example` in a real
environment.

### 3. Install frontend dependencies

From the repository root:

```bash
npm ci
```

## 🔐 Environment Variables

The backend optionally loads `backend/.env` through Spring Boot's native
configuration import. Operating-system environment variables take precedence.

| Variable | Required | Purpose / default |
|---|---:|---|
| `SPRING_PROFILES_ACTIVE` | Local | Use `local` for development |
| `DB_URL` | Yes | PostgreSQL JDBC URL |
| `DB_USERNAME` | Yes | Runtime database user |
| `DB_PASSWORD` | Yes | Runtime database password |
| `JWT_SECRET` | Yes | Standard Base64 secret containing at least 256 bits |
| `JWT_ISSUER` | No | Defaults to `campusone-backend` |
| `JWT_AUDIENCE` | No | Defaults to `campusone-api` |
| `JWT_ACCESS_TOKEN_TTL` | No | Defaults to `15m` |
| `REFRESH_TOKEN_TTL_DAYS` | No | Defaults to `7` |
| `AUTH_COOKIE_SECURE` | Production | Defaults to `true`; local profile uses `false` |
| `REFRESH_TOKEN_CLEANUP_INTERVAL` | No | Defaults to `24h` |
| `MAX_LOGIN_ATTEMPTS` | No | Defaults to `5` |
| `ACCOUNT_LOCK_MINUTES` | No | Defaults to `15` |
| `CORS_ALLOWED_ORIGINS` | No | Comma-separated exact frontend origins; defaults to local Vite origins and should be overridden in production |
| `OPENAPI_ENABLED` | No | Enabled by default; set to `false` to disable API documentation |
| `FLYWAY_URL` | No | Optional migration-role JDBC URL; falls back to `DB_URL` |
| `FLYWAY_USERNAME` | No | Optional migration user; falls back to `DB_USERNAME` |
| `FLYWAY_PASSWORD` | No | Optional migration password; falls back to `DB_PASSWORD` |

Secrets and local `.env` files must never be committed.

## 💻 Running Locally

### Backend

Ensure PostgreSQL is running, then:

```bash
cd backend
mvn spring-boot:run
```

The Maven run goal activates the `local` profile. Flyway applies pending
migrations automatically, and the API starts on `http://localhost:8080`.

### Frontend

In a second terminal, from the repository root:

```bash
npm run dev
```

Vite serves the frontend at `http://localhost:5173` by default.

## 🧪 Running Tests and Quality Checks

Run the complete backend verification gate:

```bash
cd backend
mvn clean verify
```

Repository tests requiring PostgreSQL Testcontainers run when Docker is
available.

Check and build the frontend:

```bash
npm run lint
npm run build
```

## 📚 API Documentation

With the backend running under the local profile:

- Swagger UI: [http://localhost:8080/api/v1/swagger-ui](http://localhost:8080/api/v1/swagger-ui)
- OpenAPI JSON: [http://localhost:8080/api/v1/openapi](http://localhost:8080/api/v1/openapi)
- Health endpoint: [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health)

Use `POST /api/v1/auth/register` and `POST /api/v1/auth/login` to obtain an
access token, then select **Authorize** in Swagger UI and enter the bearer
token. OpenAPI is disabled by default outside local development and should
remain disabled in untrusted production environments.

## 🗺️ Roadmap

- [x] Responsive React product interface
- [x] Spring Boot foundation and PostgreSQL schema
- [x] JWT and refresh-token authentication
- [x] Profiles, notes, marketplace, discussions, events, and internships
- [x] Notifications, global search, gamification, and local AI study tools
- [ ] Connect the React frontend to the REST APIs
- [ ] Add email verification and password recovery
- [ ] Add note and image uploads through private object storage
- [ ] Implement administration and moderation workflows
- [ ] Connect notification and XP triggers across domain modules
- [ ] Add production deployment and release automation

## 🔭 Future Improvements

- A production AI provider behind the existing provider abstraction, with
  quotas, privacy controls, retention rules, and safety checks
- Content reporting, campus verification, and moderator audit trails
- Reliable asynchronous notification delivery when product scale requires it
- Secure marketplace conversations without payment processing
- Search ranking and recommendation improvements based on measured usage
- Expanded integration, concurrency, accessibility, and performance testing
- Production observability, backup procedures, and documented recovery drills

## 🤝 Contributing

Contributions are welcome once the repository's license and contribution terms
are finalized. Before opening a pull request:

1. Read [CONTRIBUTING.md](CONTRIBUTING.md).
2. Keep changes focused on one module or concern.
3. Follow the existing package and layered architecture.
4. Add tests for changed behavior.
5. Run `mvn clean verify`, `npm run lint`, and `npm run build`.
6. Do not commit credentials, `.env` files, generated artifacts, or local data.

For larger proposals, open an issue before implementation so the design can be
discussed first.

## 📄 License

This repository does not currently include a software license. Until a license
is added, the source code is not granted for copying, modification,
distribution, or reuse. A recognized open-source license should be selected
before accepting external contributions or distributing releases.

## 👤 Author

Designed and developed by **Mustafa Nasir**.
