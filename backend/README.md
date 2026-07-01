# CampusOne Backend

Spring Boot foundation for the CampusOne university community platform.

## Current scope

The backend currently contains the Phase 0 foundation, Phase 1 core domain
model, and Phase 2 authentication MVP:

- Users, roles, universities, departments, courses, and student profiles
- PostgreSQL schema managed by Flyway
- Repository interfaces, response DTOs, and manual mappers
- BCrypt password storage with the CampusOne password policy
- Stateless, 15-minute HS256 JWT access tokens
- Registration, login, and authenticated current-user endpoints
- Consistent validation, authentication, authorization, and API error responses

Refresh tokens, logout, email verification, password recovery, and CampusOne
business modules are intentionally not implemented yet.

## Requirements

- Java 21
- Maven 3.9 or newer
- PostgreSQL

## Environment variables

The application reads its PostgreSQL connection from:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

JWT signing uses:

- `JWT_SECRET`: standard Base64 encoding of at least 32 cryptographically
  random bytes
- `JWT_ISSUER`: optional; defaults to `campusone-backend`
- `JWT_ACCESS_TOKEN_TTL`: optional; defaults to `15m`

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
from `.env`.

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
$env:JWT_SECRET = "<generated-standard-base64-secret>"
mvn spring-boot:run
```

## Verification

```powershell
mvn clean verify
```

## Available URLs

- Health: `GET http://localhost:8080/api/v1/health`
- Register: `POST http://localhost:8080/api/v1/auth/register`
- Login: `POST http://localhost:8080/api/v1/auth/login`
- Current user: `GET http://localhost:8080/api/v1/users/me`
- OpenAPI JSON: `http://localhost:8080/api/v1/openapi`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui`
