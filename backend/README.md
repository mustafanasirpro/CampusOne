# CampusOne Backend

Spring Boot foundation for the CampusOne university community platform.

## Current scope

The backend currently contains the Phase 0 foundation, Phase 1 core domain
model, Phase 2 authentication MVP, Phase 3 refresh-token authentication, and
Phase 4 user profiles:

- Users, roles, universities, departments, courses, and student profiles
- PostgreSQL schema managed by Flyway
- Repository interfaces, response DTOs, and manual mappers
- BCrypt password storage with the CampusOne password policy
- Stateless, 15-minute HS256 JWT access tokens
- Hashed, PostgreSQL-backed refresh-token sessions with rotation
- Persistent failed-login throttling and temporary account lockout
- Exact-origin CORS and browser request-origin validation
- Registration, login, refresh, and logout endpoints
- Authenticated profile editing with validated academic selections
- Normalized student skills and personal display preferences
- Public and private student profile visibility
- Consistent validation, authentication, authorization, and API error responses

Email verification, password recovery, and CampusOne business modules are
intentionally not implemented yet.

## Development Status

### Completed

- Backend Foundation (Phase 0)
- Core Academic Domain (Phase 1)
- Authentication & JWT (Phase 2)
- Refresh Token Authentication (Phase 3)
- User Profile Module (Phase 4)
- Java 21 development environment
- Maven build and test pipeline
- JWT configuration validation
- Security configuration

### Upcoming

- Academic Directory Enhancements (Phase 5)
- Multi-session management
- Email Verification
- Password Reset
- Notes Module
- Discussions Module
- Marketplace Module
- Events Module
- Internship Module
- Notifications
- Leaderboard

## Requirements

- Java 21
- Maven 3.9 or newer
- PostgreSQL

## Environment variables

The application reads its PostgreSQL connection from:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Flyway defaults to the same values locally. Production can use a separate
migration role by setting `FLYWAY_URL`, `FLYWAY_USERNAME`, and
`FLYWAY_PASSWORD`, while keeping the runtime `DB_*` role limited to required
DML privileges. Production JDBC URLs should require certificate-verified TLS,
for example PostgreSQL `sslmode=verify-full`.

JWT signing uses:

- `JWT_SECRET`: standard Base64 encoding of at least 32 cryptographically
  random bytes
- `JWT_ISSUER`: optional; defaults to `campusone-backend`
- `JWT_AUDIENCE`: optional; defaults to `campusone-api`
- `JWT_ACCESS_TOKEN_TTL`: optional; defaults to `15m`
- `REFRESH_TOKEN_TTL_DAYS`: optional; defaults to `7`
- `AUTH_COOKIE_SECURE`: defaults to `true`; set to `false` only for local HTTP
- `REFRESH_TOKEN_CLEANUP_INTERVAL`: optional; defaults to `24h`
- `MAX_LOGIN_ATTEMPTS`: optional; defaults to `5`
- `ACCOUNT_LOCK_MINUTES`: optional; defaults to `15`
- `CORS_ALLOWED_ORIGINS`: required comma-separated exact frontend origins in
  production; the `local` profile defaults to `http://localhost:5173`
- `OPENAPI_ENABLED`: defaults to `false`; enable only in trusted environments

Generate a different JWT secret for every environment. PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
[Convert]::ToBase64String($bytes)
```

Or with OpenSSL:

```shell
openssl rand -base64 32
```

`JWT_SECRET` must use standard Base64. Raw passphrases and Base64URL values
containing `_` or `-` are rejected at startup. The value in `.env.example` is
public development data and must not be reused as a real secret.

### Local setup with `.env`

Spring Boot resolves operating-system environment variables automatically, but
does not load `.env` files by default. CampusOne uses Spring Boot's native
`spring.config.import` support to optionally load `backend/.env` as a properties
file. Operating-system environment variables still take precedence over values
from `.env`. The Maven `spring-boot:run` goal activates the `local` profile
automatically. The example also sets `SPRING_PROFILES_ACTIVE=local` for IDE
launches and direct local JAR execution.

From the repository root:

```powershell
cd backend
Copy-Item .env.example .env
```

Edit `.env` and replace:

- `DB_URL` with the JDBC URL for an existing PostgreSQL database.
- `DB_USERNAME` and `DB_PASSWORD` with a PostgreSQL login that owns or can
  migrate that database.
- `JWT_SECRET` with a newly generated Base64 secret. Never use the public
  example value outside local development.

The local profile provides development-only defaults for:

- `CORS_ALLOWED_ORIGINS=http://localhost:5173`
- `AUTH_COOKIE_SECURE=false`
- `OPENAPI_ENABLED=true`

They are not packaged as production defaults. Without the `local` profile,
startup still requires an explicit non-empty `CORS_ALLOWED_ORIGINS` value.

For the default values in `.env.example`, PostgreSQL should have a database
named `campusone` and a login named `campusone`. One possible setup from an
administrator `psql` session is:

```sql
CREATE ROLE campusone WITH LOGIN PASSWORD 'choose-a-local-password';
CREATE DATABASE campusone OWNER campusone;
```

Use the same password in `.env`, then start the backend while the current
directory is `backend`:

```powershell
mvn spring-boot:run
```

The `.env` file is ignored by Git. If you prefer shell or IDE environment
variables, set the same names there and do not create `.env`:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/campusone"
$env:DB_USERNAME = "campusone"
$env:DB_PASSWORD = "<your-local-password>"
$env:SPRING_PROFILES_ACTIVE = "local"
$env:JWT_SECRET = "<generated-standard-base64-secret>"
$env:JWT_AUDIENCE = "campusone-api"
$env:REFRESH_TOKEN_TTL_DAYS = "7"
$env:AUTH_COOKIE_SECURE = "false"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
$env:OPENAPI_ENABLED = "true"
mvn spring-boot:run
```

Refresh tokens are never returned in JSON or stored in plaintext. Login places
the opaque token in an HttpOnly, `SameSite=Strict` cookie scoped to
`/api/v1/auth`; the cookie is always host-only, and PostgreSQL stores only its
SHA-256 hash. Set
`AUTH_COOKIE_SECURE=true` in every HTTPS environment.

Unsafe browser requests are accepted only from exact configured origins (or
the API's own origin). This origin check complements the refresh cookie's
`SameSite=Strict` policy. Wildcard CORS origins are intentionally rejected.
Swagger and OpenAPI are enabled by default only in the local profile. Production
deployments should leave `OPENAPI_ENABLED=false`, avoid the `local` profile, and
set `CORS_ALLOWED_ORIGINS` explicitly.

## Verification

```powershell
mvn clean verify
```

## Available URLs

- Health: `GET http://localhost:8080/api/v1/health`
- Register: `POST http://localhost:8080/api/v1/auth/register`
- Login: `POST http://localhost:8080/api/v1/auth/login`
- Refresh access token: `POST http://localhost:8080/api/v1/auth/refresh`
- Logout current session: `POST http://localhost:8080/api/v1/auth/logout`
- Current user: `GET http://localhost:8080/api/v1/users/me`
- Update current profile: `PATCH http://localhost:8080/api/v1/users/me`
- Replace current skills: `PUT http://localhost:8080/api/v1/users/me/skills`
- Public profile: `GET http://localhost:8080/api/v1/profiles/{userId}`
- OpenAPI JSON: `http://localhost:8080/api/v1/openapi`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui`
