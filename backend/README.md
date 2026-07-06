# CampusOne Backend

Spring Boot foundation for the CampusOne university community platform.

## Current scope

The backend is a modular Spring Boot API for CampusOne:

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
- Notes with real R2-backed PDF upload and tracked downloads
- Marketplace, discussions, events, internships, and notifications
- Global search, gamification, AI study tools, and moderation workflows
- Consistent validation, authentication, authorization, and API error responses

## Development Status

### Completed

- Backend foundation and academic domain
- Authentication, JWT, refresh sessions, and profiles
- Notes, marketplace, discussions, events, and internships
- Notifications, search, gamification, AI, and moderation
- S3-compatible note PDF upload/download
- Java 21 development environment
- Maven build and test pipeline
- JWT configuration validation
- Security configuration

### Upcoming

- Multi-session management
- Email Verification
- Password Reset

## Requirements

- Java 21
- Maven 3.9 or newer
- PostgreSQL

## Environment variables

The application reads its PostgreSQL connection from:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SERVER_PORT`: hosting-platform server port, supplied by Render

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
- `CORS_ALLOWED_ORIGINS`: optional comma-separated exact frontend origins;
  defaults to `http://localhost:5173` and `http://127.0.0.1:5173`. Set it to
  the deployed frontend origin in production.
- `APP_CORS_ALLOWED_ORIGINS`: Spring-style deployment alias for the same
  exact-origin CORS property
- `OPENAPI_ENABLED`: defaults to `true`; set to `false` to disable API
  documentation
- `STORAGE_PROVIDER`: set to `r2` to enable real note PDF uploads; defaults to
  disabled so missing storage credentials never prevent startup
- `R2_ENDPOINT`: Cloudflare R2 S3 API endpoint
- `R2_ACCESS_KEY_ID`: R2 API token access key
- `R2_SECRET_ACCESS_KEY`: R2 API token secret key
- `R2_BUCKET`: bucket used for note PDFs
- `R2_REGION`: optional; defaults to `auto`
- `R2_PUBLIC_BASE_URL`: optional public bucket/custom-domain URL; when omitted,
  the backend creates short-lived private presigned GET URLs
- `MAX_UPLOAD_SIZE_MB`: optional maximum admin PDF size; defaults to `25`
- `ADMIN_MAX_UPLOADS_PER_DAY`: optional per-admin daily upload count;
  defaults to `200`
- `ADMIN_MAX_STORAGE_MB_PER_MONTH`: optional per-admin monthly uploaded
  bytes; defaults to `5000`
- `GLOBAL_UPLOAD_STORAGE_CAP_MB`: optional global monthly uploaded bytes;
  defaults to `8192` (8 GB)
- `ADMIN_UPLOAD_EMAILS`: optional comma-separated fallback note-admin emails;
  active `ADMIN` assignments in the moderators table are preferred
- `STORAGE_DOWNLOAD_URL_TTL`: optional presigned URL lifetime; defaults to `10m`

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

The local profile provides development defaults for:

- `CORS_ALLOWED_ORIGINS=http://localhost:5173`
- `AUTH_COOKIE_SECURE=false`
- `OPENAPI_ENABLED=true`

The two exact loopback CORS origins are also safe startup defaults for packaged
deployments. Production deployments should replace them through
`CORS_ALLOWED_ORIGINS` so the deployed frontend can access the API.

### Cloudflare R2 note storage

The note upload endpoint accepts multipart form data at
`POST /api/v1/notes/upload`, with a JSON `note` part and an
`application/pdf` `file` part. The backend validates the PDF, generates an
owner-scoped object key, uploads it to R2, and stores only its metadata in
PostgreSQL. Uploads and note create/edit/delete operations require an active
`ADMIN` assignment (or an email explicitly listed in `ADMIN_UPLOAD_EMAILS`).
Normal users retain view, download, bookmark, and rating access. Uploads are
limited to PDF files.
Quota checks use UTC calendar days/months and PostgreSQL transaction locks so
concurrent requests and multiple backend instances cannot bypass the limits.

Configure these Render environment variables:

```text
STORAGE_PROVIDER=r2
R2_ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=<secret>
R2_SECRET_ACCESS_KEY=<secret>
R2_BUCKET=<bucket-name>
R2_REGION=auto
R2_PUBLIC_BASE_URL=
MAX_UPLOAD_SIZE_MB=25
ADMIN_MAX_UPLOADS_PER_DAY=200
ADMIN_MAX_STORAGE_MB_PER_MONTH=5000
GLOBAL_UPLOAD_STORAGE_CAP_MB=8192
ADMIN_UPLOAD_EMAILS=
```

Use an R2 API token restricted to Object Read & Write for the selected bucket.
Leave `R2_PUBLIC_BASE_URL` blank for private storage; CampusOne then returns
short-lived presigned download URLs. No storage credentials are sent to the
frontend or stored in PostgreSQL.

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
Swagger and OpenAPI are enabled by default and remain publicly reachable through
the documented routes. Set `OPENAPI_ENABLED=false` only when API documentation
should be disabled. Production deployments should avoid the `local` profile and
set `CORS_ALLOWED_ORIGINS` to the exact deployed frontend origin.

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
- Upload note PDF: `POST http://localhost:8080/api/v1/notes/upload`
- OpenAPI JSON: `http://localhost:8080/api/v1/openapi`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui`
